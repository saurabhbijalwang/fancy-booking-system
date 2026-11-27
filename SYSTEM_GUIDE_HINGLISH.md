# Auction System Guide (Hinglish)

## 1. Ye system kya karta hai?

Ye Java 21 aur Spring Boot par bana timed auction demo hai. Users/bidders create hote hain, auction start hoti hai, aur bidder auction ke dauran bid place kar sakta hai. Har accepted bid current amount ko 1,000 se badhati hai. Payments process nahi hote.

## 2. Ek bid ka flow

1. Browser `POST /api/auctions/{auctionId}/bids` request bhejta hai, jisme `userId` hota hai.
2. `AuctionController` request ko `AuctionService.placeBid()` tak bhejta hai.
3. Service database mein auction row par write lock leti hai. Isse same auction par ek waqt mein ek hi request current bid/sequence update karti hai.
4. Service auction open hai aur bidder maujood hai, ye verify karti hai; phir bid amount aur sequence update karke bid save karti hai.
5. Transaction commit hone ke baad latest auction state WebSocket subscribers ko bheji jati hai.
6. Browser WebSocket event ke saath har 2 seconds mein status poll bhi karta hai. Isliye selected auction ID dekhne wale tabs ko updated total milta hai.

## 3. Bid endpoint — line-by-line samjhaav

File: `src/main/java/com/example/auction/api/AuctionController.java`

```java
@PostMapping("/{auctionId}/bids") // Is URL par HTTP POST bid request accept hoti hai.
public ResponseEntity<BidResponse> placeBid( // Method bid ka response HTTP response ke roop mein deta hai.
        @PathVariable long auctionId, // URL se auction ki ID leta hai.
        @Valid @RequestBody PlaceBidRequest request) { // JSON body ko read aur validate karta hai.
    return ResponseEntity.status(201).body( // Successful bid ke liye HTTP 201 response banata hai.
            auctionService.placeBid(auctionId, request.userId())); // Business logic ko auction ID aur bidder ID deta hai.
}
```

## 4. Race condition ko kaise handle kiya hai?

