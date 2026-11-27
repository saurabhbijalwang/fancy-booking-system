package com.example.auction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.auction.domain.Booking;
import com.example.auction.repository.BookingRepository;

@SpringBootTest
class BookingIdentifierTests {
    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void defaultGeneratedIdsAreNumericInsteadOfFancyNumbers() {
        Booking first = bookingRepository.saveAndFlush(new Booking("Guest One"));
        Booking second = bookingRepository.saveAndFlush(new Booking("Guest Two"));
        Booking third = bookingRepository.saveAndFlush(new Booking("Guest Three"));

        assertThat(List.of(first.getId(), second.getId(), third.getId()))
                .containsExactly(1L, 2L, 3L);
    }
}
