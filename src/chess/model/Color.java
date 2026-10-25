package chess.model;

/** The two sides in chess. Every piece belongs to exactly one. */
public enum Color {
    WHITE, BLACK;

    /** Handy helper: given one side, return the other. */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
