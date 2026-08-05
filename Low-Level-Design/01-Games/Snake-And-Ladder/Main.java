import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/*
 * ============================================================================
 *  SNAKE AND LADDER — Low-Level Design sketch (single file, javac-friendly)
 * ============================================================================
 *  Rules implemented (all configurable via GameRules):
 *    - Cells are numbered 1..N. Every player starts OFF the board at 0.
 *    - Snake: head > tail. Landing on the head slides you down to the tail.
 *    - Ladder: start < end. Landing on the start climbs you up to the end.
 *    - Exact finish (default ON): if position + roll > N you do not move.
 *    - Extra turn on max roll (default ON): rolling the highest face grants
 *      another roll; three in a row forfeits the turn (classic house rule).
 *    - First player to land exactly on N wins.
 *
 *  Sections:
 *    1. Dice (Strategy) + implementations
 *    2. Jump / Snake / Ladder value objects
 *    3. Board (validated at construction)
 *    4. Player
 *    5. GameRules
 *    6. Game (turn orchestration, jump resolution, safety caps)
 *    7. Main (runnable demos)
 *
 *  Run:  javac Main.java && java Main
 * ============================================================================
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. DICE — Strategy pattern: the game never knows how a number is produced
// ─────────────────────────────────────────────────────────────────────────────

interface Dice {
    /** @return the total pips rolled this throw (>= 1). */
    int roll();

    /** Highest value a single throw can produce — used for the "extra turn" rule. */
    int maxValue();

    String describe();
}

/** N fair dice with F faces each. Classic Snake & Ladder is 1 die, 6 faces. */
class FairDice implements Dice {
    private final int diceCount;
    private final int faces;
    private final Random random;

    FairDice(int diceCount, int faces, Random random) {
        if (diceCount < 1 || faces < 2) {
            throw new IllegalArgumentException("Need >=1 dice with >=2 faces");
        }
        this.diceCount = diceCount;
        this.faces = faces;
        this.random = random;
    }

    static FairDice standard(Random random) {
        return new FairDice(1, 6, random);
    }

    @Override
    public int roll() {
        int total = 0;
        for (int i = 0; i < diceCount; i++) {
            total += random.nextInt(faces) + 1;
        }
        return total;
    }

    @Override
    public int maxValue() {
        return diceCount * faces;
    }

    @Override
    public String describe() {
        return diceCount + "d" + faces + " (fair)";
    }
}

/**
 * Biased die: {@code weights[i]} is the relative chance of rolling {@code i+1}.
 * Handy for demonstrating that the engine is indifferent to the distribution,
 * and for testing "what if the player always rolls high".
 */
class LoadedDice implements Dice {
    private final int[] weights;
    private final int totalWeight;
    private final Random random;

    LoadedDice(int[] weights, Random random) {
        if (weights == null || weights.length < 2) {
            throw new IllegalArgumentException("Need at least 2 faces");
        }
        int sum = 0;
        for (int w : weights) {
            if (w < 0) {
                throw new IllegalArgumentException("Weights must be non-negative");
            }
            sum += w;
        }
        if (sum == 0) {
            throw new IllegalArgumentException("At least one face must have positive weight");
        }
        this.weights = weights.clone();
        this.totalWeight = sum;
        this.random = random;
    }

    @Override
    public int roll() {
        int pick = random.nextInt(totalWeight);
        for (int face = 0; face < weights.length; face++) {
            pick -= weights[face];
            if (pick < 0) {
                return face + 1;
            }
        }
        return weights.length; // unreachable while totalWeight is consistent
    }

    @Override
    public int maxValue() {
        return weights.length;
    }

    @Override
    public String describe() {
        return "loaded d" + weights.length;
    }
}

/** Replays a fixed sequence of rolls — makes demos and unit tests deterministic. */
class ScriptedDice implements Dice {
    private final int[] sequence;
    private final int max;
    private int index;

    ScriptedDice(int max, int... sequence) {
        if (sequence.length == 0) {
            throw new IllegalArgumentException("Sequence must not be empty");
        }
        this.max = max;
        this.sequence = sequence.clone();
    }

    @Override
    public int roll() {
        int value = sequence[index % sequence.length];
        index++;
        return value;
    }

    @Override
    public int maxValue() {
        return max;
    }

