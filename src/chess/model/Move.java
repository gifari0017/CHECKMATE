package chess.model;

public class Move {
    public final Square from;
    public final Square to;
    public final Piece movedPiece;
    public final PieceType promotion;

    public boolean isEnPassant = false;
    public boolean isCastle = false;
    public Square rookFrom, rookTo;      // only used when isCastle

    public Piece capturedPiece;
    public Square prevEnPassantTarget;
    public boolean[] prevCastling;

    public Move(Square from, Square to, Piece movedPiece) {
        this(from, to, movedPiece, null);
    }

    public Move(Square from, Square to, Piece movedPiece, PieceType promotion) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
        this.promotion = promotion;
    }

    @Override
    public String toString() {
        if (isCastle) {
            return (to.col == 6 ? "O-O (kingside castle)" : "O-O-O (queenside castle)");
        }
        String s = movedPiece.getType() + " " + from + "->" + to;
        if (promotion != null)      s += "=" + promotion;
        if (isEnPassant)            s += " (en passant)";
        if (capturedPiece != null)  s += " (captures " + capturedPiece.getType() + ")";
        return s;
    }
}