Mujhe Hibernate ke 5 topic alag-alag phase me implement karne hai. Har phase me 2 branch hogi - pehle problem wali, fir usi se fix wali. Koi bhi PR merge nahi karna hai, sirf PR bana ke chhodna hai. Main branch aur distributed branch ko mat chhedna. Har phase ke baad rukna, main bolunga tabhi agla phase karna.

PHASE 1 - BATCH PROCESSING / BULK INSERT:
- Branch: feature/batch-problem (main se banao)
  Problem code: BookingService me 1000 Booking ko for loop me ek-ek karke save() karo, batch_size mat lagana, flush/clear mat karna. Ye slow hai.
  Commit: problem: bulk insert slow - 1000 queries for 1000 bookings
  Push karo.
- Branch: fix/batch-processing (feature/batch-problem se banao)
  Fix: application.properties me hibernate.jdbc.batch_size=50 lagao, order_inserts=true, order_updates=true lagao. Service me har 50 record ke baad flush() + clear() karo.
  Commit: fix: batch processing - 1000 inserts optimized with batch_size 50
  Push karo, PR banao fix/batch-processing -> feature/batch-problem, PR Title: fix: Optimize bulk insert with batch processing, Description: Pehle 1000 queries, ab batch me. Merge mat karna.

PHASE 2 - CUSTOM ID GENERATOR:
- Branch: feature/custom-id-problem (main se banao)
  Problem code: Booking me normal @GeneratedValue se 1,2,3 ID aa raha hai. Fancy Number 0001, 0002 chahiye par ban nahi raha.
  Commit: problem: default ID generator cannot generate fancy number
  Push karo.
- Branch: fix/custom-id-generator (feature/custom-id-problem se banao)
  Fix: FancyNumberGenerator class banao jo IdentifierGenerator implement kare, generate() me DB se max fancyNumber leke +1 karke 4-digit format 0001 me return kare. Booking entity me @GenericGenerator se isko lagao.
  Commit: fix: custom ID generator for fancy number 0001 format
  Push karo, PR banao fix/custom-id-generator -> feature/custom-id-problem, Merge mat karna.

PHASE 3 - FIRST-LEVEL / SECOND-LEVEL CACHE:
- Branch: feature/cache-problem (main se banao)
  Problem code: UserService me ek hi User ko baar-baar findById se lao, har baar DB query ja rahi hai. Cache mat lagana.
  Commit: problem: repeated DB hits for same user - no second level cache
  Push karo.
- Branch: fix/cache-second-level (feature/cache-problem se banao)
  Fix: User entity pe @Cacheable + @Cache(usage = READ_WRITE) lagao. application.properties me hibernate.cache.use_second_level_cache=true aur EhCache provider lagao. Explain karo first-level session ke andar hai, second-level session ke bahar hai.
  Commit: fix: enable second level cache with EhCache
  Push karo, PR banao, Merge mat karna.

PHASE 4 - INHERITANCE STRATEGIES:
- Branch: feature/inheritance-problem (main se banao)
  Problem code: Payment abstract class banao, UPI, Card, NetBanking uske child hai. Sabko SINGLE_TABLE me daal do, bahut saare NULL column aa rahe hai.
  Commit: problem: SINGLE_TABLE causes many NULL columns for payment types
  Push karo.
- Branch: fix/inheritance-joined (feature/inheritance-problem se banao)
  Fix: Payment pe @Inheritance(strategy = JOINED) lagao. Payment ek table, UPI/Card/NetBanking alag-alag table. Comment me likho SINGLE_TABLE vs JOINED vs TABLE_PER_CLASS ka difference.
  Commit: fix: use JOINED inheritance for Payment
  Push karo, PR banao, Merge mat karna.

PHASE 5 - N+1 PROBLEM:
- Branch: feature/report-nplus1-problem (main se banao)
  Problem code: Booking me @ManyToOne(fetch = LAZY) User user. ReportService me findAll() karke loop me b.getUser().getName() karo. JOIN FETCH mat lagana.
  Commit: problem: N+1 query in report - 101 queries
  Push karo.
- Branch: fix/report-nplus1 (feature/report-nplus1-problem se banao)
  Fix: BookingRepository me @Query("SELECT b FROM Booking b JOIN FETCH b.user") List<Booking> findAllWithUser() banao. Service me findAll() ki jagah ye call karo. Entity me LAZY hi rakho, Eager mat karna.
  Commit: fix: solve N+1 with JOIN FETCH - 101 queries to 1 query
  Push karo, PR banao fix/report-nplus1 -> feature/report-nplus1-problem, Merge mat karna.

Har phase ke baad git status dikhana aur rukna.