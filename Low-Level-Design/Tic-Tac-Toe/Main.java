import java.util.ArrayList;
import java.util.List;

// ============================================================
// 1. DOMAIN ENUMS
// ============================================================

enum Symbol {
    X,
    O,
    EMPTY
}

enum GameStatus {
    IN_PROGRESS,
    X_WON,
    O_WON,
    DRAW
}

// ============================================================
// 2. PLAYER
// ============================================================

class Player {
    private final String name;
    private final Symbol symbol;

    public Player(String name, Symbol symbol) {
        if (symbol == Symbol.EMPTY) {
            throw new IllegalArgumentException("Player cannot have EMPTY symbol.");
        }
        this.name = name;
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public Symbol getSymbol() {
        return symbol;
    }
}

// ============================================================
// 3. BOARD (Single Responsibility: board state only)
// ============================================================

class Board {
    private final int size;
    private final Symbol[][] grid;
    private int movesPlayed;

    public Board(int size) {
        if (size < 3) {
            throw new IllegalArgumentException("Board size must be at least 3.");
        }
        this.size = size;
        this.grid = new Symbol[size][size];
        this.movesPlayed = 0;
        reset();
    }

    public void reset() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = Symbol.EMPTY;
            }
        }
        movesPlayed = 0;
    }

    public boolean isValidMove(int row, int col) {
        return row >= 0 && row < size
                && col >= 0 && col < size
                && grid[row][col] == Symbol.EMPTY;
    }

    public void placeSymbol(int row, int col, Symbol symbol) {
        if (!isValidMove(row, col)) {
            throw new IllegalArgumentException(
                    "Invalid move at (" + row + ", " + col + "). Cell is occupied or out of bounds.");
        }
        grid[row][col] = symbol;
        movesPlayed++;
    }

    public Symbol getCell(int row, int col) {
        return grid[row][col];
    }

    public int getSize() {
        return size;
    }

    public int getMovesPlayed() {
        return movesPlayed;
    }

    public boolean isFull() {
        return movesPlayed == size * size;
    }

    public void display() {
        System.out.println();
        for (int i = 0; i < size; i++) {
            StringBuilder row = new StringBuilder(" ");
            for (int j = 0; j < size; j++) {
                char mark = grid[i][j] == Symbol.EMPTY ? '.' : grid[i][j].name().charAt(0);
                row.append(mark);
                if (j < size - 1) {
                    row.append(" | ");
                }
            }
            System.out.println(row);
            if (i < size - 1) {
                System.out.println("---+---+---");
            }
        }
        System.out.println();
    }
}

// ============================================================
// 4. WINNING STRATEGY (Strategy Pattern — Open/Closed)
// ============================================================

interface WinningStrategy {
    boolean hasWon(Board board, Symbol symbol);
}

/**
 * Checks rows, columns, and both diagonals for a complete line.
 * Works for any N x N board (classic Tic-Tac-Toe is N = 3).
 */
class DefaultWinningStrategy implements WinningStrategy {
    @Override
    public boolean hasWon(Board board, Symbol symbol) {
        int n = board.getSize();

        // Rows
        for (int i = 0; i < n; i++) {
            boolean rowWin = true;
            for (int j = 0; j < n; j++) {
                if (board.getCell(i, j) != symbol) {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin) {
                return true;
            }
        }

        // Columns
        for (int j = 0; j < n; j++) {
            boolean colWin = true;
            for (int i = 0; i < n; i++) {
                if (board.getCell(i, j) != symbol) {
                    colWin = false;
                    break;
                }
            }
            if (colWin) {
                return true;
            }
        }

        // Main diagonal
        boolean diagWin = true;
        for (int i = 0; i < n; i++) {
            if (board.getCell(i, i) != symbol) {
                diagWin = false;
                break;
            }
        }
        if (diagWin) {
            return true;
        }

        // Anti-diagonal
        boolean antiDiagWin = true;
        for (int i = 0; i < n; i++) {
            if (board.getCell(i, n - 1 - i) != symbol) {
                antiDiagWin = false;
                break;
            }
        }
        return antiDiagWin;
    }
}

// ============================================================
// 5. GAME (Orchestrator — turn management & status)
// ============================================================

class Game {
    private final Board board;
    private final List<Player> players;
    private final WinningStrategy winningStrategy;
    private int currentPlayerIndex;
    private GameStatus status;

    public Game(Player player1, Player player2, int boardSize, WinningStrategy winningStrategy) {
        if (player1.getSymbol() == player2.getSymbol()) {
            throw new IllegalArgumentException("Players must have different symbols.");
        }
        this.board = new Board(boardSize);
        this.players = new ArrayList<>();
        this.players.add(player1);
        this.players.add(player2);
        this.winningStrategy = winningStrategy;
        this.currentPlayerIndex = 0;
        this.status = GameStatus.IN_PROGRESS;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public GameStatus getStatus() {
        return status;
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Places the current player's symbol and updates game status.
     * Returns true if the move was accepted; false if the game is already over.
     */
    public boolean makeMove(int row, int col) {
        if (status != GameStatus.IN_PROGRESS) {
            System.out.println("Game is already over. Status: " + status);
            return false;
        }

        Player current = getCurrentPlayer();
        board.placeSymbol(row, col, current.getSymbol());

        if (winningStrategy.hasWon(board, current.getSymbol())) {
            status = current.getSymbol() == Symbol.X ? GameStatus.X_WON : GameStatus.O_WON;
            System.out.println(current.getName() + " (" + current.getSymbol() + ") wins!");
        } else if (board.isFull()) {
            status = GameStatus.DRAW;
            System.out.println("It's a draw!");
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }

        return true;
    }

    public void displayBoard() {
        board.display();
    }
}

// ============================================================
// 6. DEMO RUNNER
// ============================================================

public class Main {
    public static void main(String[] args) {
        Player alice = new Player("Alice", Symbol.X);
        Player bob = new Player("Bob", Symbol.O);
        WinningStrategy strategy = new DefaultWinningStrategy();

        Game game = new Game(alice, bob, 3, strategy);

        System.out.println("=== Tic-Tac-Toe Low Level Design Demo ===");
        System.out.println(alice.getName() + " is " + alice.getSymbol());
        System.out.println(bob.getName() + " is " + bob.getSymbol());
        System.out.println();

        // Scripted game: Alice wins on the main diagonal
        // Board layout after all moves:
        //   X | O | .
        //  ---+---+---
        //   O | X | .
        //  ---+---+---
        //   . | . | X
        int[][] moves = {
                {0, 0}, // Alice X
                {0, 1}, // Bob   O
                {1, 1}, // Alice X
                {1, 0}, // Bob   O
                {2, 2}  // Alice X — wins on diagonal
        };

        for (int[] move : moves) {
            Player current = game.getCurrentPlayer();
            System.out.println(current.getName() + " plays at (" + move[0] + ", " + move[1] + ")");
            game.makeMove(move[0], move[1]);
            game.displayBoard();

            if (game.getStatus() != GameStatus.IN_PROGRESS) {
                break;
            }
        }

        System.out.println("Final status: " + game.getStatus());
    }
}
