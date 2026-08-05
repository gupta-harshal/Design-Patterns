import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Movie Ticket Booking (BookMyShow-lite) — single-file LLD sketch.
 *
 * Central invariant: for a given (show, seat) there is AT MOST ONE confirmed booking.
 * It is enforced by ShowInventory, whose methods are synchronized and use a
 * two-pass "check everything, then write everything" protocol so seat selection
 * is all-or-nothing.
 */

// ─────────────────────────────────────────────────────────────
// 1. TIME — injectable so lock TTLs are testable without sleeping
// ─────────────────────────────────────────────────────────────
interface Clock {
    long nowMillis();
}

class SystemClock implements Clock {
    @Override
    public long nowMillis() {
        return System.currentTimeMillis();
    }
}

/** Virtual clock: lets the demo fast-forward past a lock TTL instantly. */
class SimulatedClock implements Clock {
    private volatile long now;

    SimulatedClock(long startMillis) {
        this.now = startMillis;
    }

    @Override
    public long nowMillis() {
        return now;
    }

    void advanceSeconds(long seconds) {
        now += seconds * 1000L;
    }
}

class TimeUtil {
    static long toMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    static String pretty(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("EEE dd MMM, HH:mm"));
    }
}

// ─────────────────────────────────────────────────────────────
// 2. ENUMS
// ─────────────────────────────────────────────────────────────
enum SeatType {
    REGULAR(200.0),
    PREMIUM(350.0);

    private final double basePrice;

    SeatType(double basePrice) {
        this.basePrice = basePrice;
    }

    double getBasePrice() {
        return basePrice;
    }
}

enum SeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED
}

enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    FAILED,
    CANCELLED
}

// ─────────────────────────────────────────────────────────────
// 3. CATALOG — City > Cinema > Screen > Seat, and Movie/Show
// ─────────────────────────────────────────────────────────────
class Seat {
    private final String id;
    private final String rowLabel;
    private final int number;
    private final SeatType type;

    Seat(String rowLabel, int number, SeatType type) {
        this.rowLabel = rowLabel;
        this.number = number;
        this.type = type;
        this.id = rowLabel + number;
    }

    String getId() {
        return id;
    }

    String getRowLabel() {
        return rowLabel;
    }

    int getNumber() {
        return number;
    }

    SeatType getType() {
        return type;
    }
}

class Screen {
    private final String id;
    private final String name;
    private final List<Seat> seats;
    private final Map<String, Seat> seatIndex = new LinkedHashMap<>();

    Screen(String id, String name, List<Seat> seats) {
        this.id = id;
        this.name = name;
        this.seats = List.copyOf(seats);
        for (Seat seat : this.seats) {
            seatIndex.put(seat.getId(), seat);
        }
    }

    /** Builds a rectangular layout; the last `premiumRows` rows are PREMIUM. */
    static Screen rectangular(String id, String name, int rows, int seatsPerRow, int premiumRows) {
        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String rowLabel = String.valueOf((char) ('A' + r));
            SeatType type = (r >= rows - premiumRows) ? SeatType.PREMIUM : SeatType.REGULAR;
            for (int n = 1; n <= seatsPerRow; n++) {
                seats.add(new Seat(rowLabel, n, type));
            }
        }
        return new Screen(id, name, seats);
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    List<Seat> getSeats() {
        return seats;
    }

    Seat findSeat(String seatId) {
        return seatIndex.get(seatId);
    }
}

class Cinema {
    private final String id;
    private final String name;
    private final String cityId;
    private final List<Screen> screens = new ArrayList<>();

    Cinema(String id, String name, String cityId) {
        this.id = id;
        this.name = name;
        this.cityId = cityId;
    }

    void addScreen(Screen screen) {
        screens.add(screen);
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getCityId() {
        return cityId;
    }

    List<Screen> getScreens() {
        return screens;
    }
}

class City {
    private final String id;
    private final String name;

    City(String id, String name) {
        this.id = id;
        this.name = name;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }
}

class Movie {
    private final String id;
    private final String title;
    private final int durationMinutes;

    Movie(String id, String title, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    String getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    int getDurationMinutes() {
        return durationMinutes;
    }
}

class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final Cinema cinema;
    private final LocalDateTime startTime;
    private final double priceMultiplier;

