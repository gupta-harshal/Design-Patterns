import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// =====================================================================
// 0. MONEY — integer cents everywhere. Never accumulate doubles.
//    0.1 + 0.2 != 0.3 in binary floating point, and a balance sheet that
//    drifts by a cent per expense is a broken balance sheet.
// =====================================================================
final class Money {

    private Money() {
    }

    /** Convert a human-entered amount (e.g. 22.50) into cents (2250). */
    static long fromUnits(double units) {
        return Math.round(units * 100.0);
    }

    static String format(long cents) {
        long abs = Math.abs(cents);
        String text = String.format("$%d.%02d", abs / 100, abs % 100);
        return cents < 0 ? "-" + text : text;
    }
}

// =====================================================================
// 1. ALLOCATOR — the only place rounding happens.
//    Largest-remainder method: floor every share, then hand the leftover
//    cents to the participants with the biggest fractional part.
//    GUARANTEE: sum(result) == total, exactly, for any weights.
// =====================================================================
final class Allocator {

    private Allocator() {
    }

    static long[] largestRemainder(long total, double[] weights) {
        int n = weights.length;
        double weightSum = 0.0;
        for (int i = 0; i < n; i++) {
            if (weights[i] < 0) {
                throw new IllegalArgumentException("Split weight cannot be negative");
            }
            weightSum += weights[i];
        }
        if (weightSum <= 0) {
            throw new IllegalArgumentException("Split weights must sum to a positive value");
        }

        long[] shares = new long[n];
        final double[] fractions = new double[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            double exact = total * weights[i] / weightSum;
            shares[i] = (long) Math.floor(exact);
            fractions[i] = exact - shares[i];
            allocated += shares[i];
        }

        // Each floor() loses strictly less than one cent, so 0 <= leftover < n.
        long leftover = total - allocated;

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int byFraction = Double.compare(fractions[b], fractions[a]);
                // Tie-break on index so the output is deterministic and testable.
                return byFraction != 0 ? byFraction : Integer.compare(a, b);
            }
        });

        for (int k = 0; k < leftover; k++) {
            shares[order[k]]++;
        }
        return shares;
    }
}

// =====================================================================
// 2. USER
// =====================================================================
class User {
    private final String id;
    private final String name;

