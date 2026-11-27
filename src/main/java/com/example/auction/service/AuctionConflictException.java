package com.example.auction.service;

public class AuctionConflictException extends RuntimeException {
    public AuctionConflictException(String message) {
        super(message);
    }
}