    Show(String id, Movie movie, Screen screen, Cinema cinema, LocalDateTime startTime, double priceMultiplier) {
        this.id = id;
        this.movie = movie;
        this.screen = screen;
        this.cinema = cinema;
        this.startTime = startTime;
        this.priceMultiplier = priceMultiplier;
    }

    String getId() {
        return id;
    }

    Movie getMovie() {
        return movie;
    }

    Screen getScreen() {
        return screen;
    }

    Cinema getCinema() {
        return cinema;
    }

    LocalDateTime getStartTime() {
        return startTime;
    }

    double getPriceMultiplier() {
        return priceMultiplier;
    }

    boolean isBookingOpen(long nowMillis) {
        return nowMillis < TimeUtil.toMillis(startTime);
    }

    @Override
    public String toString() {
        return String.format("%s | %s @ %s (%s) | x%.2f",
                id, movie.getTitle(), cinema.getName(), TimeUtil.pretty(startTime), priceMultiplier);
    }
}

// ─────────────────────────────────────────────────────────────
// 4. SEAT LOCK — immutable "who holds what until when"
// ─────────────────────────────────────────────────────────────
class SeatLock {
    private final String seatId;
    private final String bookingId;
    private final String userId;
    private final long expiresAtMillis;

    SeatLock(String seatId, String bookingId, String userId, long expiresAtMillis) {
        this.seatId = seatId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.expiresAtMillis = expiresAtMillis;
    }

    boolean isActive(long nowMillis) {
        return nowMillis < expiresAtMillis;
    }

    boolean isOwnedBy(String bookingId) {
        return this.bookingId.equals(bookingId);
    }

    String getSeatId() {
        return seatId;
    }

    String getBookingId() {
        return bookingId;
    }

    String getUserId() {
        return userId;
    }

    long getExpiresAtMillis() {
        return expiresAtMillis;
    }
}

class LockResult {
    final boolean success;
    final String reason;

    private LockResult(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }

    static LockResult ok() {
        return new LockResult(true, "locked");
    }

    static LockResult fail(String reason) {
        return new LockResult(false, reason);
    }
}

// ─────────────────────────────────────────────────────────────
// 5. SHOW INVENTORY — the ONE concurrency boundary per show
// ─────────────────────────────────────────────────────────────
class ShowInventory {
    private final Show show;
    private final Clock clock;

    // Terminal state: seatId -> bookingId that owns it forever.
    private final Map<String, String> bookedBy = new HashMap<>();
    // Temporary state: seatId -> lock (may already be expired; expiry is lazy).
    private final Map<String, SeatLock> locks = new HashMap<>();

    ShowInventory(Show show, Clock clock) {
        this.show = show;
        this.clock = clock;
    }

    Show getShow() {
        return show;
    }

    /**
     * Status is DERIVED, never stored. An expired lock therefore reads as
     * AVAILABLE immediately — no sweeper thread is needed for correctness.
     */
    synchronized SeatStatus statusOf(String seatId) {
        if (bookedBy.containsKey(seatId)) {
            return SeatStatus.BOOKED;
        }
        SeatLock lock = locks.get(seatId);
        if (lock != null && lock.isActive(clock.nowMillis())) {
            return SeatStatus.LOCKED;
        }
        return SeatStatus.AVAILABLE;
    }

    /**
     * All-or-nothing hold. Two passes inside one synchronized block:
     * pass 1 validates every seat, pass 2 writes. A single-pass version would
     * leave partial locks behind when a later seat turns out to be taken.
     */
    synchronized LockResult lockSeats(List<String> seatIds, String userId, String bookingId, long ttlMillis) {
        if (seatIds == null || seatIds.isEmpty()) {
            return LockResult.fail("no seats requested");
        }

        long now = clock.nowMillis();
        if (!show.isBookingOpen(now)) {
            return LockResult.fail("booking window closed — show already started");
        }

        // Duplicate ids would otherwise pass validation twice and hide a bug.
        Set<String> unique = new LinkedHashSet<>(seatIds);
        if (unique.size() != seatIds.size()) {
            return LockResult.fail("duplicate seat ids in request");
        }

        // PASS 1 — check only, mutate nothing.
        for (String seatId : unique) {
            if (show.getScreen().findSeat(seatId) == null) {
                return LockResult.fail("unknown seat " + seatId + " for screen " + show.getScreen().getName());
            }
            SeatStatus status = statusOf(seatId);
            if (status != SeatStatus.AVAILABLE) {
                return LockResult.fail("seat " + seatId + " is " + status);
            }
        }

        // PASS 2 — commit.
        long expiresAt = now + ttlMillis;
        for (String seatId : unique) {
            locks.put(seatId, new SeatLock(seatId, bookingId, userId, expiresAt));
        }
        return LockResult.ok();
    }

