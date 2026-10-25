package chess.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Piece {
    protected final Color color;
    protected final PieceType type;

    protected Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public Color getColor() { return color; }
    public PieceType getType() { return type; }

    public char symbol() {
        char c = type.letter;
        return (color == Color.WHITE)
                ? Character.toUpperCase(c)
                : Character.toLowerCase(c);
    }

    /** List every square this piece could move to. Default = none; each piece overrides. */
    public List<Square> generateMoves(Board board, Square from) {
        return new ArrayList<>();
    }

    /** True if (row, col) is a real square inside the 8x8 board. */
    protected boolean onBoard(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}