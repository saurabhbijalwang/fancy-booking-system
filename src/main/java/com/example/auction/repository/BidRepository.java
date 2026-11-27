package com.example.auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.auction.domain.Bid;

public interface BidRepository extends JpaRepository<Bid, Long> {
    @EntityGraph(attributePaths = "bidder")
    @Query("select b from Bid b where b.auction.id = :auctionId order by b.sequenceNumber")
    List<Bid> findReportBids(@Param("auctionId") Long auctionId);
}
