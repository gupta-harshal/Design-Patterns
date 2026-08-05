import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/*
 * ============================================================================
 *  TIC TAC TOE — Low-Level Design sketch (single file, javac-friendly)
 * ============================================================================
 *  Works for any board size N >= 1. Win = N identical symbols in a row,
 *  column, main diagonal or anti-diagonal.
 *
 *  Sections:
 *    1. PieceType / PlayingPiece  — what sits on a cell
 *    2. Move                      — immutable (row, col) value object
 *    3. Board                     — grid storage, validation, win/draw checks
 *    4. MoveStrategy (+ impls)    — Human / Random bot / Heuristic bot
 *    5. Player                    — identity + piece + strategy
 *    6. GameStatus / Game         — turn orchestration and lifecycle
 *    7. Main                      — runnable demos
 *
 *  Run:  javac Main.java && java Main
 *        java Main --human       (play against the heuristic bot)
 * ============================================================================
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. PIECE IDENTITY
// ─────────────────────────────────────────────────────────────────────────────

enum PieceType {
    X,
    O;

    /** Only meaningful for the classic 2-player game. */
    PieceType opponent() {
        return this == X ? O : X;
    }
}

/**
 * Thin wrapper around {@link PieceType}. Kept as a class so metadata (colour,
 * avatar, score weight) can be attached later without touching Board or Player.
 */
class PlayingPiece {
    private final PieceType pieceType;

    PlayingPiece(PieceType pieceType) {
        this.pieceType = pieceType;
    }

    PieceType getPieceType() {
        return pieceType;
    }