    /**
     * Confirms only if EVERY seat still carries an ACTIVE lock owned by this
     * booking. This is where an expired-then-poached seat is caught.
     */
    synchronized boolean confirmSeats(List<String> seatIds, String bookingId) {
        long now = clock.nowMillis();

        // PASS 1 — verify ownership of all seats.
        for (String seatId : seatIds) {
            SeatLock lock = locks.get(seatId);
            boolean held = lock != null && lock.isActive(now) && lock.isOwnedBy(bookingId);
            if (!held) {
                return false;
            }
        }

        // PASS 2 — promote locks to permanent bookings.
        for (String seatId : seatIds) {
            bookedBy.put(seatId, bookingId);
            locks.remove(seatId);
        }
        return true;
    }

    /** Releases only the locks this booking still owns (never someone else's). */
    synchronized void releaseSeats(List<String> seatIds, String bookingId) {
        for (String seatId : seatIds) {
            SeatLock lock = locks.get(seatId);
            if (lock != null && lock.isOwnedBy(bookingId)) {
                locks.remove(seatId);
            }
        }
    }

    synchronized long lockExpiryOf(String seatId) {
        SeatLock lock = locks.get(seatId);
        return lock == null ? -1L : lock.getExpiresAtMillis();
    }

    synchronized long countBooked() {
        return bookedBy.size();
    }
}

// ─────────────────────────────────────────────────────────────
// 6. PRICING STRATEGY
// ─────────────────────────────────────────────────────────────
interface PricingStrategy {
    double priceFor(Show show, List<Seat> seats);
}

class SeatTypePricingStrategy implements PricingStrategy {
    @Override
    public double priceFor(Show show, List<Seat> seats) {
        double total = 0.0;
        for (Seat seat : seats) {
            total += seat.getType().getBasePrice() * show.getPriceMultiplier();
        }
        return total;
    }
}

// ─────────────────────────────────────────────────────────────
// 7. PAYMENT SEAM
// ─────────────────────────────────────────────────────────────
interface PaymentProvider {
    boolean charge(String bookingId, double amount);

    void refund(String bookingId, double amount);
}

class AlwaysSucceedsPaymentProvider implements PaymentProvider {
    @Override
    public boolean charge(String bookingId, double amount) {
        return true;
    }

    @Override
    public void refund(String bookingId, double amount) {
        System.out.printf("      [payment] refunded %.2f for %s%n", amount, bookingId);
    }
}

class DecliningPaymentProvider implements PaymentProvider {
    @Override
    public boolean charge(String bookingId, double amount) {
        return false;
    }

    @Override
    public void refund(String bookingId, double amount) {
        // Nothing was ever charged.
    }
}

// ─────────────────────────────────────────────────────────────
// 8. BOOKING
// ─────────────────────────────────────────────────────────────
class Booking {
    private final String id;
    private final String userId;
    private final Show show;
    private final List<Seat> seats;
    private final double amount;
    private final long lockExpiresAtMillis;
    private volatile BookingStatus status;

    Booking(String id, String userId, Show show, List<Seat> seats, double amount, long lockExpiresAtMillis) {
        this.id = id;
        this.userId = userId;
        this.show = show;
        this.seats = seats;
        this.amount = amount;
        this.lockExpiresAtMillis = lockExpiresAtMillis;
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    String getId() {
        return id;
    }

    String getUserId() {
        return userId;
    }

    Show getShow() {
        return show;
    }

    List<Seat> getSeats() {
        return seats;
    }

    List<String> getSeatIds() {
        List<String> ids = new ArrayList<>();
        for (Seat seat : seats) {
            ids.add(seat.getId());
        }
        return ids;
    }

    double getAmount() {
        return amount;
    }

    long getLockExpiresAtMillis() {
        return lockExpiresAtMillis;
    }

    BookingStatus getStatus() {
        return status;
    }

    void setStatus(BookingStatus status) {
        this.status = status;
    }

    String seatLabel() {
        return String.join(",", getSeatIds());
    }
}

class BookingResult {
    final boolean success;
    final String message;
    final Booking booking;

