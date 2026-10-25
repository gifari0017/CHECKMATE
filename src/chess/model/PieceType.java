package chess.model;

/**
 * The six kinds of chess piece.
 * Each one carries a letter (for printing) and a rough point value
 * (the AI will use these values later to judge who is winning).
 */
public enum PieceType {
    PAWN('P', 100),
    KNIGHT('N', 320),
    BISHOP('B', 330),
    ROOK('R', 500),
    QUEEN('Q', 900),
    KING('K', 20000);   // king is priceless; big number = "never lose it"

    public final char letter;
    public final int value;

    PieceType(char letter, int value) {
        this.letter = letter;
        this.value = value;
    }
}
