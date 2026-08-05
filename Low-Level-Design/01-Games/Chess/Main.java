enum Color {
    WHITE, BLACK;

    Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

final class Pos {
    final int r;
    final int c;

    Pos(int r, int c) {
        this.r = r;
        this.c = c;
    }

    boolean onBoard() {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    @Override
    public String toString() {
        return "(" + r + "," + c + ")";
    }
}

abstract class Piece {
    final Color color;

    Piece(Color color) {
        this.color = color;
    }

    abstract String code();

    abstract boolean canMove(Board board, Pos from, Pos to);

    @Override
    public String toString() {
        return (color == Color.WHITE ? "W" : "B") + code();
    }
}

class Rook extends Piece {
    Rook(Color color) { super(color); }
    @Override String code() { return "R"; }

    @Override
    boolean canMove(Board board, Pos from, Pos to) {
        if (from.r != to.r && from.c != to.c) return false;
        return board.isPathClear(from, to);
    }
}

class Bishop extends Piece {
    Bishop(Color color) { super(color); }
    @Override String code() { return "B"; }

    @Override
    boolean canMove(Board board, Pos from, Pos to) {
        if (Math.abs(from.r - to.r) != Math.abs(from.c - to.c)) return false;
        return board.isPathClear(from, to);
    }
}

class Queen extends Piece {
    Queen(Color color) { super(color); }
    @Override String code() { return "Q"; }

    @Override
    boolean canMove(Board board, Pos from, Pos to) {
        boolean rookLike = from.r == to.r || from.c == to.c;
        boolean bishopLike = Math.abs(from.r - to.r) == Math.abs(from.c - to.c);
        if (!rookLike && !bishopLike) return false;
        return board.isPathClear(from, to);
    }
}

class Knight extends Piece {
    Knight(Color color) { super(color); }
    @Override String code() { return "N"; }

    @Override
    boolean canMove(Board board, Pos from, Pos to) {
        int dr = Math.abs(from.r - to.r);
        int dc = Math.abs(from.c - to.c);
        return dr * dc == 2; // 2×1
    }
}

class King extends Piece {
    King(Color color) { super(color); }
    @Override String code() { return "K"; }

    @Override
    boolean canMove(Board board, Pos from, Pos to) {
        return Math.max(Math.abs(from.r - to.r), Math.abs(from.c - to.c)) == 1;
    }
}

class Pawn extends Piece {
    Pawn(Color color) { super(color); }
    @Override String code() { return "P"; }

    @Override
    boolean canMove(Board board, Pos from, Pos to) {
        int dir = color == Color.WHITE ? -1 : 1; // row 7 white home in our array? we'll place white at bottom rows 6-7
        int dr = to.r - from.r;
        int dc = to.c - from.c;
        Piece dest = board.get(to);
        // forward
        if (dc == 0 && dest == null) {
            if (dr == dir) return true;
            int startRow = color == Color.WHITE ? 6 : 1;
            if (from.r == startRow && dr == 2 * dir && board.get(new Pos(from.r + dir, from.c)) == null) {
                return true;
            }
        }
        // diagonal capture
        if (Math.abs(dc) == 1 && dr == dir && dest != null && dest.color != color) {
            return true;
        }
        return false;
    }
}

class Board {
    private final Piece[][] cells = new Piece[8][8];

    Piece get(Pos p) {
        return cells[p.r][p.c];
    }

    void place(Pos p, Piece piece) {
        cells[p.r][p.c] = piece;
    }

    boolean isPathClear(Pos from, Pos to) {
        int dr = Integer.compare(to.r, from.r);
        int dc = Integer.compare(to.c, from.c);
        int r = from.r + dr;
        int c = from.c + dc;
        while (r != to.r || c != to.c) {
            if (cells[r][c] != null) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    boolean tryMove(Pos from, Pos to, Color turn) {
        if (!from.onBoard() || !to.onBoard()) {
            System.out.println("Off board");
            return false;
        }
        Piece piece = get(from);
        if (piece == null) {
            System.out.println("No piece at " + from);
            return false;
        }
        if (piece.color != turn) {
            System.out.println("Not " + turn + "'s piece");
            return false;
        }
        Piece dest = get(to);
        if (dest != null && dest.color == turn) {
            System.out.println("Cannot capture own piece");
            return false;
        }
        if (!piece.canMove(this, from, to)) {
            System.out.println("Illegal " + piece + " move " + from + " -> " + to);
            return false;
        }
        cells[to.r][to.c] = piece;
        cells[from.r][from.c] = null;
        System.out.println("OK: " + piece + " " + from + " -> " + to);
        return true;
    }

    void print() {
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + " ");
            for (int c = 0; c < 8; c++) {
                Piece p = cells[r][c];
                System.out.print(p == null ? ". " : p + " ");
            }
            System.out.println();
        }
        System.out.println("  a  b  c  d  e  f  g  h (cols as 0..7 in API)");
    }
}

class Game {
    private final Board board = new Board();
    private Color turn = Color.WHITE;

    Board getBoard() { return board; }

    void setupDemo() {
        // Minimal: white rook/knight/pawn, black pieces to block/capture
        board.place(new Pos(7, 0), new Rook(Color.WHITE));
        board.place(new Pos(7, 1), new Knight(Color.WHITE));
        board.place(new Pos(6, 3), new Pawn(Color.WHITE));
        board.place(new Pos(7, 4), new King(Color.WHITE));
        board.place(new Pos(0, 0), new Rook(Color.BLACK));
        board.place(new Pos(4, 3), new Pawn(Color.BLACK));
        board.place(new Pos(0, 4), new King(Color.BLACK));
        board.place(new Pos(5, 2), new Bishop(Color.WHITE));
    }

    void tryMove(Pos from, Pos to) {
        if (board.tryMove(from, to, turn)) {
            turn = turn.opposite();
            System.out.println("Turn -> " + turn);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        game.setupDemo();
        game.getBoard().print();

        System.out.println("\n-- Legal rook slide --");
        game.tryMove(new Pos(7, 0), new Pos(5, 0));

        System.out.println("\n-- Illegal rook (blocked path set up differently) --");
        // place blocker
        game.getBoard().place(new Pos(6, 0), new Pawn(Color.WHITE));
        game.tryMove(new Pos(5, 0), new Pos(0, 0)); // path blocked by pawn at 6,0? from 5,0 to 0,0 passes 4..1 — 6 not on path
        // Actually from 5,0 to 0,0 path is 4,0 3,0 2,0 1,0 — clear. Use different:
        game.tryMove(new Pos(5, 0), new Pos(7, 0)); // back toward own knight/path: 6,0 has pawn

        System.out.println("\n-- Knight jump --");
        game.tryMove(new Pos(7, 1), new Pos(5, 2)); // may fail if bishop there — bishop is at 5,2 WHITE own
        game.tryMove(new Pos(7, 1), new Pos(5, 0));

        System.out.println("\n-- Pawn forward --");
        game.tryMove(new Pos(6, 3), new Pos(5, 3));

        game.getBoard().print();
    }
}