    @Override
    public String describe() {
        return "scripted";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. JUMPS — a snake and a ladder are the same shape, different direction
// ─────────────────────────────────────────────────────────────────────────────

enum JumpType {
    SNAKE,
    LADDER
}

final class Jump {
    final int start;
    final int end;
    final JumpType type;

    private Jump(int start, int end, JumpType type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    /** head > tail: landing on {@code head} sends you back to {@code tail}. */
    static Jump snake(int head, int tail) {
        if (head <= tail) {
            throw new IllegalArgumentException("Snake head (" + head + ") must be above its tail (" + tail + ")");
        }
        return new Jump(head, tail, JumpType.SNAKE);
    }

    /** start < end: landing on {@code start} lifts you to {@code end}. */
    static Jump ladder(int start, int end) {
        if (start >= end) {
            throw new IllegalArgumentException("Ladder start (" + start + ") must be below its end (" + end + ")");
        }
        return new Jump(start, end, JumpType.LADDER);
    }

    @Override
    public String toString() {
        return (type == JumpType.SNAKE ? "snake " : "ladder ") + start + "->" + end;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. BOARD — cells 1..N plus a start -> jump lookup, validated once up front
// ─────────────────────────────────────────────────────────────────────────────

class Board {
    private final int size;
    private final Map<Integer, Jump> jumpsByStart = new HashMap<>();

    Board(int size, List<Jump> jumps) {
        if (size < 2) {
            throw new IllegalArgumentException("Board needs at least 2 cells, got " + size);
        }
        this.size = size;
        for (Jump jump : jumps) {
            validate(jump);
            Jump clash = jumpsByStart.put(jump.start, jump);
            if (clash != null) {
                throw new IllegalArgumentException("Cell " + jump.start + " already starts " + clash);
            }
        }
    }

    /**
     * Board invariants. These are the checks interviewers probe for, because a
     * board that violates them produces an unwinnable or nonsensical game.
     */
    private void validate(Jump jump) {
        if (jump.start < 1 || jump.start > size || jump.end < 1 || jump.end > size) {
            throw new IllegalArgumentException(jump + " falls outside cells 1.." + size);
        }
        if (jump.start == size) {
            // A snake head on the last cell makes the game unwinnable; a ladder
            // start there is meaningless because landing on N already wins.
            throw new IllegalArgumentException(jump + " starts on the winning cell " + size);
        }
    }

    int getSize() {
        return size;
    }

    /** @return the jump starting at {@code cell}, or null. */
    Jump jumpAt(int cell) {
        return jumpsByStart.get(cell);
    }

    /** Sorted by starting cell so transcripts and diagrams read consistently. */
    Map<Integer, Jump> allJumps() {
        return new TreeMap<>(jumpsByStart);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. PLAYER
// ─────────────────────────────────────────────────────────────────────────────

class Player {
    private final String name;
    private int position; // 0 means "not on the board yet"

    Player(String name) {
        this.name = name;
        this.position = 0;
    }

    String getName() {
        return name;
    }

    int getPosition() {
        return position;
    }

    void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return name + "@" + position;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. RULES — every house-rule variation lives here, not scattered in the loop
// ─────────────────────────────────────────────────────────────────────────────

class GameRules {
    /** true: overshooting N leaves you where you are. false: you just land on N. */
    final boolean exactFinish;
    /** true: rolling the die's maximum face grants another roll. */
    final boolean extraTurnOnMaxRoll;
    /** How many consecutive max rolls before the turn is forfeited (0 = unlimited). */
    final int maxConsecutiveMaxRolls;
    /** true: a jump that lands on another jump's start chains. */
    final boolean chainJumps;
    /** Hard stop so a pathological board can never hang the process. */
    final int maxTurns;

    GameRules(boolean exactFinish, boolean extraTurnOnMaxRoll, int maxConsecutiveMaxRolls,
              boolean chainJumps, int maxTurns) {
        this.exactFinish = exactFinish;
        this.extraTurnOnMaxRoll = extraTurnOnMaxRoll;
        this.maxConsecutiveMaxRolls = maxConsecutiveMaxRolls;
        this.chainJumps = chainJumps;
        this.maxTurns = maxTurns;
    }

    static GameRules classic() {
        return new GameRules(true, true, 3, true, 2000);
    }

    static GameRules simple() {
        // No extra turns, overshoot is clamped to a win: the "textbook" variant.
        return new GameRules(false, false, 0, true, 2000);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. GAME — orchestration
// ─────────────────────────────────────────────────────────────────────────────

enum GameStatus {
    IN_PROGRESS,
    FINISHED,
    ABORTED_TURN_LIMIT
}

class Game {
    /**
     * Belt-and-braces cap on the extra-turn chain. Only reachable if a caller
     * configures unlimited consecutive max rolls; the classic rules stop at 3.
     */
    private static final int MAX_ROLLS_PER_TURN = 100;

    private final Board board;
    private final Dice dice;
    private final GameRules rules;
    private final Deque<Player> turnOrder = new ArrayDeque<>();
    private final boolean verbose;

    private GameStatus status = GameStatus.IN_PROGRESS;
    private Player winner;
    private int turnCount;

    Game(Board board, Dice dice, GameRules rules, List<Player> players, boolean verbose) {
        if (players == null || players.size() < 2) {
            throw new IllegalArgumentException("Need at least two players");
        }
        this.board = board;
        this.dice = dice;
        this.rules = rules;
        this.turnOrder.addAll(players);
        this.verbose = verbose;
    }

    GameStatus getStatus() {
        return status;
    }

    Player getWinner() {
        return winner;
    }

    int getTurnCount() {
        return turnCount;
    }

    GameStatus play() {
        if (verbose) {
            System.out.println("Board: 1.." + board.getSize() + " | dice: " + dice.describe()
                    + " | exactFinish=" + rules.exactFinish + " extraTurnOnMax=" + rules.extraTurnOnMaxRoll);
            System.out.println("Jumps: " + board.allJumps().values());
            System.out.println();
        }

        while (status == GameStatus.IN_PROGRESS) {
            if (turnCount >= rules.maxTurns) {
                status = GameStatus.ABORTED_TURN_LIMIT;
                break;
            }
            turnCount++;

            Player current = turnOrder.peekFirst();
            takeTurn(current);

            if (status == GameStatus.IN_PROGRESS) {
                turnOrder.addLast(turnOrder.removeFirst());
            }
        }

        announce();
        return status;
    }

    /** One turn = one or more rolls, depending on the extra-turn rule. */
    private void takeTurn(Player player) {
        int consecutiveMaxRolls = 0;

        for (int rollsThisTurn = 0; rollsThisTurn < MAX_ROLLS_PER_TURN; rollsThisTurn++) {
            int roll = dice.roll();
            boolean rolledMax = roll == dice.maxValue();

            if (rules.extraTurnOnMaxRoll && rules.maxConsecutiveMaxRolls > 0) {
                consecutiveMaxRolls = rolledMax ? consecutiveMaxRolls + 1 : 0;
                if (consecutiveMaxRolls == rules.maxConsecutiveMaxRolls) {
                    log(player.getName() + " rolls " + roll + " — " + rules.maxConsecutiveMaxRolls
                            + " in a row, turn forfeited (stays at " + player.getPosition() + ")");
                    return;
                }
            }

            int from = player.getPosition();
            int target = from + roll;

            if (target > board.getSize()) {
                if (rules.exactFinish) {
                    log(player.getName() + " rolls " + roll + " — overshoots " + board.getSize()
                            + ", stays at " + from);
                    if (!(rules.extraTurnOnMaxRoll && rolledMax)) {
                        return;
                    }
                    continue;
                }
                target = board.getSize(); // non-exact variant: clamp onto the last cell
            }

            int landed = resolveJumps(target, player);
            player.setPosition(landed);

            if (landed == board.getSize()) {
                status = GameStatus.FINISHED;
                winner = player;
                log(player.getName() + " rolls " + roll + ": " + from + " -> " + landed + "  *** WINS ***");
                return;
            }

            if (!(rules.extraTurnOnMaxRoll && rolledMax)) {
                return;
            }
            log("   " + player.getName() + " rolled the maximum — extra turn");
        }
    }

    /**
     * Applies snakes/ladders at the landing cell.
     *
     * With {@code chainJumps} a ladder that lands on a snake head keeps
     * resolving. A visited set makes an A->B->A board terminate instead of
     * looping forever; well-formed boards never trigger it.
     */
    private int resolveJumps(int cell, Player player) {
        if (!rules.chainJumps) {
            Jump jump = board.jumpAt(cell);
            if (jump == null) {
                return cell;
            }
            log("   " + player.getName() + " hits " + jump);
            return jump.end;
        }

        Set<Integer> visited = new HashSet<>();
        int current = cell;
        while (visited.add(current)) {
            Jump jump = board.jumpAt(current);
            if (jump == null) {
                return current;
            }
            log("   " + player.getName() + " hits " + jump);
            current = jump.end;
        }
        log("   cycle detected at cell " + current + " — jump chain stopped");
        return current;
    }

    private void log(String message) {
        if (verbose) {
            System.out.println("  " + message);
        }
    }

    private void announce() {
        if (!verbose) {
            return;
        }
        System.out.println();
        if (status == GameStatus.FINISHED) {
            System.out.println(">>> " + winner.getName() + " wins after " + turnCount + " turns.");
        } else {
            System.out.println(">>> Aborted: turn limit " + rules.maxTurns + " reached (no winner).");
        }
        System.out.println();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. CLIENT
// ─────────────────────────────────────────────────────────────────────────────

public class Main {

    public static void main(String[] args) {
        demoClassicAutoPlay();
        demoScriptedRules();
        demoLoadedDice();
        demoStatistics();
    }

    /** Standard 100-cell board, fair d6, seeded so the transcript is stable. */
    private static void demoClassicAutoPlay() {
        System.out.println("=== Demo 1: classic 10x10 board, fair d6, 3 players ===\n");
        Random random = new Random(2024);
        Game game = new Game(classicBoard(), FairDice.standard(random), GameRules.classic(),
                players("Alice", "Bob", "Charlie"), true);
        game.play();
    }

    /**
     * Deterministic transcript that exercises every rule branch:
     * ladder climb, snake bite, overshoot with exact-finish, exact win.
     */
    private static void demoScriptedRules() {
        System.out.println("=== Demo 2: scripted dice on a tiny 25-cell board ===\n");

        List<Jump> jumps = new ArrayList<>();
        jumps.add(Jump.ladder(3, 12));
        jumps.add(Jump.ladder(9, 21));
        jumps.add(Jump.snake(18, 6));
        Board board = new Board(25, jumps);

        // Rolls are consumed in order by whoever is on turn:
        //   A: 3  -> cell 3, ladder to 12
        //   B: 5  -> cell 5
        //   A: 6  -> cell 18, snake down to 6; a 6 also buys an extra roll
        //      3  -> cell 9, ladder up to 21
        //   B: 4  -> cell 9, ladder up to 21
        //   A: 6  -> 27 overshoots 25, stays at 21; extra roll for the 6
        //      4  -> exactly 25, Alice wins
        Dice dice = new ScriptedDice(6, 3, 5, 6, 3, 4, 6, 4);
        GameRules rules = new GameRules(true, true, 3, true, 200);

        new Game(board, dice, rules, players("Alice", "Bob"), true).play();
    }

    /** Same engine, a die that favours high faces — nothing else changes. */
    private static void demoLoadedDice() {
        System.out.println("=== Demo 3: loaded dice (6 is 5x likelier than 1) ===\n");
        Random random = new Random(11);
        Dice loaded = new LoadedDice(new int[] { 1, 1, 2, 2, 4, 5 }, random);
        new Game(classicBoard(), loaded, GameRules.classic(), players("Loaded-Lu", "Fair-Fay"), true).play();
    }

    /** Shows why the turn cap matters and gives a feel for game length. */
    private static void demoStatistics() {
        System.out.println("=== Demo 4: 10,000 silent games on the classic board ===\n");
        Random random = new Random(99);
        long totalTurns = 0;
        int aborted = 0;
        int shortest = Integer.MAX_VALUE;
        int longest = 0;
        Map<String, Integer> wins = new LinkedHashMap<>();
        wins.put("Alice", 0);
        wins.put("Bob", 0);

        for (int i = 0; i < 10_000; i++) {
            Game game = new Game(classicBoard(), FairDice.standard(random), GameRules.classic(),
                    players("Alice", "Bob"), false);
            GameStatus status = game.play();
            if (status != GameStatus.FINISHED) {
                aborted++;
                continue;
            }
            int turns = game.getTurnCount();
            totalTurns += turns;
            shortest = Math.min(shortest, turns);
            longest = Math.max(longest, turns);
            wins.merge(game.getWinner().getName(), 1, Integer::sum);
        }

        int finished = 10_000 - aborted;
        System.out.println("finished=" + finished + " aborted=" + aborted);
        System.out.println("avg turns=" + String.format("%.2f", totalTurns / (double) finished)
                + "  min=" + shortest + "  max=" + longest);
        System.out.println("wins by seat order (first mover advantage): " + wins);
        System.out.println();
    }

    /** The classic 1..100 board: 10 snakes, 9 ladders, none overlapping. */
    private static Board classicBoard() {
        List<Jump> jumps = new ArrayList<>();
        jumps.add(Jump.ladder(2, 38));
        jumps.add(Jump.ladder(7, 14));
        jumps.add(Jump.ladder(8, 31));
        jumps.add(Jump.ladder(15, 26));
        jumps.add(Jump.ladder(21, 42));
        jumps.add(Jump.ladder(28, 84));
        jumps.add(Jump.ladder(36, 44));
        jumps.add(Jump.ladder(51, 67));
        jumps.add(Jump.ladder(71, 91));
        jumps.add(Jump.ladder(78, 98));

        jumps.add(Jump.snake(16, 6));
        jumps.add(Jump.snake(46, 25));
        jumps.add(Jump.snake(49, 11));
        jumps.add(Jump.snake(62, 19));
        jumps.add(Jump.snake(64, 60));
        jumps.add(Jump.snake(74, 53));
        jumps.add(Jump.snake(89, 68));
        jumps.add(Jump.snake(92, 88));
        jumps.add(Jump.snake(95, 75));
        jumps.add(Jump.snake(99, 80));

        return new Board(100, jumps);
    }

    private static List<Player> players(String... names) {
        List<Player> list = new ArrayList<>();
        for (String name : names) {
            list.add(new Player(name));
        }
        return list;
    }
}
