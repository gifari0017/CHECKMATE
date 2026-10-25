package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Square> generateMoves(Board board, Square from) {
        List<Square> moves = new ArrayList<>();

        int dir      = (color == Color.WHITE) ? -1 : 1;
        int startRow = (color == Color.WHITE) ?  6 : 1;

        int r = from.row;
        int c = from.col;

        // Forward one.
        int r1 = r + dir;
        if (onBoard(r1, c) && board.get(r1, c) == null) {
            moves.add(new Square(r1, c));
            int r2 = r + 2 * dir;
            if (r == startRow && board.get(r2, c) == null) {
                moves.add(new Square(r2, c));
            }
        }

        // Diagonal captures -- a real enemy piece, OR the en-passant target square.
        Square ep = board.getEnPassantTarget();
        for (int dc : new int[]{ -1, 1 }) {
            int cr = r + dir;
            int cc = c + dc;
            if (!onBoard(cr, cc)) continue;

            Piece target = board.get(cr, cc);
            boolean enemyThere = target != null && target.getColor() != color;
            boolean isEpSquare = ep != null && ep.row == cr && ep.col == cc;

            if (enemyThere || isEpSquare) {
                moves.add(new Square(cr, cc));
            }
        }

        return moves;
    }
}