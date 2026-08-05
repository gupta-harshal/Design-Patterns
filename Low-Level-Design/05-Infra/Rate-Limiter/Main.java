import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// =====================================================================
// 1. CLOCK — injected so the demo is deterministic and testable.
//    Never call System.currentTimeMillis() directly inside a limiter:
//    you could not unit-test "what happens after 3 seconds".
// =====================================================================
interface Clock {
    long nowMillis();
}

class SystemClock implements Clock {
    @Override
    public long nowMillis() {
        return System.currentTimeMillis();
    }
}

class ManualClock implements Clock {
    private long now;

    public ManualClock(long start) {
        this.now = start;
    }

    public void advanceTo(long millis) {
        if (millis < now) {
            throw new IllegalArgumentException("clock cannot go backwards");
        }
        this.now = millis;
    }

    public void advanceBy(long millis) {
        this.now += millis;
    }

    @Override
    public long nowMillis() {
        return now;
    }
}

// =====================================================================
// 2. COMMON CONTRACT
// =====================================================================
interface RateLimiter {
    boolean allowRequest(String clientId);

    String name();
}

// =====================================================================
// 3. TOKEN BUCKET (primary implementation)
//    Bucket holds up to `capacity` tokens and refills at `refillPerSecond`.
//    A request costs 1 token; no token => reject.
//    Allows bursts up to `capacity`, then settles at the refill rate.
// =====================================================================
class TokenBucket {
    // Tokens accumulate through repeated floating-point addition, so an exact
    // ">= 1" boundary can land on 0.9999999999999998. Compare with a tolerance
    // instead of trusting binary doubles at the boundary.
    private static final double EPSILON = 1e-9;

    private final double capacity;
    private final double refillPerSecond;
    private final Clock clock;

    private double availableTokens;    // double: fractional tokens must NOT be lost
    private long lastRefillTimestamp;

    public TokenBucket(double capacity, double refillPerSecond, Clock clock) {
        if (capacity <= 0 || refillPerSecond <= 0) {
            throw new IllegalArgumentException("capacity and refill rate must be > 0");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.availableTokens = capacity;             // start full: first burst is allowed
        this.lastRefillTimestamp = clock.nowMillis();
    }

    // Lazy refill: no background thread. Tokens are computed on demand from
    // elapsed time, which is O(1) and costs nothing while the client is idle.
    private void refill() {
        long now = clock.nowMillis();
        if (now <= lastRefillTimestamp) {
            return; // clock did not move (or went backwards): keep state as is
        }
        double elapsedSeconds = (now - lastRefillTimestamp) / 1000.0;
        double refilled = elapsedSeconds * refillPerSecond;
        availableTokens = Math.min(capacity, availableTokens + refilled);
        lastRefillTimestamp = now;
    }

    public synchronized boolean tryConsume(double tokens) {
        refill();
        if (availableTokens + EPSILON >= tokens) {
            availableTokens = Math.max(0.0, availableTokens - tokens);
            return true;
        }
        return false;
    }

    public synchronized boolean tryConsume() {
        return tryConsume(1);
    }

    /** Milliseconds until `tokens` are available — feeds the Retry-After header. */
    public synchronized long millisUntilAvailable(double tokens) {
        refill();
        if (availableTokens + EPSILON >= tokens) {
            return 0;
        }
        double deficit = tokens - availableTokens;
        return (long) Math.ceil(deficit / refillPerSecond * 1000.0);
    }

    public synchronized double getAvailableTokens() {
        refill();
        return availableTokens;
    }
}

class TokenBucketRateLimiter implements RateLimiter {
    private final double capacity;
    private final double refillPerSecond;
    private final Clock clock;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(double capacity, double refillPerSecond, Clock clock) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
    }

    private TokenBucket bucketFor(String clientId) {
        return buckets.computeIfAbsent(clientId,
                id -> new TokenBucket(capacity, refillPerSecond, clock));
    }

    @Override
    public boolean allowRequest(String clientId) {
        return bucketFor(clientId).tryConsume();
    }

    public double tokensLeft(String clientId) {
        return bucketFor(clientId).getAvailableTokens();
    }

    public long retryAfterMillis(String clientId) {
        return bucketFor(clientId).millisUntilAvailable(1);
    }

    @Override
    public String name() {
        return "TokenBucket(cap=" + capacity + ", refill=" + refillPerSecond + "/s)";
    }
}