    User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        return id.equals(((User) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}

// =====================================================================
// 3. SPLIT HIERARCHY — the *input* the user gives per participant.
//    Each subclass answers one question: "what is my relative weight?"
//    The resolved cent amount is written back by the strategy.
// =====================================================================
enum SplitType {
    EQUAL,
    EXACT,
    PERCENT
}

abstract class Split {
    private final User user;
    private long amountCents;

    protected Split(User user) {
        if (user == null) {
            throw new IllegalArgumentException("A split must belong to a user");
        }
        this.user = user;
    }

    User getUser() {
        return user;
    }

    long getAmountCents() {
        return amountCents;
    }

    void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    abstract SplitType getType();

    /** Relative weight fed to the allocator. */
    abstract double getWeight();
}

class EqualSplit extends Split {

    EqualSplit(User user) {
        super(user);
    }

    @Override
    SplitType getType() {
        return SplitType.EQUAL;
    }

    @Override
    double getWeight() {
        return 1.0;
    }
}

class ExactSplit extends Split {
    private final long exactCents;

    ExactSplit(User user, double amount) {
        super(user);
        this.exactCents = Money.fromUnits(amount);
    }

    long getExactCents() {
        return exactCents;
    }

    @Override
    SplitType getType() {
        return SplitType.EXACT;
    }

    @Override
    double getWeight() {
        return exactCents;
    }
}

class PercentSplit extends Split {
    private final double percent;

    PercentSplit(User user, double percent) {
        super(user);
        this.percent = percent;
    }

    double getPercent() {
        return percent;
    }

    @Override
    SplitType getType() {
        return SplitType.PERCENT;
    }

    @Override
    double getWeight() {
        return percent;
    }
}

// =====================================================================
// 4. SPLIT STRATEGY — validate the input, then resolve cent amounts.
//    Adding "split by shares" later = one new class, zero edits elsewhere.
// =====================================================================
interface SplitStrategy {

    void validate(long totalCents, List<Split> splits);

    void apply(long totalCents, List<Split> splits);
}

abstract class WeightedSplitStrategy implements SplitStrategy {

    @Override
    public void apply(long totalCents, List<Split> splits) {
        double[] weights = new double[splits.size()];
        for (int i = 0; i < splits.size(); i++) {
            weights[i] = splits.get(i).getWeight();
        }
        long[] shares = Allocator.largestRemainder(totalCents, weights);
        for (int i = 0; i < splits.size(); i++) {
            splits.get(i).setAmountCents(shares[i]);
        }
    }

    protected void requireType(List<Split> splits, SplitType expected) {
        for (Split split : splits) {
            if (split.getType() != expected) {
                throw new IllegalArgumentException(
                        "Expected " + expected + " splits but found " + split.getType());
            }
        }
    }
}

class EqualSplitStrategy extends WeightedSplitStrategy {

    @Override
    public void validate(long totalCents, List<Split> splits) {
        requireType(splits, SplitType.EQUAL);
    }
}

class PercentSplitStrategy extends WeightedSplitStrategy {

    private static final double EPSILON = 1e-6;

    @Override
    public void validate(long totalCents, List<Split> splits) {
        requireType(splits, SplitType.PERCENT);
        double sum = 0.0;
        for (Split split : splits) {
            double percent = ((PercentSplit) split).getPercent();
            if (percent < 0) {
                throw new IllegalArgumentException("Percentage cannot be negative");
            }
            sum += percent;
        }
        if (Math.abs(sum - 100.0) > EPSILON) {
            throw new IllegalArgumentException("Percentages must sum to 100 but summed to " + sum);
        }
    }
}

class ExactSplitStrategy implements SplitStrategy {

    @Override
    public void validate(long totalCents, List<Split> splits) {
        long sum = 0;
        for (Split split : splits) {
            if (split.getType() != SplitType.EXACT) {
                throw new IllegalArgumentException(
                        "Expected EXACT splits but found " + split.getType());
            }
            long exact = ((ExactSplit) split).getExactCents();
            if (exact < 0) {
                throw new IllegalArgumentException("Exact share cannot be negative");
            }
            sum += exact;
        }
        if (sum != totalCents) {
            throw new IllegalArgumentException("Exact shares sum to " + Money.format(sum)
                    + " but the expense is " + Money.format(totalCents));
        }
    }

    @Override
    public void apply(long totalCents, List<Split> splits) {
        // No rounding needed: the user already gave us exact cents.
        for (Split split : splits) {
            split.setAmountCents(((ExactSplit) split).getExactCents());
        }
    }
}

// =====================================================================
// 5. EXPENSE
// =====================================================================
class Expense {
    private final String id;
    private final String description;
    private final long amountCents;
    private final User paidBy;
    private final SplitType splitType;
    private final List<Split> splits;

    Expense(String id, String description, long amountCents, User paidBy,
            SplitType splitType, List<Split> splits) {
        this.id = id;
        this.description = description;
        this.amountCents = amountCents;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.splits = new ArrayList<Split>(splits);
    }

    String getId() {
        return id;
    }

    String getDescription() {
        return description;
    }

    long getAmountCents() {
        return amountCents;
    }

    User getPaidBy() {
        return paidBy;
    }

    SplitType getSplitType() {
        return splitType;
    }

    List<Split> getSplits() {
        return Collections.unmodifiableList(splits);
    }

    void printReceipt() {
        System.out.println("  [" + id + "] " + description + " — " + Money.format(amountCents)
                + " paid by " + paidBy + " (" + splitType + ")");
        for (Split split : splits) {
            System.out.println("        " + split.getUser() + " owes share "
                    + Money.format(split.getAmountCents()));
        }
    }
}

// =====================================================================
// 6. BALANCE SHEET
//    Convention (write it on the whiteboard before you code):
//        owes[X][Y] = cents that X owes Y
//    The map is kept ANTISYMMETRIC: owes[X][Y] == -owes[Y][X].
//    So a pair debt is a single number, and "who owes whom" is just its sign.
// =====================================================================
class BalanceSheet {

    private final Map<User, Map<User, Long>> owes = new LinkedHashMap<User, Map<User, Long>>();

    /** Records that {@code debtor} now owes {@code creditor} an extra {@code cents}. */
    void record(User debtor, User creditor, long cents) {
        if (cents == 0 || debtor.equals(creditor)) {
            return; // Paying for your own share moves no money.
        }
        bump(debtor, creditor, cents);
        bump(creditor, debtor, -cents);
    }

    private void bump(User from, User to, long delta) {
        Map<User, Long> row = owes.get(from);
        if (row == null) {
            row = new LinkedHashMap<User, Long>();
            owes.put(from, row);
        }
        long current = row.containsKey(to) ? row.get(to).longValue() : 0L;
        row.put(to, current + delta);
    }

    /** Positive => debtor owes creditor. Negative => creditor owes debtor. */
    long amountOwed(User debtor, User creditor) {
        Map<User, Long> row = owes.get(debtor);
        if (row == null || !row.containsKey(creditor)) {
            return 0L;
        }
        return row.get(creditor).longValue();
    }

    /** Positive => the world owes this user. Negative => this user owes the world. */
    long netBalance(User user) {
        Map<User, Long> row = owes.get(user);
        if (row == null) {
            return 0L;
        }
        long net = 0L;
        for (Long value : row.values()) {
            net -= value.longValue();
        }
        return net;
    }

    List<User> participants() {
        return new ArrayList<User>(owes.keySet());
    }

    void showBalances(String title) {
        System.out.println(title);
        boolean any = false;
        for (Map.Entry<User, Map<User, Long>> row : owes.entrySet()) {
            for (Map.Entry<User, Long> cell : row.getValue().entrySet()) {
                if (cell.getValue().longValue() > 0) {
                    System.out.println("  " + row.getKey() + " owes " + cell.getKey()
                            + " " + Money.format(cell.getValue().longValue()));
                    any = true;
                }
            }
        }
        if (!any) {
            System.out.println("  (everyone is settled up)");
        }
    }

    void showNetBalances(String title) {
        System.out.println(title);
        for (User user : owes.keySet()) {
            long net = netBalance(user);
            String verdict = net > 0 ? "gets back" : (net < 0 ? "owes" : "is settled");
            System.out.println("  " + user + " " + verdict
                    + (net == 0 ? "" : " " + Money.format(Math.abs(net))));
        }
    }
}

// =====================================================================
// 7. DEBT SIMPLIFICATION (min cash flow, greedy)
//    Collapse the pairwise graph into net balances, then repeatedly match
//    the largest debtor with the largest creditor.
//    Produces at most n-1 transfers. Not always the true minimum — finding
//    that is NP-hard (it needs subset-sum style grouping).
// =====================================================================
class Transfer {
    final User from;
    final User to;
    final long cents;

    Transfer(User from, User to, long cents) {
        this.from = from;
        this.to = to;
        this.cents = cents;
    }

    @Override
    public String toString() {
        return from + " pays " + to + " " + Money.format(cents);
    }
}

final class DebtSimplifier {

    private DebtSimplifier() {
    }

    private static class Node {
        final User user;
        final long amount;

        Node(User user, long amount) {
            this.user = user;
            this.amount = amount;
        }
    }

    private static final Comparator<Node> LARGEST_FIRST = new Comparator<Node>() {
        @Override
        public int compare(Node a, Node b) {
            int byAmount = Long.compare(b.amount, a.amount);
            return byAmount != 0 ? byAmount : a.user.getId().compareTo(b.user.getId());
        }
    };

    static List<Transfer> simplify(BalanceSheet sheet) {
        PriorityQueue<Node> creditors = new PriorityQueue<Node>(16, LARGEST_FIRST);
        PriorityQueue<Node> debtors = new PriorityQueue<Node>(16, LARGEST_FIRST);

        for (User user : sheet.participants()) {
            long net = sheet.netBalance(user);
            if (net > 0) {
                creditors.add(new Node(user, net));
            } else if (net < 0) {
                debtors.add(new Node(user, -net));
            }
        }

        List<Transfer> transfers = new ArrayList<Transfer>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Node creditor = creditors.poll();
            Node debtor = debtors.poll();
            long settled = Math.min(creditor.amount, debtor.amount);
            transfers.add(new Transfer(debtor.user, creditor.user, settled));
            if (creditor.amount > settled) {
                creditors.add(new Node(creditor.user, creditor.amount - settled));
            }
            if (debtor.amount > settled) {
                debtors.add(new Node(debtor.user, debtor.amount - settled));
            }
        }
        return transfers;
    }
}

// =====================================================================
// 8. GROUP
// =====================================================================
class Group {
    private final String id;
    private final String name;
    private final List<User> members = new ArrayList<User>();
    private final List<Expense> expenses = new ArrayList<Expense>();
    private final BalanceSheet sheet = new BalanceSheet();

