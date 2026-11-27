Mujhe main se 2 branch banani hai - pehle problem wali, fir usi se fix wali. Merge nahi karna hai, sirf PR dikhana hai.

PART 1 - PROBLEM WALI BRANCH:
1. git checkout main
2. git checkout -b feature/report-nplus1-problem
3. Isme N+1 PROBLEM wala code likh - FIX mat lagana:
   - User Entity: id, name, email
   - Booking Entity: id, fancyNumber, status, @ManyToOne(fetch = LAZY) User user
   - BookingRepository: JpaRepository<Booking, Long>
   - ReportService: findAll() se saari Booking lao, fir for loop me har Booking ka b.getUser().getName() print karo - Har loop me alag query jayegi, yahi N+1 hai
   - Koi JOIN FETCH, EntityGraph, Eager mat lagana
   - Koi Kafka, Redis, Distributed mat lagana
4. git add .
5. git commit -m "problem: N+1 query in report - lazy loading causes 1+100 queries"
6. git push -u origin feature/report-nplus1-problem

PART 2 - FIX WALI BRANCH:
1. git checkout feature/report-nplus1-problem
2. git checkout -b fix/report-nplus1
3. Isme sirf N+1 FIX karo, baaki kuch mat chhedna:
   - BookingRepository me ye method add karo:
     @Query("SELECT b FROM Booking b JOIN FETCH b.user")
     List<Booking> findAllWithUser();
   - ReportService me findAll() ki jagah findAllWithUser() call karo
   - Entity me LAZY hi rehne do, Eager mat karna
4. git add .
5. git commit -m "fix: solve N+1 with JOIN FETCH - 101 queries to 1 query"
6. git push -u origin fix/report-nplus1

PART 3 - PR BANAO, MERGE MAT KARNA:
- GitHub pe fix/report-nplus1 se feature/report-nplus1-problem pe PR banao
- PR Title: fix: Solve N+1 problem in report with JOIN FETCH
- PR Description:
  Problem: Report me 101 Queries chal rahi thi - 1 Booking ki + 100 User ki
  Fix: JOIN FETCH lagaya, ab 1 Query chalegi
  Merge mat karna, sirf review ke liye hai

Main branch aur distributed branch ko mat chhedna.