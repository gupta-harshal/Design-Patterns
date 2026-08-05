import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

// 1. PIECE IDENTITY
enum PieceType {
    X,
    O
}

// 2. PLAYING PIECE (thin wrapper — room for future metadata)
class PlayingPiece {
    private final PieceType pieceType;

    public PlayingPiece(PieceType pieceType) {
        this.pieceType = pieceType;
    }

    public PieceType getPieceType() {
        return pieceType;
    }

    @Override
    public String toString() {
        return pieceType.name();
    }
}

// 3. BOARD — grid storage, validation, win/draw checks
class Board {
    private final int size;
    private final PlayingPiece[][] grid;

    public Board(int size) {
        this.size = size;
        this.grid = new PlayingPiece[size][size];
    }

    public int getSize() {
        return size;
    }

    public boolean addPiece(int row, int col, PlayingPiece piece) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            return false;
        }
        if (grid[row][col] != null) {
            return false;
        }
        grid[row][col] = piece;
        return true;
    }

    public boolean isFull() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasWinner(PlayingPiece piece) {
        PieceType target = piece.getPieceType();

        // Rows
        for (int i = 0; i < size; i++) {
            boolean rowWin = true;
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == null || grid[i][j].getPieceType() != target) {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin) {
                return true;
            }
        }

        // Columns
        for (int j = 0; j < size; j++) {
            boolean colWin = true;
            for (int i = 0; i < size; i++) {
                if (grid[i][j] == null || grid[i][j].getPieceType() != target) {
                    colWin = false;
                    break;
                }
            }
            if (colWin) {
                return true;
            }
        }

        // Main diagonal
        boolean mainDiag = true;
        for (int i = 0; i < size; i++) {
            if (grid[i][i] == null || grid[i][i].getPieceType() != target) {
                mainDiag = false;
                break;
            }
        }
        if (mainDiag) {
            return true;
        }

        // Anti diagonal
        boolean antiDiag = true;
        for (int i = 0; i < size; i++) {
            if (grid[i][size - 1 - i] == null || grid[i][size - 1 - i].getPieceType() != target) {
                antiDiag = false;
                break;
            }
        }
        return antiDiag;
    }

    public void printBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                String cell = grid[i][j] == null ? " " : grid[i][j].toString();
                System.out.print(" " + cell + " ");
                if (j < size - 1) {
                    System.out.print("|");
                }
            }
            System.out.println();
            if (i < size - 1) {
                System.out.println("---+---+---");
            }
        }
        System.out.println();
    }
}

// 4. PLAYER
class Player {
    private final String name;
    private final PlayingPiece piece;

    public Player(String name, PlayingPiece piece) {
        this.name = name;
        this.piece = piece;
    }

    public String getName() {
        return name;
    }

    public PlayingPiece getPiece() {
        return piece;
    }
}

// 5. GAME LIFECYCLE
enum GameStatus {
    IN_PROGRESS,
    WIN,
    DRAW
}

// 6. GAME ORCHESTRATOR
class Game {
    private final Board board;
    private final Deque<Player> players;
    private GameStatus status;
    private Player winner;

    public Game(int boardSize, Player player1, Player player2) {
        this.board = new Board(boardSize);
        this.players = new ArrayDeque<>();
        this.players.addLast(player1);
        this.players.addLast(player2);
        this.status = GameStatus.IN_PROGRESS;
        this.winner = null;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Tic Tac Toe ===");
        board.printBoard();

        while (status == GameStatus.IN_PROGRESS) {
            Player current = players.peekFirst();
            System.out.print(current.getName() + " (" + current.getPiece() + ") — enter row col: ");

            if (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Invalid input. Use two integers, e.g. 0 1");
                continue;
            }
            int row = scanner.nextInt();
            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Use two integers, e.g. 0 1");
                continue;
            }
            int col = scanner.nextInt();

            boolean placed = board.addPiece(row, col, current.getPiece());
            if (!placed) {
                System.out.println("Invalid move. Cell occupied or out of bounds. Try again.");
                continue;
            }

            board.printBoard();

            if (board.hasWinner(current.getPiece())) {
                status = GameStatus.WIN;
                winner = current;
                break;
            }

            if (board.isFull()) {
                status = GameStatus.DRAW;
                break;
            }

            // Rotate turn: current player goes to the back
            players.addLast(players.removeFirst());
        }

        if (status == GameStatus.WIN) {
            System.out.println("Winner: " + winner.getName() + " (" + winner.getPiece() + ")");
        } else if (status == GameStatus.DRAW) {
            System.out.println("Game ended in a draw.");
        }
    }
}

// 7. CLIENT
public class Main {
    public static void main(String[] args) {
        Player alice = new Player("Alice", new PlayingPiece(PieceType.X));
        Player bob = new Player("Bob", new PlayingPiece(PieceType.O));

        Game game = new Game(3, alice, bob);
        game.start();
    }
}
