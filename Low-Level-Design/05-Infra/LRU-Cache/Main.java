import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// =====================================================================
// 1. NODE — element of the doubly linked list.
//    It stores the KEY as well, because eviction starts from the list
//    (tail) and must be able to delete the matching map entry in O(1).
// =====================================================================
class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    Node(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

// Optional hook: real caches need this for write-back / metrics.
interface EvictionListener<K, V> {
    void onEvict(K key, V value);
}

// =====================================================================
// 2. LRU CACHE — HashMap (O(1) lookup) + Doubly Linked List (O(1) reorder)
//    Invariant: head.next = most recently used, tail.prev = least recently used.
//    Sentinel head/tail nodes remove every null check from the rewiring code.
// =====================================================================
class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head; // sentinel: MRU side
    private final Node<K, V> tail; // sentinel: LRU side
    private EvictionListener<K, V> evictionListener;

    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0, got " + capacity);
        }
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        this.head.next = tail;
        this.tail.prev = head;
    }

    public void setEvictionListener(EvictionListener<K, V> listener) {
        this.evictionListener = listener;
    }

    // ---------------- list primitives (the part that gets fumbled) ----------------

    // Unlink a node from wherever it currently sits.
    private void unlink(Node<K, V> node) {
        node.prev.next = node.next; // sentinels guarantee prev/next are never null
        node.next.prev = node.prev;
        node.prev = null;           // avoid stale references (helps GC, catches bugs)
        node.next = null;
    }

    // Insert right after head, i.e. mark as most recently used.
    // ORDER MATTERS: read head.next BEFORE overwriting it.
    private void addFirst(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        addFirst(node);
    }

    // ---------------- public API ----------------

    /** O(1). Returns null on miss; a hit refreshes recency. */
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            misses++;
            return null;
        }
        hits++;
        moveToFront(node); // a read counts as a use
        return node.value;
    }

    /** O(1). Inserts or updates; either way the key becomes most recently used. */
    public void put(K key, V value) {
        Node<K, V> existing = map.get(key);
        if (existing != null) {
            existing.value = value;   // update in place: no new node, no eviction
            moveToFront(existing);
            return;
        }
        if (map.size() == capacity) { // evict BEFORE inserting, never after
            evictLeastRecentlyUsed();
        }
        Node<K, V> node = new Node<>(key, value);
        map.put(key, node);
        addFirst(node);
    }

    private void evictLeastRecentlyUsed() {
        Node<K, V> lru = tail.prev;
        if (lru == head) {
            return; // empty list (only reachable if capacity were 0)
        }
        K key = lru.key;   // capture before unlink clears the links
        V value = lru.value;
        unlink(lru);
        map.remove(key);
        evictions++;
        if (evictionListener != null) {
            evictionListener.onEvict(key, value);
        }
    }

    /** O(1). Explicit removal — handy for invalidation. */
    public boolean remove(K key) {
        Node<K, V> node = map.remove(key);
        if (node == null) {
            return false;
        }
        unlink(node);
        return true;
    }

    public boolean containsKey(K key) {
        return map.containsKey(key); // deliberately does NOT touch recency
    }

    public int size() {
        return map.size();
    }

    public int capacity() {
        return capacity;
    }

    public String stats() {
        return "hits=" + hits + " misses=" + misses + " evictions=" + evictions;
    }

    /** Debug view, MRU first. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MRU [");
        for (Node<K, V> cur = head.next; cur != tail; cur = cur.next) {
            if (cur != head.next) {
                sb.append(", ");
            }
            sb.append(cur.key).append("=").append(cur.value);
        }
        return sb.append("] LRU").toString();
    }

    /** Verifies the two structures never drift apart. Used by the demo. */
    public void assertConsistent() {
        int forward = 0;
        for (Node<K, V> cur = head.next; cur != tail; cur = cur.next) {
            if (cur.next.prev != cur) {
                throw new IllegalStateException("broken back-link at key " + cur.key);
            }
            if (!map.containsKey(cur.key)) {
                throw new IllegalStateException("list node not in map: " + cur.key);
            }
            forward++;
        }
        int backward = 0;
        for (Node<K, V> cur = tail.prev; cur != head; cur = cur.prev) {
            backward++;
        }
        if (forward != backward || forward != map.size()) {
            throw new IllegalStateException(
                    "size drift: forward=" + forward + " backward=" + backward + " map=" + map.size());
        }
        if (map.size() > capacity) {
            throw new IllegalStateException("capacity exceeded: " + map.size());
        }
    }
}

