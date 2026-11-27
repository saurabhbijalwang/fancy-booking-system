# Auction Performance Changes - Hinglish Summary

## Kaam ka context

Ye changes existing Fancy Auction project mein kiye gaye hain. Naya demo ya toy
application nahi banaya gaya. Kaam `feature/auction-performance-fixes` branch par
hai. Is branch ko push nahi kiya gaya.

## 1. Concurrent bidding test

- Simultaneous bids test mein 12 worker threads wala bounded
  `ThreadPoolExecutor` use hota hai. Worker aur queue size fixed hain.
- Test khud executor ko shutdown karta hai aur workers ke terminate hone ka wait
  karta hai.
- `ConcurrentHashMap.merge` se successful bids ka count thread-safe tareeke se
  collect hota hai.
- Is map ka use sirf test results ke liye hai. Auction ya bid ka asli data
  PostgreSQL mein hi rehta hai.
- `volatile` visibility deta hai, lekin `count++` jaise read-modify-write ko
  atomic nahi banata; isliye yahan atomic `merge` use hua.
- Test verify karta hai ki 12 bids accept hon, unke sequence numbers 1 se 12 hon,
  aur bid amounts sahi hon.

## 2. Relations aur N+1 query

- Existing `Bid` ke `Auction` aur `Bidder` relations LAZY `ManyToOne` hain.
  Is feature ke liye artificial `ManyToMany`, GraphQL ya nayi relation add
  nahi ki gayi.
- Auction report query mein bidder ka fetch join pehle se tha; use duplicate
  karne ke bajaye SQL query count se verify kiya.
- 3 bidders wali report ke liye 2 SQL statements chalti hain—bidders ke liye
  har row par alag query nahi chalti.

## 3. Hibernate cache aur locking

- First-level cache test ek hi persistence context mein ek Auction ko do baar
  read karta hai aur verify karta hai ki ek hi entity instance aur ek SQL query
  use hoti hai.
- Sirf immutable `Bidder` entities ke liye Hibernate second-level cache add
  kiya gaya. Ehcache region `bidder` mein 15-minute time-to-idle aur maximum
  10,000 entries configure hain.
- Missing cache region ab silently default cache se create nahi hota: startup
  fail-fast configuration rakhi hai. Test logs mein configured `ehcache.xml`
  se region load hona verify hua.
- L2 tests cache miss, cache hit (SQL query ke bina), aur bidder delete ke baad
  cache eviction verify karte hain.
- Normal bid flow mein `PESSIMISTIC_WRITE` lock auction row ko serialize karta
  hai. Alag test verify karta hai ki stale update ko existing `@Version`
  optimistic locking reject karta hai. Dono ka kaam alag hai; production bid
  flow se pessimistic lock nahi hataya gaya.
- Services ka `@Transactional` default `REQUIRED` propagation aur database ka
  default isolation use karta hai. Koi unnecessary custom propagation ya
  isolation level add nahi kiya.

## AuctionController.java ka role

`src/main/java/com/example/auction/api/AuctionController.java` is kaam mein
modify nahi hua. Ye existing REST endpoints ko `AuctionService` se connect
karta hai:

- `POST /api/auctions` - auction banana
- `GET /api/auctions/{auctionId}` - auction details lena
- `POST /api/auctions/{auctionId}/bids` - bid place karna
- `GET /api/auctions/{auctionId}/report` - report lena
- `GET /api/auctions/{auctionId}/report.csv` - CSV report download karna

CSV output mein bidder name ke quotes escape hote hain. Controller ko change
karne ki zaroorat nahi padi, kyunki performance fixes service, cache
configuration aur tests mein the.

## Verification aur baaki

- `mvn test` pass hua: 7 tests, 0 failures.
- Tests H2 par chale. PostgreSQL port reachable tha, lekin configured `auction`
  database ko test data ya migration se touch karna safe confirm nahi tha; isliye
  PostgreSQL validation skip ki gayi. H2 result ko PostgreSQL proof nahi
  maana gaya.
- `prompt2.md` original execution prompt hai; user ki request par is commit mein
  include kiya gaya hai. Branch push nahi ki gayi.
