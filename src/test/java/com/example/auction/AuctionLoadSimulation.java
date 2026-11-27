package com.example.auction;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.LongStream;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class AuctionLoadSimulation extends Simulation {
    private final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private final String auctionId = requiredProperty("auctionId");
    private final long firstUserId = Long.parseLong(requiredProperty("firstUserId"));
    private final int virtualUsers = Integer.getInteger("users", 1_000);
    private final int rampSeconds = Integer.getInteger("rampSeconds", 60);
    private final int durationSeconds = Integer.getInteger("durationSeconds", 60);
    private final int pauseMillis = Integer.getInteger("pauseMillis", 1_000);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final Iterator<Map<String, Object>> bidderIds = LongStream.iterate(firstUserId, id -> id + 1)
            .mapToObj(id -> Map.<String, Object>of("userId", id))
            .iterator();

    private final ScenarioBuilder biddingScenario = scenario("Concurrent auction bidders")
            .feed(bidderIds)
            .exec(session -> session.set("auctionId", auctionId))
            .during(Duration.ofSeconds(durationSeconds)).on(
                    exec(http("submit bid")
                            .post(session -> "/api/auctions/" + session.getString("auctionId") + "/bids")
                            .body(StringBody(session -> "{\"userId\":" + session.getLong("userId") + "}"))
                            .asJson()
                            .check(status().in(201, 409)))
                            .pause(Duration.ofMillis(pauseMillis)));

    public AuctionLoadSimulation() {
        if (virtualUsers < 1 || rampSeconds < 1 || durationSeconds < 1 || pauseMillis < 0) {
            throw new IllegalArgumentException("users/rampSeconds/durationSeconds must be positive; pauseMillis cannot be negative");
        }
        setUp(biddingScenario.injectOpen(rampUsers(virtualUsers).during(Duration.ofSeconds(rampSeconds))))
                .protocols(httpProtocol);
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Provide -D" + name + "=...");
        }
        return value;
    }
}
