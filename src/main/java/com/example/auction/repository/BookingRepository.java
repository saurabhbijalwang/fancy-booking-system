package com.example.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.auction.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {
}
