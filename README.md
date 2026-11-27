# Timed Auction Platform

Java 21 and Spring Boot application for time-bound auctions. It provides bidder and auction APIs, PostgreSQL persistence, race-safe bid processing, live status updates, downloadable reports, and Gatling load simulations. Each accepted bid increases the current bid by 1,000; payment processing is not included.

## Purpose and technology

The application demonstrates an end-to-end auction workflow: create bidders, open a timed auction, accept concurrent bids without duplicate bid sequences, and report the result.

- **Language and framework:** Java 21, Spring Boot 3
- **Database and migrations:** PostgreSQL, Spring Data JPA, Flyway
- **HTTP API and UI:** Spring MVC REST APIs, Thymeleaf
- **Live updates:** WebSocket with SockJS/STOMP and Spring's in-memory simple broker
- **Load testing:** Gatling

## Current architecture

The application runs as a single Spring Boot instance. Bid updates are serialized per auction with a PostgreSQL row lock. Live events use Spring's in-memory simple broker; Kafka is not configured, and the broker does not share messages across multiple application instances. The API also does not implement authenticated user accounts: bid requests identify a bidder with a supplied `userId`.

## Run locally

Prerequisites: Java 21, Maven, and Docker.

```powershell
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080). PostgreSQL is exposed on port 5432. The default local credentials are for development only.

If Docker Desktop/Engine is unavailable, run with the persistent local H2 profile instead:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The H2 profile stores data under `./data/auction`; use PostgreSQL via Docker for PostgreSQL-specific validation.

To run tests without Docker:

```powershell
mvn test
```

## API examples

Create 10 users:

```http
POST /api/users
Content-Type: application/json

{"count":10}
```

Start a 15-minute auction (it starts immediately):

```http
POST /api/auctions
Content-Type: application/json

{"title":"Demo auction","durationMinutes":15,"startingBid":0}
```

Place a bid:

```http
POST /api/auctions/1/bids
Content-Type: application/json

{"userId":1}
```

Read status/report:

```http
GET /api/auctions/1
GET /api/auctions/1/report
GET /api/auctions/1/report.csv
```

Auction numbers are zero-padded database IDs (`0001`, `0002`, ...). Bids arriving at or after the server-side end time are rejected. A user can bid multiple times. The report lists each accepted bid and each bidder's count/latest bid; it does not calculate money spent.

The UI receives committed bid updates over SockJS/STOMP at `/ws` on `/topic/auctions/{auctionId}`. When an auction ID is selected, its status also refreshes automatically every two seconds, so users viewing the same auction see current bid totals without manually refreshing.

## Gatling load test

Create enough bidders and an open auction through the UI/API, then run Gatling against that auction:

```powershell
mvn gatling:test "-Dgatling.simulationClass=com.example.auction.AuctionLoadSimulation" `
  "-DbaseUrl=http://localhost:8080" "-DauctionId=1" "-DfirstUserId=1" `
  "-Dusers=1000" "-DrampSeconds=60" "-DdurationSeconds=60" "-DpauseMillis=1000"
```

The test uses one virtual user per bidder ID starting at `firstUserId`, submits bids at the configured interval, and treats accepted (201) and closed-auction (409) responses as expected. Run a one-million-user test only with distributed Gatling injectors and sufficient pre-created bidder records; a single workstation is not a capacity claim.

## Current scope and limitations

The auction row is locked during bid acceptance to guarantee a gap-free sequence and correct increment for one auction. That intentionally serializes bids for the same auction and limits throughput for that auction. Creating one million users is supported in bounded database batches, but does not mean one million live connections or one million bids/second. Use distributed load generators and measure the actual database/app deployment before making capacity claims. The current report endpoint returns the full bid history and is intended for small-to-moderate auction histories.
