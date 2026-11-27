package com.example.auction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.auction.domain.Bidder;
import com.example.auction.service.UserService;

@SpringBootTest
class BidderCacheTests {
    @Autowired
    private UserService userService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void repeatedUserLookupsUseTheSecondLevelCacheAcrossTransactions() {
        long userId = userService.createUsers(1).firstUserId();
        entityManagerFactory.getCache().evict(Bidder.class, userId);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        assertThat(userService.findUserById(userId).id()).isEqualTo(userId);
        assertThat(userService.findUserById(userId).id()).isEqualTo(userId);
        assertThat(userService.findUserById(userId).id()).isEqualTo(userId);

        assertThat(statistics.getSecondLevelCacheMissCount()).isGreaterThanOrEqualTo(1L);
        assertThat(statistics.getSecondLevelCacheHitCount()).isGreaterThanOrEqualTo(2L);
    }
}