// =====================================================================
// 4. THE CLASSIC BUG — integer token math silently starves the bucket.
//    Kept runnable so the failure is visible, not just described.
// =====================================================================
class NaiveIntegerTokenBucket {
    private final int capacity;
    private final int refillPerSecond;
    private final Clock clock;
    private int availableTokens;
    private long lastRefillTimestamp;

    public NaiveIntegerTokenBucket(int capacity, int refillPerSecond, Clock clock) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.availableTokens = capacity;
        this.lastRefillTimestamp = clock.nowMillis();
    }

    public boolean tryConsume() {
        long now = clock.nowMillis();
        // BUG: integer division truncates the earned fraction to 0, yet the
        // timestamp still advances -> that elapsed time is lost forever.
        int refilled = (int) ((now - lastRefillTimestamp) / 1000) * refillPerSecond;
        availableTokens = Math.min(capacity, availableTokens + refilled);
        lastRefillTimestamp = now;
        if (availableTokens >= 1) {
            availableTokens--;
            return true;
        }
        return false;
    }
}

// =====================================================================
// 5. FIXED WINDOW COUNTER — cheapest, but allows a 2x burst at the seam.
// =====================================================================
class FixedWindowRateLimiter implements RateLimiter {
    private static class Window {
        long windowId = -1;
        int count = 0;
    }

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, long windowMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        Window w = windows.computeIfAbsent(clientId, id -> new Window());
        long currentWindowId = clock.nowMillis() / windowMillis;
        if (w.windowId != currentWindowId) {
            w.windowId = currentWindowId; // new window: counter resets hard
            w.count = 0;
        }
        if (w.count < maxRequests) {
            w.count++;
            return true;
        }
        return false;
    }

    @Override
    public String name() {
        return "FixedWindow(" + maxRequests + " per " + windowMillis + "ms)";
    }
}

// =====================================================================
// 6. SLIDING WINDOW LOG — exact, but stores one timestamp per request.
// =====================================================================
class SlidingWindowLogRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> log = new ConcurrentHashMap<>();

    public SlidingWindowLogRateLimiter(int maxRequests, long windowMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        long now = clock.nowMillis();
        Deque<Long> timestamps = log.computeIfAbsent(clientId, id -> new ArrayDeque<>());

        // Drop everything that fell out of the trailing window.
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowMillis) {
            timestamps.pollFirst();
        }
        if (timestamps.size() < maxRequests) {
            timestamps.addLast(now); // only successful requests are logged
            return true;
        }
        return false;
    }

    @Override
    public String name() {
        return "SlidingWindowLog(" + maxRequests + " per " + windowMillis + "ms)";
    }
}

// =====================================================================
// 7. SLIDING WINDOW COUNTER — the practical compromise: two counters,
//    previous window weighted by how much of it still overlaps.
// =====================================================================
class SlidingWindowCounterRateLimiter implements RateLimiter {
    private static class Counters {
        long windowId = Long.MIN_VALUE;
        int currentCount = 0;
        int previousCount = 0;
    }

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Counters> state = new ConcurrentHashMap<>();

    public SlidingWindowCounterRateLimiter(int maxRequests, long windowMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        long now = clock.nowMillis();
        long windowId = now / windowMillis;
        Counters c = state.computeIfAbsent(clientId, id -> new Counters());

        if (c.windowId != windowId) {
            c.previousCount = (windowId == c.windowId + 1) ? c.currentCount : 0;
            c.currentCount = 0;
            c.windowId = windowId;
        }

        long elapsedInWindow = now % windowMillis;
        double previousWeight = (windowMillis - elapsedInWindow) / (double) windowMillis;
        double estimated = c.previousCount * previousWeight + c.currentCount;

        if (estimated < maxRequests) {
            c.currentCount++;
            return true;
        }
        return false;
    }

    @Override
    public String name() {
        return "SlidingWindowCounter(" + maxRequests + " per " + windowMillis + "ms)";
    }
}

public class Main {
    private static ManualClock clock;

    public static void main(String[] args) {
        tokenBucketWalkthrough();
        integerBugDemo();
        perClientIsolation();
        algorithmComparison();
    }