    @Override
    public String toString() {
        return pieceType.name();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. MOVE — immutable value object
// ─────────────────────────────────────────────────────────────────────────────

final class Move {
    final int row;
    final int col;

    Move(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. BOARD — owns the grid and every rule about the grid
// ─────────────────────────────────────────────────────────────────────────────

class Board {
    private final int size;
    private final PlayingPiece[][] grid;
    private int filledCells; // maintained incrementally so isFull() is O(1)

    Board(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Board size must be >= 1, got " + size);
        }
        this.size = size;
        this.grid = new PlayingPiece[size][size];
        this.filledCells = 0;
    }

    int getSize() {
        return size;
    }

    boolean isInside(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    boolean isEmpty(int row, int col) {
        return isInside(row, col) && grid[row][col] == null;
    }

    PlayingPiece pieceAt(int row, int col) {
        return isInside(row, col) ? grid[row][col] : null;
    }

    /** @return false when the cell is out of bounds or already taken. */
    boolean addPiece(int row, int col, PlayingPiece piece) {
        if (!isInside(row, col) || grid[row][col] != null) {
            return false;
        }
        grid[row][col] = piece;
        filledCells++;
        return true;
    }

    /** Used by bots to simulate a move and roll it back. */
    void removePiece(int row, int col) {
        if (isInside(row, col) && grid[row][col] != null) {
            grid[row][col] = null;
            filledCells--;
        }
    }

    boolean isFull() {
        return filledCells == size * size;
    }

    List<Move> emptyCells() {
        List<Move> cells = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == null) {
                    cells.add(new Move(i, j));
                }
            }
        }
        return cells;
    }

    // ── Win detection ────────────────────────────────────────────────────────

    private boolean matches(int row, int col, PieceType target) {
        PlayingPiece p = grid[row][col];
        return p != null && p.getPieceType() == target;
    }

    /**
     * O(N) incremental check. Only the row, column and (when relevant) the two
     * diagonals through the freshly played cell can possibly have completed.
     * This is the check the Game loop actually uses.
     */
    boolean hasWinnerAt(int row, int col) {
        PlayingPiece placed = pieceAt(row, col);
        if (placed == null) {
            return false;
        }
        PieceType target = placed.getPieceType();

        boolean win = true;
        for (int j = 0; j < size && win; j++) {
            win = matches(row, j, target);
        }
        if (win) {
            return true;
        }

        win = true;
        for (int i = 0; i < size && win; i++) {
            win = matches(i, col, target);
        }
        if (win) {
            return true;
        }

        if (row == col) {
            win = true;
            for (int i = 0; i < size && win; i++) {
                win = matches(i, i, target);
            }
            if (win) {
                return true;
            }
        }

        if (row + col == size - 1) {
            win = true;
            for (int i = 0; i < size && win; i++) {
                win = matches(i, size - 1 - i, target);
            }
            if (win) {
                return true;
            }
        }

        return false;
    }

    /**
     * O(N^2) full scan. Kept because it is the version most people write first
     * in an interview and it is handy for validating an arbitrary position.
     */
    boolean hasWinner(PlayingPiece piece) {
        PieceType target = piece.getPieceType();

        for (int i = 0; i < size; i++) {
            boolean rowWin = true;
            boolean colWin = true;
            for (int j = 0; j < size; j++) {
                rowWin &= matches(i, j, target);
                colWin &= matches(j, i, target);
            }
            if (rowWin || colWin) {
                return true;
            }
        }

        boolean mainDiag = true;
        boolean antiDiag = true;
        for (int i = 0; i < size; i++) {
            mainDiag &= matches(i, i, target);
            antiDiag &= matches(i, size - 1 - i, target);
        }
        return mainDiag || antiDiag;
    }

    // ── Rendering (correct separators for any N) ─────────────────────────────

    void printBoard() {
        StringBuilder separator = new StringBuilder("   ");
        for (int j = 0; j < size; j++) {
            separator.append("+---");
        }
        separator.append('+');

        StringBuilder header = new StringBuilder("   ");
        for (int j = 0; j < size; j++) {
            header.append("  ").append(j % 10).append(' ');
        }

        System.out.println(header);
        System.out.println(separator);
        for (int i = 0; i < size; i++) {
            StringBuilder line = new StringBuilder();
            line.append(' ').append(i % 10).append(' ');
            for (int j = 0; j < size; j++) {
                String cell = grid[i][j] == null ? " " : grid[i][j].toString();
                line.append("| ").append(cell).append(' ');
            }
            line.append('|');
            System.out.println(line);
            System.out.println(separator);
        }
        System.out.println();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. MOVE STRATEGY — the Strategy pattern hook that makes Human/Bot pluggable
// ─────────────────────────────────────────────────────────────────────────────

interface MoveStrategy {
    /** @return the chosen cell, or null to resign / give up on this turn. */
    Move nextMove(Board board, PlayingPiece piece);

    String describe();
}

/** Reads "row col" from stdin. */
class HumanMoveStrategy implements MoveStrategy {
    private final Scanner scanner;

    HumanMoveStrategy(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public Move nextMove(Board board, PlayingPiece piece) {
        System.out.print("Enter row col: ");
        if (!scanner.hasNextInt()) {
            if (scanner.hasNext()) {
                scanner.next(); // drop the junk token
            }
            System.out.println("Expected two integers, e.g. 0 1");
            return null;
        }
        int row = scanner.nextInt();
        if (!scanner.hasNextInt()) {
            System.out.println("Expected two integers, e.g. 0 1");
            return null;
        }
        return new Move(row, scanner.nextInt());
    }

    @Override
    public String describe() {
        return "human";
    }
}

/** Picks a uniformly random empty cell. */
class RandomBotStrategy implements MoveStrategy {
    private final Random random;

    RandomBotStrategy(Random random) {
        this.random = random;
    }

    @Override
    public Move nextMove(Board board, PlayingPiece piece) {
        List<Move> options = board.emptyCells();
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }

    @Override
    public String describe() {
        return "random bot";
    }
}

/**
 * Classic one-ply heuristic: win if you can, otherwise block, otherwise prefer
 * centre then corners then anything. Assumes the classic two-symbol game so it
 * can derive the opponent's piece from its own.
 *
 * This is deliberately NOT minimax — the point is that Game never changes when
 * the brain does.
 */
class HeuristicBotStrategy implements MoveStrategy {
    private final Random random;

    HeuristicBotStrategy(Random random) {
        this.random = random;
    }

    @Override
    public Move nextMove(Board board, PlayingPiece piece) {
        List<Move> options = board.emptyCells();
        if (options.isEmpty()) {
            return null;
        }

        Move winning = findImmediateWin(board, options, piece.getPieceType());
        if (winning != null) {
            return winning;
        }

        Move blocking = findImmediateWin(board, options, piece.getPieceType().opponent());
        if (blocking != null) {
            return blocking;
        }

        int n = board.getSize();
        if (n % 2 == 1 && board.isEmpty(n / 2, n / 2)) {
            return new Move(n / 2, n / 2);
        }

        List<Move> corners = new ArrayList<>();
        for (int r : new int[] { 0, n - 1 }) {
            for (int c : new int[] { 0, n - 1 }) {
                if (board.isEmpty(r, c)) {
                    corners.add(new Move(r, c));
                }
            }
        }
        if (!corners.isEmpty()) {
            return corners.get(random.nextInt(corners.size()));
        }

        return options.get(random.nextInt(options.size()));
    }

    /** Simulate each empty cell for {@code type} and roll the move back. */
    private Move findImmediateWin(Board board, List<Move> options, PieceType type) {
        PlayingPiece probe = new PlayingPiece(type);
        for (Move move : options) {
            if (!board.addPiece(move.row, move.col, probe)) {
                continue;
            }
            boolean wins = board.hasWinnerAt(move.row, move.col);
            board.removePiece(move.row, move.col);
            if (wins) {
                return move;
            }
        }
        return null;
    }

    @Override
    public String describe() {
        return "heuristic bot";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. PLAYER
// ─────────────────────────────────────────────────────────────────────────────

class Player {
    private final String name;
    private final PlayingPiece piece;
    private final MoveStrategy strategy;

    Player(String name, PlayingPiece piece, MoveStrategy strategy) {
        this.name = name;
        this.piece = piece;
        this.strategy = strategy;
    }

    String getName() {
        return name;
    }

    PlayingPiece getPiece() {
        return piece;
    }

    MoveStrategy getStrategy() {
        return strategy;
    }

    @Override
    public String toString() {
        return name + " (" + piece + ", " + strategy.describe() + ")";
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. GAME LIFECYCLE + ORCHESTRATION
// ─────────────────────────────────────────────────────────────────────────────

enum GameStatus {
    IN_PROGRESS,
    WIN,
    DRAW,
    ABANDONED
}

class Game {
    /** Guards against a buggy or stubborn strategy looping forever. */
    private static final int MAX_INVALID_ATTEMPTS_PER_TURN = 5;

    private final Board board;
    private final Deque<Player> turnOrder = new ArrayDeque<>();
    private final boolean verbose;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Player winner;

    Game(int boardSize, List<Player> players, boolean verbose) {
        if (players == null || players.size() < 2) {
            throw new IllegalArgumentException("Need at least two players");
        }
        this.board = new Board(boardSize);
        this.turnOrder.addAll(players);
        this.verbose = verbose;
    }

    GameStatus getStatus() {
        return status;
    }

    Player getWinner() {
        return winner;
    }

    GameStatus start() {
        if (verbose) {
            System.out.println("Players: " + turnOrder);
            board.printBoard();
        }

        while (status == GameStatus.IN_PROGRESS) {
            Player current = turnOrder.peekFirst();
            Move move = requestValidMove(current);

            if (move == null) {
                status = GameStatus.ABANDONED;
                System.out.println(current.getName() + " could not produce a legal move. Game abandoned.");
                break;
            }

            board.addPiece(move.row, move.col, current.getPiece());
            if (verbose) {
                System.out.println(current.getName() + " plays " + move);
                board.printBoard();
            }

            if (board.hasWinnerAt(move.row, move.col)) {
                status = GameStatus.WIN;
                winner = current;
            } else if (board.isFull()) {
                status = GameStatus.DRAW;
            } else {
                turnOrder.addLast(turnOrder.removeFirst()); // rotate turn
            }
        }

        announce();
        return status;
    }

    /** Re-prompts on illegal input; returns null once the budget is exhausted. */
    private Move requestValidMove(Player player) {
        for (int attempt = 0; attempt < MAX_INVALID_ATTEMPTS_PER_TURN; attempt++) {
            Move move = player.getStrategy().nextMove(board, player.getPiece());
            if (move != null && board.isEmpty(move.row, move.col)) {
                return move;
            }
            if (move != null) {
                System.out.println("Illegal move " + move + " — cell is occupied or off-board. Try again.");
            }
        }
        return null;
    }

    private void announce() {
        switch (status) {
            case WIN:
                System.out.println(">>> Winner: " + winner.getName() + " with " + winner.getPiece());
                break;
            case DRAW:
                System.out.println(">>> Draw — board is full with no winner.");
                break;
            case ABANDONED:
                System.out.println(">>> Game abandoned.");
                break;
            default:
                break;
        }
        System.out.println();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. CLIENT
// ─────────────────────────────────────────────────────────────────────────────

public class Main {

    public static void main(String[] args) {
        boolean human = args.length > 0 && "--human".equals(args[0]);
        if (human) {
            playAgainstBot();
            return;
        }

        demoHeuristicVsRandom();
        demoLargerBoard();
        demoForcedDraw();
    }

    /** 3x3, seeded so the output is reproducible. */
    private static void demoHeuristicVsRandom() {
        System.out.println("=== Demo 1: 3x3 — heuristic bot (X) vs random bot (O) ===\n");
        Random random = new Random(7);
        List<Player> players = new ArrayList<>();
        players.add(new Player("Ada", new PlayingPiece(PieceType.X), new HeuristicBotStrategy(random)));
        players.add(new Player("Bob", new PlayingPiece(PieceType.O), new RandomBotStrategy(random)));
        new Game(3, players, true).start();
    }

    /** Proves the N x N generalisation: rendering, win check and draw all scale. */
    private static void demoLargerBoard() {
        System.out.println("=== Demo 2: 5x5 — same code, bigger board ===\n");
        Random random = new Random(42);
        List<Player> players = new ArrayList<>();
        players.add(new Player("Ada", new PlayingPiece(PieceType.X), new HeuristicBotStrategy(random)));
        players.add(new Player("Bob", new PlayingPiece(PieceType.O), new HeuristicBotStrategy(random)));
        new Game(5, players, true).start();
    }

    /**
     * Scripted 3x3 game that ends in a draw — a cheap regression test for the
     * "board full and nobody won" branch.
     */
    private static void demoForcedDraw() {
        System.out.println("=== Demo 3: scripted 3x3 draw ===\n");
        // X O X
        // X O O
        // O X X
        List<Move> xMoves = new ArrayList<>();
        Collections.addAll(xMoves, new Move(0, 0), new Move(0, 2), new Move(1, 0), new Move(2, 1), new Move(2, 2));
        List<Move> oMoves = new ArrayList<>();
        Collections.addAll(oMoves, new Move(0, 1), new Move(1, 1), new Move(1, 2), new Move(2, 0));

        List<Player> players = new ArrayList<>();
        players.add(new Player("Script-X", new PlayingPiece(PieceType.X), new ScriptedStrategy(xMoves)));
        players.add(new Player("Script-O", new PlayingPiece(PieceType.O), new ScriptedStrategy(oMoves)));
        new Game(3, players, true).start();
    }

    private static void playAgainstBot() {
        Scanner scanner = new Scanner(System.in);
        List<Player> players = new ArrayList<>();
        players.add(new Player("You", new PlayingPiece(PieceType.X), new HumanMoveStrategy(scanner)));
        players.add(new Player("Bot", new PlayingPiece(PieceType.O), new HeuristicBotStrategy(new Random())));
        new Game(3, players, true).start();
        scanner.close();
    }
}

/** Replays a fixed list of moves — useful for deterministic tests/demos. */
class ScriptedStrategy implements MoveStrategy {
    private final List<Move> moves;
    private int index;

    ScriptedStrategy(List<Move> moves) {
        this.moves = moves;
    }

    @Override
    public Move nextMove(Board board, PlayingPiece piece) {
        return index < moves.size() ? moves.get(index++) : null;
    }

    @Override
    public String describe() {
        return "scripted";
    }
}
