package com.example.auction.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Marks this class as a database-backed entity managed by JPA.
@Entity
// Maps this entity to the "bid" table.
@Table(name = "bid")
public class Bid {
    // Primary key; the database generates its value when a bid is inserted.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each bid belongs to one auction; LAZY loading avoids fetching it unless needed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Stores the auction foreign key and requires it to be present.
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    // Each bid is placed by one bidder; load bidder details only when accessed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Stores the bidder foreign key and requires it to be present.
    @JoinColumn(name = "bidder_id", nullable = false)
    private Bidder bidder;

    // The bid's order within its auction, mapped to the database column sequence_number.
    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    // The offered amount; every bid must have an amount.
    @Column(nullable = false)
    private long amount;

    // The time this bid was placed, stored in the created_at column.
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Required by JPA when it creates an entity from a database row.
    protected Bid() {
    }

    // Creates a bid with its auction, bidder, order, amount, and placement time.
    public Bid(Auction auction, Bidder bidder, long sequenceNumber, long amount, Instant createdAt) {
        this.auction = auction;
        this.bidder = bidder;
        this.sequenceNumber = sequenceNumber;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    // Returns the database-generated identifier for this bid.
    public Long getId() {
        return id;
    }

    // Returns the associated auction's ID without exposing the full auction object.
    public Long getAuctionId() {
        return auction.getId();
    }

    // Returns the bidder associated with this bid.
    public Bidder getBidder() {
        return bidder;
    }

    // Returns this bid's position in the auction's sequence.
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    // Returns the amount offered in this bid.
    public long getAmount() {
        return amount;
    }

    // Returns when this bid was placed.
    public Instant getCreatedAt() {
        return createdAt;
    }
}