File: `src/main/java/com/example/auction/repository/AuctionRepository.java`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE) // Database row par write lock leta hai.
@Query("select a from Auction a where a.id = :id") // Di hui ID ki auction row dhoondhta hai.
Optional<Auction> findByIdForUpdate(@Param("id") Long id); // Locked row deta hai; row na ho toh empty result.
```

File: `src/main/java/com/example/auction/service/AuctionService.java`

```java
@Transactional // Lock, update aur bid save ek database transaction mein rakhta hai.
public BidResponse placeBid(long auctionId, long userId) { // Ek bidder ki bid process karta hai.
    Instant now = clock.instant(); // Current time leta hai.
    Auction auction = auctionRepository.findByIdForUpdate(auctionId) // Auction row lock ke saath load karta hai.
            .orElseThrow(() -> new ResourceNotFoundException("Auction " + auctionId + " was not found")); // Missing auction par error deta hai.
    if (auction.getStatus() != AuctionStatus.OPEN || !now.isBefore(auction.getEndsAt())) { // Auction closed/expired hai ya nahi check karta hai.
        if (auction.getStatus() == AuctionStatus.OPEN) { // Expired par abhi bhi OPEN ho toh:
            auction.end(); // Auction ko ENDED mark karta hai.
        }
        throw new AuctionConflictException("Auction is closed; bids are accepted only before its end time"); // Bid reject karta hai.
    }
    Bidder bidder = bidderRepository.findById(userId) // Bidder ko database se load karta hai.
            .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found")); // Unknown bidder par error deta hai.
    long amount = Math.addExact(auction.getCurrentBid(), BID_INCREMENT); // Current bid mein 1,000 safely add karta hai.
    auction.acceptBid(amount); // Auction ka current amount aur sequence update karta hai.
    Bid bid = bidRepository.save(new Bid(auction, bidder, auction.getBidSequence(), amount, now)); // Nayi bid ko persist karta hai.
    eventPublisher.publishEvent(new AuctionUpdatedEvent(AuctionResponse.from(auction, now))); // Commit ke baad broadcast ke liye event publish karta hai.
    return BidResponse.from(bid); // API ke liye saved bid ka response banata hai.
}
```

**Example:** 2,000 par shuru hui auction mein do simultaneous bids lock ke peeche sequentially process hongi: pehli 3,000, doosri 4,000. Same auction ke liye updates serialize hote hain; alag auctions ke locks alag hote hain.

## 5. WebSocket broker kya hai? Kafka hai?

**Kafka use nahi hua hai.** `pom.xml` mein Kafka dependency nahi hai. Project Spring ka `enableSimpleBroker("/topic")` use karta hai. Ye Spring application ke andar chalne wala simple, in-memory STOMP message broker hai; alag Kafka server/service nahi.

File: `src/main/java/com/example/auction/config/WebSocketConfiguration.java`

```java
@EnableWebSocketMessageBroker // Spring WebSocket ke upar STOMP message handling enable karta hai.
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer { // Broker/endpoints ki settings deta hai.
    @Override // Spring ke broker configuration method ko override karta hai.
    public void configureMessageBroker(MessageBrokerRegistry registry) { // Message destinations set karta hai.
        registry.enableSimpleBroker("/topic"); // App ke andar /topic subscribers ko messages bhejta hai.
        registry.setApplicationDestinationPrefixes("/app"); // Client-to-application messages ke liye /app prefix rakhta hai.
    } // Broker configuration khatam.
    @Override // WebSocket endpoint configuration method ko override karta hai.
    public void registerStompEndpoints(StompEndpointRegistry registry) { // Client connection endpoint register karta hai.
        registry.addEndpoint("/ws").withSockJS(); // Browser /ws par SockJS ke through connect kar sakta hai.
    } // Endpoint configuration khatam.
} // Configuration class khatam.
```

Successful database commit ke baad broadcast hota hai:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Sirf transaction commit hone ke baad chalta hai.
public void broadcast(AuctionUpdatedEvent event) { // Auction update event receive karta hai.
    messagingTemplate.convertAndSend( // STOMP message ko destination par bhejta hai.
            "/topic/auctions/" + event.auction().id(), event.auction()); // Usi auction ID ke subscribers ko latest state deta hai.
}
```

**Limitation:** Simple broker ek application instance ke andar hota hai. Multiple app instances mein shared messaging chahiye ho toh external broker (jaise RabbitMQ) ya suitable messaging setup chahiye. Current single-instance POC mein Kafka configured nahi hai.

## 6. Database aur migrations

- Default config `jdbc:postgresql://localhost:5432/auction` use karti hai. Credentials environment variables se override ho sakte hain; defaults development ke liye `auction` / `auction` hain.
- `docker-compose.yml` PostgreSQL container start karta hai aur `auction` database/user configure karta hai.
- `application-local.yml` H2 file database use karta hai; `local` profile ke saath chalane par PostgreSQL nahi use hota.
- Flyway migration `src/main/resources/db/migration/V1__create_auction_tables.sql` mein `bidder`, `auction`, aur `bid` tables banati hai. Flyway tables/schema manage karta hai; PostgreSQL database service khud available honi chahiye.
- Bidder rows `UserService` ke JDBC batch insert se bante hain. `created_at` PostgreSQL ko dene se pehle UTC `OffsetDateTime` mein convert hota hai.

## 7. Useful API routes

| Kaam | Method aur route |
|---|---|
| Bidders banana | `POST /api/users` |
| Auction banana | `POST /api/auctions` |
| Auction status dekhna | `GET /api/auctions/{id}` |
| Bid place karna | `POST /api/auctions/{id}/bids` |
| Auction report | `GET /api/auctions/{id}/report` |
| CSV report | `GET /api/auctions/{id}/report.csv` |

Ye POC real authentication/login implement nahi karta. Bid request mein bheja gaya `userId` bidder ko identify karta hai.

## 8. Tests aur load test

- `AuctionApplicationTests` simultaneous bids ko exercise karta hai aur sequence/amount verify karta hai.
- `AuctionLoadSimulation` Gatling ke through concurrent HTTP bid requests generate karta hai.
- Tests run karne ke liye: `mvn test`
