package com.example.auction.service;

import com.example.auction.api.ApiModels.AuctionResponse;

public record AuctionUpdatedEvent(AuctionResponse auction) {
}
