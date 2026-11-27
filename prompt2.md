Fancy Auction ke existing codebase par kaam karo—naya toy/POC application mat banao.
`feature/auction-performance-fixes` branch `feature/timed-auction-platform` se
banao. `feature/distributed-auction` ko touch mat karo. Agar worktree dirty ho ya
branch state expected se alag ho, existing changes preserve karo aur pehle report karo.

Ek hi run mein neeche ke phases sequence mein complete karo; har phase ke baad
focused tests chalao aur mujhe chat mein Hinglish progress update do. Har phase par
approval ke liye mat ruko; blocker ya destructive DB change aaye toh ruk kar poochho.

Pehle current system ka baseline verify karo: bidder creation, auction lifecycle,
PostgreSQL persistence, concurrent bids, bid sequence, reports aur live updates.
Existing tests chalao. Bug mile toh evidence/root cause ke saath minimal fix aur
regression test banao. Jo feature/optimization already sahi hai, usse duplicate mat karo.

Phase 1 — Concurrency:
- Existing simultaneous-bid test ko inspect karo.
- Test mein `ExecutorService`/bounded `ThreadPoolExecutor` ka pool size aur shutdown
  explicit, samajhne layak rakho.
- `ConcurrentHashMap` ko concurrent test results collect/aggregate karne ke liye
  use karke atomic `compute`/`merge` behavior test karo.
- `ConcurrentHashMap` ko PostgreSQL auction/bid data ka source of truth mat banao.
  `volatile` visibility deta hai, atomic increment ya lock ka replacement nahi—ye
  difference bhi code comments se samjhao.

Phase 2 — Entity relations aur N+1:
- Existing mappings inspect karo; `Auction`-`Bid`, `Bidder`-`Bid` relations, LAZY
  loading, ownership aur cascade verify karo.
- Query count/statistics se N+1 reproduce karo, phir Fetch Join, EntityGraph,
  batch fetching ya DTO projection mein suitable fix implement karke test karo.
- ManyToMany sirf real auction feature ke liye add karo (jaise Auction-Tag), schema,
  API aur tests ke saath; artificial relation mat banao.
- GraphQL apne aap N+1 fix nahi karta. GraphQL add karna justified ho tabhi
  DataLoader/batching ke saath add karo; warna existing REST API rakho.

Phase 3 — Transactions, caches aur locks:
- `@Transactional` propagation, isolation levels aur Hibernate Session/EntityManager
  lifecycle ko current code ke context mein verify karo.
- First-level cache aur second-level cache ko alag test karo. L2 provider dependency
  add karne se pehle Hibernate version compatibility verify karo; sirf justified,
  low-change data cache karo aur hit/miss/invalidation test karo.
- Existing pessimistic lock ko `@Version` optimistic locking ke saath compare karo. pasessitmistic bhi 
  Concurrent bid correctness aur expected conflicts tests se verify karo.

Phase 4 — Final verification:
- `mvn test` chalao aur PostgreSQL-specific behavior ko possible ho toh PostgreSQL
  par integration test karo; H2 result ko PostgreSQL proof mat batao.
- Small, bounded concurrency/load validation karo. Failures aur unverified cheezein
  clearly report karo; production-ready claim mat karo bina evidence ke.

Code comments English mein important class/method/complex logic ke just upar likho;
har obvious line par comment mat lagao. README ya separate documentation file mat
banao. Har phase ke end par Hinglish mein kya badla, kya test hua, aur kya result
aaya batao. Commit/push mat karo.