    private BookingResult(boolean success, String message, Booking booking) {
        this.success = success;
        this.message = message;
        this.booking = booking;
    }

    static BookingResult ok(String message, Booking booking) {
        return new BookingResult(true, message, booking);
    }

    static BookingResult fail(String message) {
        return new BookingResult(false, message, null);
    }
}

// ─────────────────────────────────────────────────────────────
// 9. BOOKING SERVICE — orchestration only
// ─────────────────────────────────────────────────────────────
class BookingService {
    private static final long SEAT_LOCK_TTL_MILLIS = 2 * 60 * 1000L; // 2 minutes

    private final Map<String, Show> shows = new LinkedHashMap<>();
    private final Map<String, ShowInventory> inventories = new LinkedHashMap<>();
    // Concurrent: several threads may create bookings at once (see the race demo).
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final PricingStrategy pricingStrategy;
    private final Clock clock;
    private final AtomicInteger bookingCounter = new AtomicInteger(1000);

    BookingService(PricingStrategy pricingStrategy, Clock clock) {
        this.pricingStrategy = pricingStrategy;
        this.clock = clock;
    }

    void registerShow(Show show) {
        shows.put(show.getId(), show);
        inventories.put(show.getId(), new ShowInventory(show, clock));
    }

    List<Show> searchShows(String cityId, String movieId) {
        List<Show> found = new ArrayList<>();
        for (Show show : shows.values()) {
            boolean cityMatches = show.getCinema().getCityId().equals(cityId);
            boolean movieMatches = show.getMovie().getId().equals(movieId);
            if (cityMatches && movieMatches && show.isBookingOpen(clock.nowMillis())) {
                found.add(show);
            }
        }
        return found;
    }

    ShowInventory inventoryOf(String showId) {
        return inventories.get(showId);
    }

    /** Step 1: price the seats and hold them for the TTL. */
    BookingResult startBooking(String userId, String showId, List<String> seatIds) {
        Show show = shows.get(showId);
        if (show == null) {
            return BookingResult.fail("unknown show " + showId);
        }
        ShowInventory inventory = inventories.get(showId);

        List<Seat> seats = new ArrayList<>();
        for (String seatId : seatIds) {
            Seat seat = show.getScreen().findSeat(seatId);
            if (seat == null) {
                return BookingResult.fail("unknown seat " + seatId);
            }
            seats.add(seat);
        }

        String bookingId = "BKG-" + bookingCounter.incrementAndGet();
        LockResult lockResult = inventory.lockSeats(seatIds, userId, bookingId, SEAT_LOCK_TTL_MILLIS);
        if (!lockResult.success) {
            return BookingResult.fail(lockResult.reason);
        }

        double amount = pricingStrategy.priceFor(show, seats);
        long expiresAt = clock.nowMillis() + SEAT_LOCK_TTL_MILLIS;
        Booking booking = new Booking(bookingId, userId, show, seats, amount, expiresAt);
        bookings.put(bookingId, booking);
        return BookingResult.ok("seats held for " + (SEAT_LOCK_TTL_MILLIS / 1000) + "s", booking);
    }

    /**
     * Step 2: charge, then confirm.
     *
     * The dangerous window is between a successful charge and a failed confirm
     * (lock expired mid-payment). That path MUST refund — "money taken, nothing
     * delivered" is the worst failure mode a booking system can have.
     */
    BookingResult payAndConfirm(String bookingId, PaymentProvider paymentProvider) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            return BookingResult.fail("unknown booking " + bookingId);
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            // Idempotency: a double-clicked "Pay" returns the existing confirmation.
            return BookingResult.ok("already confirmed (idempotent replay)", booking);
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return BookingResult.fail("booking is " + booking.getStatus());
        }

        ShowInventory inventory = inventories.get(booking.getShow().getId());

        boolean charged = paymentProvider.charge(bookingId, booking.getAmount());
        if (!charged) {
            // Release immediately rather than making other users wait out the TTL.
            inventory.releaseSeats(booking.getSeatIds(), bookingId);
            booking.setStatus(BookingStatus.FAILED);
            return BookingResult.fail("payment declined — seats released");
        }

        boolean confirmed = inventory.confirmSeats(booking.getSeatIds(), bookingId);
        if (!confirmed) {
            paymentProvider.refund(bookingId, booking.getAmount());
            booking.setStatus(BookingStatus.FAILED);
            return BookingResult.fail("seat lock expired or seats taken — payment refunded");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return BookingResult.ok("confirmed", booking);
    }

