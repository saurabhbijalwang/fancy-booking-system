package com.example.auction.service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.auction.api.ApiModels.UserBatchResponse;
import com.example.auction.api.ApiModels.UserResponse;
import com.example.auction.domain.Bidder;
import com.example.auction.repository.BidderRepository;

@Service
public class UserService {
    private static final int MAX_USERS_PER_REQUEST = 1_000_000;
    private static final int MAX_USERS_PER_TRANSACTION = 2_000;
    private static final String INSERT_USER_SQL =
            "INSERT INTO bidder (display_name, email, created_at) VALUES (?, ?, ?)";
    private static final String FIND_INSERTED_USERS_SQL =
            "SELECT MIN(id), MAX(id), COUNT(*) FROM bidder WHERE email LIKE ?";

    private final JdbcTemplate jdbcTemplate;
    private final BidderRepository bidderRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public UserService(
            JdbcTemplate jdbcTemplate,
            BidderRepository bidderRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.bidderRepository = bidderRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public UserBatchResponse createUsers(int count) {
        if (count < 1 || count > MAX_USERS_PER_REQUEST) {
            throw new IllegalArgumentException("count must be between 1 and 1,000,000");
        }

        Long firstId = null;
        Long lastId = null;
        for (int offset = 0; offset < count; offset += MAX_USERS_PER_TRANSACTION) {
            int batchStart = offset;
            int batchEnd = Math.min(offset + MAX_USERS_PER_TRANSACTION, count);
            String batchToken = UUID.randomUUID().toString();
            long[] insertedIds = transactionTemplate.execute(
                    status -> insertBatch(batchStart, batchEnd, batchToken));
            if (insertedIds == null || insertedIds.length != 2) {
                throw new IllegalStateException("Could not create the bidder batch");
            }
            if (firstId == null) {
                firstId = insertedIds[0];
            }
            lastId = insertedIds[1];
        }
        return new UserBatchResponse(count, firstId, lastId);
    }

    private long[] insertBatch(int start, int end, String batchToken) {
        OffsetDateTime createdAt = clock.instant().atOffset(ZoneOffset.UTC);
        for (int index = start; index < end; index++) {
            String identifier = "%08d".formatted(index + 1);
            jdbcTemplate.update(
                    INSERT_USER_SQL,
                    "Bidder-" + identifier,
                    "bidder-" + batchToken + "-" + identifier + "@auction.local",
                    createdAt);
        }
        long[] ids = jdbcTemplate.queryForObject(
                FIND_INSERTED_USERS_SQL,
                (resultSet, rowNumber) -> new long[]{
                        resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3)
                },
                "bidder-" + batchToken + "-%@auction.local");
        if (ids == null || ids[2] != end - start) {
            throw new IllegalStateException("Could not verify the created bidder batch");
        }
        return new long[]{ids[0], ids[1]};
    }

    public List<UserResponse> listUsers(int limit) {
        return bidderRepository.findAll(org.springframework.data.domain.PageRequest.of(
                        0, Math.min(Math.max(limit, 1), 500)))
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}