// =====================================================================
// 3. ALTERNATIVE — LinkedHashMap already IS a hash map + linked list.
//    Great "I know the library" answer, but interviewers usually want
//    the hand-rolled version above.
// =====================================================================
class LinkedHashLruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LinkedHashLruCache(int capacity) {
        super(16, 0.75f, true); // accessOrder = true -> get() moves entry to the end
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}

public class Main {
    private static LRUCache<Integer, Integer> cache;

    public static void main(String[] args) {
        System.out.println("=== Classic trace: capacity = 2 ===");
        cache = new LRUCache<>(2);
        cache.setEvictionListener((k, v) -> System.out.println("      >> evicted " + k + "=" + v));

        put(1, 1);   // MRU [1=1] LRU
        put(2, 2);   // MRU [2=2, 1=1] LRU
        get(1);      // 1 -> refreshes 1: MRU [1=1, 2=2] LRU
        put(3, 3);   // full -> evict LRU (key 2): MRU [3=3, 1=1] LRU
        get(2);      // null  (key 2 was evicted)
        get(3);      // 3
        put(4, 4);   // full -> evict LRU (key 1): MRU [4=4, 3=3] LRU
        get(1);      // null
        get(3);      // 3
        get(4);      // 4
        System.out.println("stats: " + cache.stats());

        System.out.println();
        System.out.println("=== Update of an existing key must NOT evict ===");
        cache = new LRUCache<>(2);
        put(1, 1);
        put(2, 2);
        put(1, 100); // update + refresh; size stays 2, nothing evicted
        get(2);      // still present -> proves no eviction happened
        System.out.println("size=" + cache.size() + " (expected 2)");

        System.out.println();
        System.out.println("=== Capacity 1: every insert evicts ===");
        cache = new LRUCache<>(1);
        cache.setEvictionListener((k, v) -> System.out.println("      >> evicted " + k + "=" + v));
        put(1, 1);
        put(2, 2);
        get(1);      // null

        System.out.println();
        System.out.println("=== Explicit remove, then re-insert ===");
        cache = new LRUCache<>(3);
        put(1, 1);
        put(2, 2);
        put(3, 3);
        System.out.printf("remove(2) -> %s | %s%n", cache.remove(2), cache);
        put(4, 4);   // room exists now, so no eviction
        System.out.println("state    : " + cache + " size=" + cache.size());

        System.out.println();
        System.out.println("=== Stress: 20k ops, structure stays consistent ===");
        LRUCache<Integer, String> big = new LRUCache<>(100);
        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < 20000; i++) {
            int key = rnd.nextInt(300);
            if (rnd.nextBoolean()) {
                big.put(key, "v" + key);
            } else {
                big.get(key);
            }
            big.assertConsistent();
        }
        System.out.println("OK — size=" + big.size() + "/" + big.capacity() + "  " + big.stats());

        System.out.println();
        System.out.println("=== Same behaviour via LinkedHashMap(accessOrder=true) ===");
        LinkedHashLruCache<Integer, Integer> lib = new LinkedHashLruCache<>(2);
        lib.put(1, 1);
        lib.put(2, 2);
        lib.get(1);
        lib.put(3, 3);
        System.out.println("keys (LRU first) = " + lib.keySet() + "  -> key 2 gone: " + !lib.containsKey(2));
    }

    private static void put(int k, int v) {
        cache.put(k, v);
        cache.assertConsistent();
        System.out.printf("put(%d,%d) -> %s%n", k, v, cache);
    }

    private static void get(int k) {
        Integer value = cache.get(k);
        cache.assertConsistent();
        System.out.printf("get(%d)   -> %-4s  %s%n", k, String.valueOf(value), cache);
    }
}
