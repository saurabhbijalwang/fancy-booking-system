package com.example.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.auction.api.ApiModels.AuctionReport;
import com.example.auction.api.ApiModels.AuctionResponse;
import com.example.auction.api.ApiModels.BidResponse;
import com.example.auction.domain.Auction;
import com.example.auction.domain.AuctionStatus;
import com.example.auction.domain.Bidder;
import com.example.auction.repository.BidderRepository;
import com.example.auction.service.AuctionConflictException;
import com.example.auction.service.AuctionService;
import com.example.auction.service.UserService;

@SpringBootTest
class AuctionApplicationTests {
    @Autowired
    private UserService userService;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private BidderRepository bidderRepository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void createsUsersPlacesAtomicBidsAndBuildsAccurateReport() throws Exception {
        long firstUserId = userService.createUsers(12).firstUserId();
        AuctionResponse auction = auctionService.createAuction("Concurrency test", 15, 0L);
        int bidCount = 12;
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentHashMap<String, Integer> outcomes = new ConcurrentHashMap<>();
        // A bounded pool keeps this test's worker and queued-task counts explicit.
        ExecutorService executor = new ThreadPoolExecutor(
                bidCount, bidCount, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(bidCount));
        try {
            List<java.util.concurrent.Future<BidResponse>> futures = new ArrayList<>();
            for (int index = 0; index < bidCount; index++) {
                long userId = firstUserId + index;
                futures.add(executor.submit(() -> {
                    start.await();
                    BidResponse bid = auctionService.placeBid(auction.id(), userId);
                    // merge is atomic for a key; volatile alone cannot make a read-modify-write counter atomic.
                    outcomes.merge("accepted", 1, Integer::sum);
                    return bid;
                }));
            }
            start.countDown();
            List<BidResponse> accepted = new ArrayList<>();
            for (var future : futures) {
                accepted.add(future.get(20, TimeUnit.SECONDS));
            }
            assertThat(accepted).extracting(BidResponse::sequence).containsExactlyInAnyOrder(
                    1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
            assertThat(accepted).extracting(BidResponse::amount).containsExactlyInAnyOrder(
                    1_000L, 2_000L, 3_000L, 4_000L, 5_000L, 6_000L,
                    7_000L, 8_000L, 9_000L, 10_000L, 11_000L, 12_000L);
            assertThat(outcomes).containsEntry("accepted", bidCount);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                        .as("bounded bid workers should terminate")
                        .isTrue();
            }
        }

        AuctionReport report = auctionService.getReport(auction.id());
        assertThat(report.totalBids()).isEqualTo(bidCount);
        assertThat(report.finalBid()).isEqualTo(12_000);
        assertThat(report.winnerUserId()).isNotNull();
        assertThat(report.winningBid()).isEqualTo(12_000);
        assertThat(report.bids()).hasSize(bidCount);
        assertThat(report.users()).hasSize(bidCount);
    }

    @Test
    void rejectsBidsForUnknownUsersAndEnforcesUserBatchLimit() {
        AuctionResponse auction = auctionService.createAuction("Validation test", 15, 0L);

        assertThatThrownBy(() -> auctionService.placeBid(auction.id(), Long.MAX_VALUE))
                .isInstanceOf(com.example.auction.service.ResourceNotFoundException.class);
        assertThatThrownBy(() -> userService.createUsers(1_000_001))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void firstLevelCacheReusesEntityWithinOnePersistenceContext() {
        AuctionResponse auction = auctionService.createAuction("First-level cache test", 15, 0L);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Auction firstRead = entityManager.find(Auction.class, auction.id());
            Auction secondRead = entityManager.find(Auction.class, auction.id());

            assertThat(secondRead).isSameAs(firstRead);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    @Test
    void readOnlyBidderUsesSecondLevelCacheAcrossPersistenceContexts() {
        long bidderId = userService.createUsers(1).firstUserId();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        entityManagerFactory.getCache().evict(Bidder.class, bidderId);
        statistics.clear();

        assertThat(bidderRepository.findById(bidderId)).isPresent();
        assertThat(statistics.getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        statistics.clear();

        assertThat(bidderRepository.findById(bidderId)).isPresent();
        assertThat(statistics.getSecondLevelCacheHitCount()).isEqualTo(1);
        assertThat(statistics.getPrepareStatementCount()).isZero();

        bidderRepository.deleteById(bidderId);
        statistics.clear();
        assertThat(bidderRepository.findById(bidderId)).isEmpty();
        assertThat(statistics.getSecondLevelCacheHitCount()).isZero();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void versionRejectsStaleAuctionUpdatesWhileBidFlowUsesPessimisticLocking() {
        AuctionResponse auction = auctionService.createAuction("Optimistic lock test", 15, 0L);
        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager secondEntityManager = entityManagerFactory.createEntityManager();
        EntityTransaction firstTransaction = firstEntityManager.getTransaction();
        EntityTransaction secondTransaction = secondEntityManager.getTransaction();
        try {
            firstTransaction.begin();
            secondTransaction.begin();
            Auction firstCopy = firstEntityManager.find(Auction.class, auction.id());
            Auction staleCopy = secondEntityManager.find(Auction.class, auction.id());

            firstCopy.acceptBid(1_000L);
            staleCopy.acceptBid(2_000L);
            firstTransaction.commit();

            assertThatThrownBy(secondTransaction::commit)
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(OptimisticLockException.class);
        } finally {
            if (firstTransaction.isActive()) {
                firstTransaction.rollback();
            }
            if (secondTransaction.isActive()) {
                secondTransaction.rollback();
            }
            firstEntityManager.close();
            secondEntityManager.close();
        }
    }

    @Test
    void reportFetchesBiddersWithoutNPlusOneQueries() {
        long firstUserId = userService.createUsers(3).firstUserId();
        AuctionResponse auction = auctionService.createAuction("Report query test", 15, 0L);
        for (long userId = firstUserId; userId < firstUserId + 3; userId++) {
            auctionService.placeBid(auction.id(), userId);
        }
        entityManagerFactory.getCache().evict(Bidder.class);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        AuctionReport report = auctionService.getReport(auction.id());

        assertThat(report.bids()).hasSize(3);
        assertThat(statistics.getEntityFetchCount()).isZero();
        // The scheduled auction closer shares global statistics and can add one query.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void biddingOnceClosedDoesNotChangeTheReport() {
        AuctionResponse auction = auctionService.createAuction("Closing test", 1, 0L);
        long userId = userService.createUsers(1).firstUserId();

        assertThat(auction.status()).isEqualTo(AuctionStatus.OPEN);
        clock.setInstant(auction.endsAt());
        assertThat(clock.instant()).isEqualTo(auction.endsAt());
        assertThat(auctionService.getAuction(auction.id()).remainingSeconds()).isZero();
        assertThat(auctionService.getAuction(auction.id()).status()).isEqualTo(AuctionStatus.ENDED);
        assertThatThrownBy(() -> auctionService.placeBid(auction.id(), userId))
                .isInstanceOf(AuctionConflictException.class)
                .hasMessageContaining("Auction is closed");
        AuctionReport report = auctionService.getReport(auction.id());
        assertThat(report.status()).isEqualTo(AuctionStatus.ENDED);
        assertThat(report.totalBids()).isZero();
    }

    @TestConfiguration
    static class TestClockConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>(Instant.now());

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        void setInstant(Instant instant) {
            current.set(instant);
        }

        void reset() {
            current.set(Instant.now());
        }
    }
}
