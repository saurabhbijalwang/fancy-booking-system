package com.example.auction.api;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import com.example.auction.domain.Auction;
import com.example.auction.domain.AuctionStatus;
import com.example.auction.domain.Bid;
import com.example.auction.domain.Bidder;

public final class ApiModels {
    private ApiModels() {
    }

    public record CreateUsersRequest(@Min(1) @Max(1_000_000) int count) {
    }

    public record UserBatchResponse(int created, long firstUserId, long lastUserId) {
    }

    public record UserResponse(long id, String displayName, String email) {
        public static UserResponse from(Bidder bidder) {
            return new UserResponse(bidder.getId(), bidder.getDisplayName(), bidder.getEmail());
        }
    }

    public record CreateAuctionRequest(
            @NotBlank String title,
            @Min(1) @Max(1440) Integer durationMinutes,
            @PositiveOrZero Long startingBid) {
    }

    public record PlaceBidRequest(@Positive long userId) {
    }

    public record AuctionResponse(
            long id,
            String auctionNumber,
            String title,
            AuctionStatus status,
            Instant startsAt,
            Instant endsAt,
            long startingBid,
            long currentBid,
            long totalBids,
            long remainingSeconds) {
        public static AuctionResponse from(Auction auction, Instant now) {
            long remaining = Math.max(0, auction.getEndsAt().getEpochSecond() - now.getEpochSecond());
            AuctionStatus status = now.isBefore(auction.getEndsAt()) ? auction.getStatus() : AuctionStatus.ENDED;
            return new AuctionResponse(auction.getId(), formatAuctionNumber(auction.getId()), auction.getTitle(),
                    status, auction.getStartsAt(), auction.getEndsAt(), auction.getStartingBid(),
                    auction.getCurrentBid(), auction.getBidSequence(), remaining);
        }
    }

    public record BidResponse(
            long bidId,
            long auctionId,
            String auctionNumber,
            long sequence,
            long userId,
            String userName,
            long amount,
            Instant timestamp) {
        public static BidResponse from(Bid bid) {
            return new BidResponse(bid.getId(), bid.getAuctionId(), formatAuctionNumber(bid.getAuctionId()),
                    bid.getSequenceNumber(), bid.getBidder().getId(), bid.getBidder().getDisplayName(),
                    bid.getAmount(), bid.getCreatedAt());
        }
    }

    public record UserBidSummary(long userId, String userName, long bidCount, Long latestBid) {
    }

    public record AuctionReport(
            long auctionId,
            String auctionNumber,
            String title,
            AuctionStatus status,
            Instant startsAt,
            Instant endsAt,
            long finalBid,
            long totalBids,
            Long winnerUserId,
            String winnerName,
            Long winningBid,
            List<BidResponse> bids,
            List<UserBidSummary> users) {
    }

    private static String formatAuctionNumber(long id) {
        return String.format("%04d", id);
    }
}