    Group(String id, String name, List<User> members) {
        this.id = id;
        this.name = name;
        this.members.addAll(members);
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    boolean hasMember(User user) {
        return members.contains(user);
    }

    BalanceSheet getSheet() {
        return sheet;
    }

    void addExpense(Expense expense) {
        expenses.add(expense);
    }

    List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }
}

// =====================================================================
// 9. SERVICE — the façade the client talks to
// =====================================================================
class SplitwiseService {

    private final Map<String, User> users = new LinkedHashMap<String, User>();
    private final Map<String, Group> groups = new LinkedHashMap<String, Group>();
    private final Map<SplitType, SplitStrategy> strategies = new HashMap<SplitType, SplitStrategy>();
    private final BalanceSheet globalSheet = new BalanceSheet();
    private int expenseSequence = 0;

    SplitwiseService() {
        strategies.put(SplitType.EQUAL, new EqualSplitStrategy());
        strategies.put(SplitType.EXACT, new ExactSplitStrategy());
        strategies.put(SplitType.PERCENT, new PercentSplitStrategy());
    }

    User addUser(String id, String name) {
        User user = new User(id, name);
        users.put(id, user);
        return user;
    }

    Group createGroup(String id, String name, List<User> members) {
        Group group = new Group(id, name, members);
        groups.put(id, group);
        return group;
    }

