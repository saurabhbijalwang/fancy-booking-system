package com.example.auction.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auction.api.ApiModels.AuctionReport;
import com.example.auction.api.ApiModels.AuctionResponse;
import com.example.auction.api.ApiModels.BidResponse;
import com.example.auction.api.ApiModels.UserBidSummary;
import com.example.auction.domain.Auction;
import com.example.auction.domain.AuctionStatus;
import com.example.auction.domain.Bid;
import com.example.auction.domain.Bidder;
import com.example.auction.repository.AuctionRepository;
import com.example.auction.repository.BidRepository;
import com.example.auction.repository.BidderRepository;

@Service
public class AuctionService {
    private static final long BID_INCREMENT = 1_000;
    private static final int MAX_DURATION_MINUTES = 24 * 60;

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final BidderRepository bidderRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public AuctionService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            BidderRepository bidderRepository,
            Clock clock,
            ApplicationEventPublisher eventPublisher) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.bidderRepository = bidderRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuctionResponse createAuction(String title, Integer durationMinutes, Long startingBid) {
        String normalizedTitle = title == null || title.isBlank() ? "Auction" : title.trim();
        int duration = durationMinutes == null ? 15 : durationMinutes;
        long startBid = startingBid == null ? 0 : startingBid;
        if (duration < 1 || duration > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("durationMinutes must be between 1 and 1440");
        }
        if (startBid < 0 || startBid > Long.MAX_VALUE - BID_INCREMENT) {
            throw new IllegalArgumentException("startingBid must be a non-negative amount");
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Auction auction = auctionRepository.save(new Auction(
                normalizedTitle, now, now.plus(Duration.ofMinutes(duration)), startBid));
        return AuctionResponse.from(auction, now);
    }

    @Transactional(readOnly = true)
    public AuctionResponse getAuction(long auctionId) {
        Auction auction = findAuction(auctionId);
        return AuctionResponse.from(auction, clock.instant());
    }

    @Transactional
    public BidResponse placeBid(long auctionId, long userId) {
        Instant now = clock.instant();
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction " + auctionId + " was not found"));
        if (auction.getStatus() != AuctionStatus.OPEN || !now.isBefore(auction.getEndsAt())) {
            if (auction.getStatus() == AuctionStatus.OPEN) {
                auction.end();
            }
            throw new AuctionConflictException("Auction is closed; bids are accepted only before its end time");
        }
        Bidder bidder = bidderRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found"));
        long amount = Math.addExact(auction.getCurrentBid(), BID_INCREMENT);
        auction.acceptBid(amount);
        Bid bid = bidRepository.save(new Bid(auction, bidder, auction.getBidSequence(), amount, now));
        eventPublisher.publishEvent(new AuctionUpdatedEvent(AuctionResponse.from(auction, now)));
        return BidResponse.from(bid);
    }

    @Transactional(readOnly = true)
    public AuctionReport getReport(long auctionId) {
        Auction auction = findAuction(auctionId);
        List<Bid> storedBids = bidRepository.findReportBids(auctionId);
        List<BidResponse> bids = storedBids.stream().map(BidResponse::from).toList();
        Map<Long, UserBidSummary> summaries = new LinkedHashMap<>();
        for (Bid bid : storedBids) {
            long bidderId = bid.getBidder().getId();
            UserBidSummary previous = summaries.get(bidderId);
            summaries.put(bidderId, new UserBidSummary(
                    bidderId,
                    bid.getBidder().getDisplayName(),
                    previous == null ? 1 : previous.bidCount() + 1,
                    bid.getAmount()));
        }
        BidResponse winningBid = bids.isEmpty() ? null : bids.get(bids.size() - 1);
        AuctionStatus status = clock.instant().isBefore(auction.getEndsAt())
                ? auction.getStatus()
                : AuctionStatus.ENDED;
        return new AuctionReport(
                auction.getId(),
                "%04d".formatted(auction.getId()),
                auction.getTitle(),
                status,
                auction.getStartsAt(),
                auction.getEndsAt(),
                auction.getCurrentBid(),
                auction.getBidSequence(),
                winningBid == null ? null : winningBid.userId(),
                winningBid == null ? null : winningBid.userName(),
                winningBid == null ? null : winningBid.amount(),
                bids,
                new ArrayList<>(summaries.values()));
    }

    @Scheduled(fixedDelay = 1_000)
    @Transactional
    public void closeExpiredAuctions() {
        Instant now = clock.instant();
        List<Auction> expired = auctionRepository.findByStatusAndEndsAtLessThanEqual(AuctionStatus.OPEN, now);
        for (Auction auction : expired) {
            auction.end();
            eventPublisher.publishEvent(new AuctionUpdatedEvent(AuctionResponse.from(auction, now)));
        }
    }

    private Auction findAuction(long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction " + auctionId + " was not found"));
    }
}
