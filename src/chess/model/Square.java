package chess.model;

/**
 * One square on the board, named by row and column.
 *   row: 0 = top of the board (rank 8) ... 7 = bottom (rank 1)
 *   col: 0 = file 'a' ... 7 = file 'h'
 * We barely use this in Week 1, but from Week 2 onward every move
 * is "from one Square to another Square", so it becomes the backbone.
 */
public class Square {
    public final int row;
    public final int col;

    public Square(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Is this square actually inside the 8x8 board? */
    public boolean isOnBoard() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /** Print in normal chess notation, e.g. "e1". */
    @Override
    public String toString() {
        char file = (char) ('a' + col);
        int rank = 8 - row;
        return "" + file + rank;
    }
}
