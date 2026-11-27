package com.example.auction.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "auction")
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuctionStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "starting_bid", nullable = false)
    private long startingBid;

    @Column(name = "current_bid", nullable = false)
    private long currentBid;

    @Column(name = "bid_sequence", nullable = false)
    private long bidSequence;

    @Version
    private long version;

    protected Auction() {
    }

    public Auction(String title, Instant startsAt, Instant endsAt, long startingBid) {
        this.title = title;
        this.status = AuctionStatus.OPEN;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.startingBid = startingBid;
        this.currentBid = startingBid;
        this.bidSequence = 0;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public long getStartingBid() {
        return startingBid;
    }

    public long getCurrentBid() {
        return currentBid;
    }

    public long getBidSequence() {
        return bidSequence;
    }

    public void acceptBid(long amount) {
        this.currentBid = amount;
        this.bidSequence++;
    }

    public void end() {
        this.status = AuctionStatus.ENDED;
    }
}