    BalanceSheet getGlobalSheet() {
        return globalSheet;
    }

    Expense addExpense(String description, double amount, User paidBy,
                       SplitType type, List<Split> splits) {
        return addExpense(description, amount, paidBy, type, splits, null);
    }

    Expense addExpense(String description, double amount, User paidBy,
                       SplitType type, List<Split> splits, Group group) {
        long totalCents = Money.fromUnits(amount);
        if (totalCents <= 0) {
            throw new IllegalArgumentException("Expense amount must be positive");
        }
        if (splits == null || splits.isEmpty()) {
            throw new IllegalArgumentException("An expense needs at least one split");
        }
        if (group != null) {
            if (!group.hasMember(paidBy)) {
                throw new IllegalArgumentException(paidBy + " is not a member of " + group.getName());
            }
            for (Split split : splits) {
                if (!group.hasMember(split.getUser())) {
                    throw new IllegalArgumentException(
                            split.getUser() + " is not a member of " + group.getName());
                }
            }
        }

        SplitStrategy strategy = strategies.get(type);
        strategy.validate(totalCents, splits);
        strategy.apply(totalCents, splits);

        // Defensive check: the resolved shares must reconstruct the total exactly.
        long sum = 0;
        for (Split split : splits) {
            sum += split.getAmountCents();
        }
        if (sum != totalCents) {
            throw new IllegalStateException("Split shares " + Money.format(sum)
                    + " != expense total " + Money.format(totalCents));
        }

        Expense expense = new Expense("e" + (++expenseSequence), description,
                totalCents, paidBy, type, splits);

        post(globalSheet, expense);
        if (group != null) {
            group.addExpense(expense);
            post(group.getSheet(), expense);
        }
        return expense;
    }

    /** Everyone in the split owes the payer their share (the payer's own share cancels). */
    private void post(BalanceSheet sheet, Expense expense) {
        for (Split split : expense.getSplits()) {
            sheet.record(split.getUser(), expense.getPaidBy(), split.getAmountCents());
        }
    }
}

// =====================================================================
// 10. CLIENT — every printed number is asserted in the comments below.
// =====================================================================
public class Main {

