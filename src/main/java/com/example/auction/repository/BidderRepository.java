package com.example.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.auction.domain.Bidder;

public interface BidderRepository extends JpaRepository<Bidder, Long> {
}