    void cancelPending(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }
        inventories.get(booking.getShow().getId()).releaseSeats(booking.getSeatIds(), bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
    }

    void printSeatMap(String showId) {
        Show show = shows.get(showId);
        ShowInventory inventory = inventories.get(showId);
        System.out.println("      Seat map for " + show.getMovie().getTitle()
                + " @ " + TimeUtil.pretty(show.getStartTime())
                + "   (. available   L locked   # booked)");

        String currentRow = null;
        StringBuilder line = new StringBuilder();
        for (Seat seat : show.getScreen().getSeats()) {
            if (!seat.getRowLabel().equals(currentRow)) {
                if (currentRow != null) {
                    System.out.println(line);
                }
                currentRow = seat.getRowLabel();
                line = new StringBuilder("        " + currentRow + " ");
            }
            SeatStatus status = inventory.statusOf(seat.getId());
            char symbol = status == SeatStatus.BOOKED ? '#' : (status == SeatStatus.LOCKED ? 'L' : '.');
            line.append(symbol).append(' ');
        }
        System.out.println(line);
    }
}

// ─────────────────────────────────────────────────────────────
// 10. DEMO
// ─────────────────────────────────────────────────────────────
public class Main {

    private static int checksPassed = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) throws Exception {
        // A fixed "now" makes every run of this demo identical.
        LocalDateTime today = LocalDateTime.of(2026, 3, 14, 17, 0);
        SimulatedClock clock = new SimulatedClock(TimeUtil.toMillis(today));

        BookingService service = new BookingService(new SeatTypePricingStrategy(), clock);

        City bengaluru = new City("C1", "Bengaluru");
        City mumbai = new City("C2", "Mumbai");

        Cinema pvrForum = new Cinema("CIN1", "PVR Forum Mall", bengaluru.getId());
        Cinema inoxGarudaMall = new Cinema("CIN2", "INOX Garuda Mall", bengaluru.getId());
        Cinema pvrPhoenix = new Cinema("CIN3", "PVR Phoenix", mumbai.getId());

        // 5 rows x 6 seats; last 2 rows (D, E) are PREMIUM.
        Screen audi1 = Screen.rectangular("SCR1", "Audi 1", 5, 6, 2);
        Screen audi2 = Screen.rectangular("SCR2", "Audi 2", 5, 6, 2);
        Screen audi3 = Screen.rectangular("SCR3", "Audi 3", 5, 6, 2);
        pvrForum.addScreen(audi1);
        inoxGarudaMall.addScreen(audi2);
        pvrPhoenix.addScreen(audi3);

        Movie dune = new Movie("M1", "Dune: Part Three", 166);
        Movie other = new Movie("M2", "Interstellar (re-release)", 169);

        Show show1 = new Show("SHOW1", dune, audi1, pvrForum, today.withHour(18).withMinute(30), 1.0);
        Show show2 = new Show("SHOW2", dune, audi2, inoxGarudaMall, today.withHour(21).withMinute(45), 1.25);
        Show show3 = new Show("SHOW3", dune, audi3, pvrPhoenix, today.withHour(19).withMinute(0), 1.5);
        Show pastShow = new Show("SHOW0", other, audi1, pvrForum, today.withHour(10).withMinute(0), 1.0);
        service.registerShow(show1);
        service.registerShow(show2);
        service.registerShow(show3);
        service.registerShow(pastShow);

        banner("MOVIE TICKET BOOKING — BookMyShow-lite");
        System.out.println("Simulated now: " + TimeUtil.pretty(today) + "   |   seat lock TTL: 120s");
        System.out.println("Prices: REGULAR 200, PREMIUM 350  (x show multiplier)");

        // ── Scenario 1: search ───────────────────────────────
        section("1. Search shows for 'Dune: Part Three' in Bengaluru");
        List<Show> results = service.searchShows(bengaluru.getId(), dune.getId());
        for (Show show : results) {
            System.out.println("      " + show);
        }
        check("search returns the 2 Bengaluru shows (Mumbai show excluded)", results.size() == 2);

        // ── Scenario 2: happy path ───────────────────────────
        section("2. Alice books A1, A2 on SHOW1 and pays");
        BookingResult aliceStart = service.startBooking("alice", "SHOW1", Arrays.asList("A1", "A2"));
        report("startBooking(alice, [A1,A2])", aliceStart);
        if (aliceStart.success) {
            System.out.printf("      amount = %.2f  (2 x REGULAR 200 x 1.00)%n", aliceStart.booking.getAmount());
            check("A1 is LOCKED while Alice pays",
                    service.inventoryOf("SHOW1").statusOf("A1") == SeatStatus.LOCKED);
            BookingResult alicePay = service.payAndConfirm(aliceStart.booking.getId(), new AlwaysSucceedsPaymentProvider());
            report("payAndConfirm(alice)", alicePay);
            check("Alice's booking is CONFIRMED", alicePay.success
                    && alicePay.booking.getStatus() == BookingStatus.CONFIRMED);
            check("A1 is now BOOKED", service.inventoryOf("SHOW1").statusOf("A1") == SeatStatus.BOOKED);
            check("price is 400.00", Math.abs(aliceStart.booking.getAmount() - 400.0) < 0.001);
        }
        service.printSeatMap("SHOW1");

        // ── Scenario 3: double booking must fail ─────────────
        section("3. Bob tries the SAME seats A1, A2  (double-booking guard)");
        BookingResult bobSame = service.startBooking("bob", "SHOW1", Arrays.asList("A1", "A2"));
        report("startBooking(bob, [A1,A2])", bobSame);
        check("Bob is rejected — no double booking", !bobSame.success);

        // ── Scenario 4: all-or-nothing ───────────────────────
        section("4. Bob asks for A5 (free) + A1 (booked)  (all-or-nothing)");
        BookingResult bobMixed = service.startBooking("bob", "SHOW1", Arrays.asList("A5", "A1"));
        report("startBooking(bob, [A5,A1])", bobMixed);
        check("whole request rejected", !bobMixed.success);
        check("A5 was NOT partially locked — still AVAILABLE",
                service.inventoryOf("SHOW1").statusOf("A5") == SeatStatus.AVAILABLE);

        // ── Scenario 5: TTL expiry ───────────────────────────
        section("5. Carol locks B1, B2 but never pays — TTL expires");
        BookingResult carolStart = service.startBooking("carol", "SHOW1", Arrays.asList("B1", "B2"));
        report("startBooking(carol, [B1,B2])", carolStart);
        check("B1 is LOCKED", service.inventoryOf("SHOW1").statusOf("B1") == SeatStatus.LOCKED);
        service.printSeatMap("SHOW1");

        System.out.println("      ... fast-forwarding the clock by 180s (TTL was 120s) ...");
        clock.advanceSeconds(180);
        check("B1 auto-reverted to AVAILABLE with no sweeper thread",
                service.inventoryOf("SHOW1").statusOf("B1") == SeatStatus.AVAILABLE);

        // ── Scenario 6: someone else takes the expired seats ─
        section("6. Dave books B1, B2 after Carol's lock expired");
        BookingResult daveStart = service.startBooking("dave", "SHOW1", Arrays.asList("B1", "B2"));
        report("startBooking(dave, [B1,B2])", daveStart);
        BookingResult davePay = service.payAndConfirm(daveStart.booking.getId(), new AlwaysSucceedsPaymentProvider());
        report("payAndConfirm(dave)", davePay);
        check("Dave holds B1, B2", davePay.success
                && service.inventoryOf("SHOW1").statusOf("B1") == SeatStatus.BOOKED);

        // ── Scenario 7: late payment on an expired lock ──────
        section("7. Carol finally tries to pay (lock expired AND seats poached)");
        BookingResult carolPay = service.payAndConfirm(carolStart.booking.getId(), new AlwaysSucceedsPaymentProvider());
        report("payAndConfirm(carol)", carolPay);
        check("Carol's payment is refunded, booking FAILED", !carolPay.success);
        check("Dave keeps B1 — it was never stolen back",
                service.inventoryOf("SHOW1").statusOf("B1") == SeatStatus.BOOKED);

        // ── Scenario 8: declined payment releases seats ──────
        section("8. Erin books PREMIUM D1, D2 but the gateway declines");
        BookingResult erinStart = service.startBooking("erin", "SHOW1", Arrays.asList("D1", "D2"));
        report("startBooking(erin, [D1,D2])", erinStart);
        System.out.printf("      amount = %.2f  (2 x PREMIUM 350 x 1.00)%n", erinStart.booking.getAmount());
        check("premium pricing is 700.00", Math.abs(erinStart.booking.getAmount() - 700.0) < 0.001);
        BookingResult erinPay = service.payAndConfirm(erinStart.booking.getId(), new DecliningPaymentProvider());
        report("payAndConfirm(erin)", erinPay);
        check("D1 released immediately, not held for the full TTL",
                service.inventoryOf("SHOW1").statusOf("D1") == SeatStatus.AVAILABLE);

        // ── Scenario 9: show multiplier ──────────────────────
        section("9. Same seats on SHOW2 cost more (x1.25 prime-time multiplier)");
        BookingResult frankStart = service.startBooking("frank", "SHOW2", Arrays.asList("A1", "A2"));
        report("startBooking(frank, SHOW2 [A1,A2])", frankStart);
        System.out.printf("      amount = %.2f  (2 x REGULAR 200 x 1.25)%n", frankStart.booking.getAmount());
        check("multiplier applied: 500.00", Math.abs(frankStart.booking.getAmount() - 500.0) < 0.001);

        // ── Scenario 10: invalid requests ────────────────────
        section("10. Invalid requests");
        report("duplicate seat ids [C1,C1]",
                service.startBooking("gina", "SHOW1", Arrays.asList("C1", "C1")));
        check("duplicates rejected",
                !service.startBooking("gina", "SHOW1", Arrays.asList("C1", "C1")).success);
        report("unknown seat [Z9]", service.startBooking("gina", "SHOW1", Arrays.asList("Z9")));
        report("show that already started", service.startBooking("gina", "SHOW0", Arrays.asList("C1")));
        check("past show rejected", !service.startBooking("gina", "SHOW0", Arrays.asList("C2")).success);

        // ── Scenario 11: real concurrency ────────────────────
        section("11. 8 threads race for the SAME seat E6 on SHOW3");
        int racers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(racers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(racers);
        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < racers; i++) {
            final String user = "racer-" + i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    BookingResult started = service.startBooking(user, "SHOW3", Arrays.asList("E6"));
                    if (!started.success) {
                        rejected.incrementAndGet();
                        return;
                    }
                    BookingResult paid = service.payAndConfirm(started.booking.getId(), new AlwaysSucceedsPaymentProvider());
                    if (paid.success) {
                        confirmed.incrementAndGet();
                        System.out.println("      winner: " + user + " -> " + started.booking.getId());
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        startGate.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("      confirmed = " + confirmed.get() + ", rejected = " + rejected.get());
        check("exactly ONE thread won seat E6", confirmed.get() == 1);
        check("the other 7 failed cleanly", rejected.get() == racers - 1);
        check("inventory records exactly 1 booked seat on SHOW3",
                service.inventoryOf("SHOW3").countBooked() == 1);

        // ── Scenario 12: idempotent pay ──────────────────────
        section("12. Double-clicked 'Pay' is idempotent");
        BookingResult hankStart = service.startBooking("hank", "SHOW2", Arrays.asList("E1"));
        service.payAndConfirm(hankStart.booking.getId(), new AlwaysSucceedsPaymentProvider());
        BookingResult replay = service.payAndConfirm(hankStart.booking.getId(), new AlwaysSucceedsPaymentProvider());
        report("second payAndConfirm(hank)", replay);
        check("replay is a no-op, not a second charge",
                replay.success && replay.booking.getStatus() == BookingStatus.CONFIRMED);

        section("Final seat map — SHOW1");
        service.printSeatMap("SHOW1");

        summary();
    }

    // ── tiny console helpers ─────────────────────────────────
    private static void banner(String title) {
        System.out.println("\n============================================================");
        System.out.println("  " + title);
        System.out.println("============================================================");
    }

    private static void section(String title) {
        System.out.println("\n--- " + title + " " + "-".repeat(Math.max(0, 56 - title.length())));
    }

    private static void report(String action, BookingResult result) {
        String tag = result.success ? "OK  " : "FAIL";
        System.out.println("      [" + tag + "] " + action + " -> " + result.message);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            checksPassed++;
            System.out.println("      PASS  " + description);
        } else {
            checksFailed++;
            System.out.println("      **FAIL**  " + description);
        }
    }

    private static void summary() {
        System.out.println("\n============================================================");
        System.out.println("  CHECKS PASSED: " + checksPassed + "   FAILED: " + checksFailed);
        System.out.println("============================================================");
    }
}