    public static void main(String[] args) {
        SplitwiseService app = new SplitwiseService();

        User alice = app.addUser("u1", "Alice");
        User bob = app.addUser("u2", "Bob");
        User charlie = app.addUser("u3", "Charlie");
        User dave = app.addUser("u4", "Dave");

        Group goa = app.createGroup("g1", "Goa Trip", Arrays.asList(alice, bob, charlie));

        System.out.println("=== Expenses ===");

        // --- Expense 1: Alice pays $300.00, EQUAL across Alice/Bob/Charlie ---
        // Share = 10000 cents each. Alice's own share cancels.
        // => Bob owes Alice $100.00, Charlie owes Alice $100.00
        Expense hotel = app.addExpense("Hotel", 300.00, alice, SplitType.EQUAL,
                equalSplits(alice, bob, charlie), goa);
        hotel.printReceipt();

        // --- Expense 2: Bob pays $150.00, EXACT 50 / 40 / 60 ---
        // => Alice owes Bob $50.00, Charlie owes Bob $60.00
        List<Split> dinnerSplits = new ArrayList<Split>();
        dinnerSplits.add(new ExactSplit(alice, 50.00));
        dinnerSplits.add(new ExactSplit(bob, 40.00));
        dinnerSplits.add(new ExactSplit(charlie, 60.00));
        Expense dinner = app.addExpense("Dinner", 150.00, bob, SplitType.EXACT, dinnerSplits, goa);
        dinner.printReceipt();

        // --- Expense 3: Charlie pays $200.00, PERCENT 50 / 25 / 25 ---
        // => Alice owes Charlie $100.00, Bob owes Charlie $50.00
        List<Split> cabSplits = new ArrayList<Split>();
        cabSplits.add(new PercentSplit(alice, 50.0));
        cabSplits.add(new PercentSplit(bob, 25.0));
        cabSplits.add(new PercentSplit(charlie, 25.0));
        Expense cab = app.addExpense("Cab", 200.00, charlie, SplitType.PERCENT, cabSplits, goa);
        cab.printReceipt();

        System.out.println();

        // -----------------------------------------------------------------
        // GROUP BALANCES — expected, by hand:
        //   Alice/Bob     : Bob owes Alice 100 (e1), Alice owes Bob 50 (e2)
        //                   => net Bob owes Alice $50.00
        //   Alice/Charlie : Charlie owes Alice 100 (e1), Alice owes Charlie 100 (e3)
        //                   => net $0.00  (fully cancels)
        //   Bob/Charlie   : Charlie owes Bob 60 (e2), Bob owes Charlie 50 (e3)
        //                   => net Charlie owes Bob $10.00
        // -----------------------------------------------------------------
        goa.getSheet().showBalances("=== Goa Trip balances ===");
        expect("Bob owes Alice", 5000, goa.getSheet().amountOwed(bob, alice));
        expect("Alice owes Charlie (netted)", 0, goa.getSheet().amountOwed(alice, charlie));
        expect("Charlie owes Bob", 1000, goa.getSheet().amountOwed(charlie, bob));

        System.out.println();

        // --- Expense 4 (no group): Dave pays $90.00, EQUAL across all four ---
        // 9000 cents / 4 = 2250 each => Alice, Bob, Charlie each owe Dave $22.50
        System.out.println("=== Non-group expense ===");
        Expense concert = app.addExpense("Concert tickets", 90.00, dave, SplitType.EQUAL,
                equalSplits(dave, alice, bob, charlie));
        concert.printReceipt();

        System.out.println();

        // -----------------------------------------------------------------
        // GLOBAL BALANCES — expected pairwise debts:
        //   Bob     -> Alice   $50.00
        //   Bob     -> Dave    $22.50
        //   Alice   -> Dave    $22.50
        //   Charlie -> Bob     $10.00
        //   Charlie -> Dave    $22.50           (5 edges)
        //
        // Net positions (must sum to zero):
        //   Alice   = +50.00 - 22.50 = +$27.50
        //   Bob     = -50.00 + 10.00 - 22.50 = -$62.50
        //   Charlie = -10.00 - 22.50 = -$32.50
        //   Dave    = +22.50 * 3     = +$67.50
        //   27.50 - 62.50 - 32.50 + 67.50 = 0  OK
        // -----------------------------------------------------------------
        BalanceSheet global = app.getGlobalSheet();
        global.showBalances("=== Global balances (raw pairwise) ===");
        System.out.println();
        global.showNetBalances("=== Global net positions ===");

        expect("Alice net", 2750, global.netBalance(alice));
        expect("Bob net", -6250, global.netBalance(bob));
        expect("Charlie net", -3250, global.netBalance(charlie));
        expect("Dave net", 6750, global.netBalance(dave));

        long zeroSum = global.netBalance(alice) + global.netBalance(bob)
                + global.netBalance(charlie) + global.netBalance(dave);
        expect("net positions sum to zero", 0, zeroSum);

        System.out.println();

        // -----------------------------------------------------------------
        // SIMPLIFIED SETTLEMENT — greedy max-debtor / max-creditor matching.
        // Expected 3 transfers instead of 5 raw debts:
        //   Bob     pays Dave  $62.50
        //   Charlie pays Alice $27.50
        //   Charlie pays Dave  $5.00
        // -----------------------------------------------------------------
        System.out.println("=== Simplified settlement (5 debts -> 3 transfers) ===");
        List<Transfer> transfers = DebtSimplifier.simplify(global);
        for (Transfer transfer : transfers) {
            System.out.println("  " + transfer);
        }
        expectCount("transfer count", 3, transfers.size());

        System.out.println();
        roundingDemo();
        System.out.println();
        validationDemo();
    }

