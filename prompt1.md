Build a phase-wise Java POC for a timed auction system.

TECH STACK
- Java 21
- Spring Boot 3.x
- Maven
- PostgreSQL as the source of truth
- Thymeleaf for a simple web UI
- WebSocket for live dashboard updates
- JUnit 5 and Testcontainers for tests
- Gatling for load testing
- Docker Compose for local development

CORE REQUIREMENTS
1. Allow an admin to dynamically create a configurable number of users, for example 1,000,000.
2. Allow an admin to create and start an auction. Assign auction numbers automatically, starting with 0001.
3. Each auction has a configurable duration; default it to 15 minutes.
4. Each accepted bid increases the current bid by exactly 1,000.
5. Assign every accepted bid a unique, increasing sequence number within its auction.
6. Record every accepted bid permanently, including auction number, bid sequence, user ID, bid amount, and server-side timestamp.
7. Display live auction status: current bid, total accepted bids, remaining time, and recent bid activity.
8. When the auction timer ends, reject further bids and declare the user who placed the highest accepted bid as the winner.
9. Generate a final report showing:
   - auction number, start/end time, and final bid
   - winner and winning bid
   - every accepted bid in order
   - each user's bid count and latest bid
   - total number of accepted bids
10. Provide report export as CSV and JSON.
11. Do not implement real payments. Clearly label any “total spent” value as unavailable/not applicable unless a payment model is explicitly added.

CORRECTNESS AND CONCURRENCY
- Use server time as the authority for auction start and end.
- Bids are accepted only while the auction is open and before its end time.
- Make bid acceptance atomic so simultaneous requests cannot receive the same sequence number or overwrite each other's bid.
- Persist the bid and update the auction's current bid/sequence consistently. Use PostgreSQL transactions and an appropriate locking or atomic-update strategy.
- Define and test the exact behavior for a bid arriving at the closing boundary.
- Never trust a client-provided timestamp, current bid, sequence number, or winner.
- Do not create one operating-system/JVM thread per simulated user.
- Do not claim that virtual threads alone make one million simultaneous database writes feasible.
- Keep the correctness-critical bid history in PostgreSQL. Do not silently drop bids or return success when a bid was rejected.

PHASES
Phase 1 — Domain and persistence:
- Create the schema/entities for User, Auction, and Bid.
- Add database migrations and basic unit/integration tests.

Phase 2 — Auction API and correctness:
- Implement user creation, auction creation/start, bid submission, auction status, and final report APIs.
- Add concurrency tests proving that simultaneous accepted bids have unique sequence numbers and correct +1,000 increments.
- Test bids before opening, after closing, and at the closing boundary.

Phase 3 — UI and reporting:
- Build a simple admin page to create users and start an auction.
- Build a live auction page with current bid, bid count, countdown, and recent activity.
- Add the final report page and CSV/JSON downloads.
- Use WebSocket updates for the dashboard; avoid broadcasting every event individually to an unbounded number of clients without measuring the impact.

Phase 4 — Load simulation:
- Provide a Gatling scenario with configurable user count, ramp-up, duration, and bid rate.
- Support testing up to 1,000,000 virtual users using distributed load generators; do not try to run all load generators inside the application JVM.
- Report accepted/rejected bids, throughput, latency percentiles, errors, and final database/report consistency.
- State the hardware and load-generator setup used. Do not claim a one-million-user capacity unless it has actually been measured.

DELIVERABLES
- Working application with documented run instructions.
- Docker Compose configuration for the app and PostgreSQL.
- API documentation and sample requests.
- Focused automated tests for auction timing, bid increments, concurrency, and report accuracy.
- Gatling load-test configuration and instructions.
- A short explanation of known POC limits and what would be required to scale further.

Implement one phase at a time. At the end of each phase, run the relevant tests and report the results before proceeding. Follow existing project conventions and avoid adding infrastructure that is not needed for the POC.