    // ---------------------------------------------------------------
    private static void tokenBucketWalkthrough() {
        System.out.println("=== TOKEN BUCKET: capacity 5, refill 2 tokens/sec ===");
        clock = new ManualClock(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 2, clock);

        System.out.println("-- t=0ms: bucket starts full, fire 6 requests --");
        for (int i = 1; i <= 6; i++) {
            fire(limiter, "alice");
        }

        System.out.println("-- t=500ms: +0.5s * 2/s = +1.0 token --");
        clock.advanceTo(500);
        fire(limiter, "alice");
        fire(limiter, "alice");

        System.out.println("-- t=1200ms: +0.7s * 2/s = +1.4 tokens (only 1 whole token usable) --");
        clock.advanceTo(1200);
        fire(limiter, "alice");
        fire(limiter, "alice");

        System.out.println("-- t=3000ms: +1.8s * 2/s = +3.6 tokens, added to the leftover 0.4 --");
        clock.advanceTo(3000);
        for (int i = 1; i <= 5; i++) {
            fire(limiter, "alice");
        }

        System.out.println("-- t=10000ms: idle 7s would earn 14 tokens, but capacity caps it at 5 --");
        clock.advanceTo(10000);
        System.out.printf("   tokens available = %.2f (capped)%n", limiter.tokensLeft("alice"));
        for (int i = 1; i <= 6; i++) {
            fire(limiter, "alice");
        }
    }

    private static void fire(TokenBucketRateLimiter limiter, String client) {
        long t = clock.nowMillis();
        boolean allowed = limiter.allowRequest(client);
        System.out.printf("   t=%-6d %-5s -> %-7s tokens=%.2f%s%n",
                t, client, allowed ? "ALLOW" : "DENY",
                limiter.tokensLeft(client),
                allowed ? "" : "  (retry after " + limiter.retryAfterMillis(client) + "ms)");
    }

    // ---------------------------------------------------------------
    private static void integerBugDemo() {
        System.out.println();
        System.out.println("=== WHY TOKENS MUST BE FRACTIONAL ===");
        System.out.println("Both buckets: capacity 1, refill 2/sec, polled every 100ms for 2s.");

        ManualClock c1 = new ManualClock(0);
        TokenBucket correct = new TokenBucket(1, 2, c1);
        ManualClock c2 = new ManualClock(0);
        NaiveIntegerTokenBucket buggy = new NaiveIntegerTokenBucket(1, 2, c2);

        int correctAllowed = 0;
        int buggyAllowed = 0;
        for (int i = 0; i < 20; i++) {
            c1.advanceBy(100);
            c2.advanceBy(100);
            if (correct.tryConsume()) {
                correctAllowed++;
            }
            if (buggy.tryConsume()) {
                buggyAllowed++;
            }
        }
        System.out.println("   double math  -> " + correctAllowed + " allowed (expected ~4: 1 initial + 2/s * 2s)");
        System.out.println("   integer math -> " + buggyAllowed + " allowed  <-- starved: every poll truncates to 0 tokens");
    }

    // ---------------------------------------------------------------
    private static void perClientIsolation() {
        System.out.println();
        System.out.println("=== PER-CLIENT ISOLATION (one bucket per key) ===");
        clock = new ManualClock(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1, clock);
        fire(limiter, "alice");
        fire(limiter, "alice");
        fire(limiter, "alice"); // alice exhausted
        fire(limiter, "bob");   // bob is untouched
        fire(limiter, "bob");
        fire(limiter, "bob");
    }

    // ---------------------------------------------------------------
    private static void algorithmComparison() {
        System.out.println();
        System.out.println("=== BOUNDARY BURST: 5 requests per 1000ms window ===");
        System.out.println("Traffic: 5 requests at t=900ms, then 5 more at t=1000ms.");
        System.out.println("A correct limiter must not admit 10 requests inside one 1000ms span.");

        long[] times = {900, 900, 900, 900, 900, 1000, 1000, 1000, 1000, 1000};

        for (int variant = 0; variant < 3; variant++) {
            ManualClock c = new ManualClock(0);
            RateLimiter limiter;
            if (variant == 0) {
                limiter = new FixedWindowRateLimiter(5, 1000, c);
            } else if (variant == 1) {
                limiter = new SlidingWindowLogRateLimiter(5, 1000, c);
            } else {
                limiter = new SlidingWindowCounterRateLimiter(5, 1000, c);
            }

            StringBuilder trace = new StringBuilder();
            int allowed = 0;
            for (long t : times) {
                c.advanceTo(t);
                boolean ok = limiter.allowRequest("alice");
                if (ok) {
                    allowed++;
                }
                trace.append(ok ? "A" : ".");
            }
            System.out.printf("   %-46s %s  allowed=%d%n", limiter.name(), trace, allowed);
        }
        System.out.println("   legend: A=allowed  .=denied   (first 5 at t=900, last 5 at t=1000)");
        System.out.println("   FixedWindow admits 10 in 100ms — the seam problem the other two fix.");
    }
}