    /** $100.00 across 3 people is not divisible — show that no cent is lost. */
    private static void roundingDemo() {
        System.out.println("=== Rounding demo: $100.00 equally across 3 ===");
        SplitwiseService app = new SplitwiseService();
        User frank = app.addUser("u1", "Frank");
        User grace = app.addUser("u2", "Grace");
        User heidi = app.addUser("u3", "Heidi");

        Expense lunch = app.addExpense("Lunch", 100.00, frank, SplitType.EQUAL,
                equalSplits(frank, grace, heidi));
        lunch.printReceipt();
        // Shares: 33.34 / 33.33 / 33.33 — the leftover cent goes to the first
        // participant by the tie-break rule. Sum is exactly $100.00.
        app.getGlobalSheet().showBalances("  balances:");
        expect("Grace owes Frank", 3333, app.getGlobalSheet().amountOwed(grace, frank));
        expect("Heidi owes Frank", 3333, app.getGlobalSheet().amountOwed(heidi, frank));
        expect("Frank is up by his own share", 6666, app.getGlobalSheet().netBalance(frank));
    }

    private static void validationDemo() {
        System.out.println("=== Validation ===");
        SplitwiseService app = new SplitwiseService();
        User ivan = app.addUser("u1", "Ivan");
        User judy = app.addUser("u2", "Judy");

        List<Split> badPercent = new ArrayList<Split>();
        badPercent.add(new PercentSplit(ivan, 60.0));
        badPercent.add(new PercentSplit(judy, 30.0));
        tryExpense(app, "Percent summing to 90", 100.00, ivan, SplitType.PERCENT, badPercent);

        List<Split> badExact = new ArrayList<Split>();
        badExact.add(new ExactSplit(ivan, 60.00));
        badExact.add(new ExactSplit(judy, 30.00));
        tryExpense(app, "Exact summing to 90", 100.00, ivan, SplitType.EXACT, badExact);

        List<Split> mixed = new ArrayList<Split>();
        mixed.add(new EqualSplit(ivan));
        mixed.add(new PercentSplit(judy, 50.0));
        tryExpense(app, "Mixed split types", 100.00, ivan, SplitType.EQUAL, mixed);
    }

    private static void tryExpense(SplitwiseService app, String label, double amount,
                                   User paidBy, SplitType type, List<Split> splits) {
        try {
            app.addExpense(label, amount, paidBy, type, splits);
            System.out.println("  " + label + " -> UNEXPECTEDLY ACCEPTED");
        } catch (IllegalArgumentException e) {
            System.out.println("  " + label + " -> rejected: " + e.getMessage());
        }
    }

    private static List<Split> equalSplits(User... users) {
        List<Split> splits = new ArrayList<Split>();
        for (User user : users) {
            splits.add(new EqualSplit(user));
        }
        return splits;
    }

    /** Tiny assertion helper so the demo self-verifies instead of just printing. */
    private static void expect(String label, long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("CHECK FAILED [" + label + "] expected "
                    + Money.format(expected) + " but was " + Money.format(actual));
        }
        System.out.println("  check OK: " + label + " = " + Money.format(actual));
    }

    private static void expectCount(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("CHECK FAILED [" + label + "] expected "
                    + expected + " but was " + actual);
        }
        System.out.println("  check OK: " + label + " = " + actual);
    }
}
