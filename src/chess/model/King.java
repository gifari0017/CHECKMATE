package chess.model;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    public King(Color color) {
        super(color, PieceType.KING);
    }

    @Override
    public List<Square> generateMoves(Board board, Square from) {
        List<Square> moves = new ArrayList<>();

        // All 8 directions -- same list as the queen. Difference: the king
        // takes ONE step only (no while-loop slide).
        int[][] directions = {
                {-1, 0}, {+1, 0}, {0, -1}, {0, +1},
                {-1, -1}, {-1, +1}, {+1, -1}, {+1, +1}
        };

        for (int[] d : directions) {
            int r = from.row + d[0];
            int c = from.col + d[1];

            if (!onBoard(r, c)) continue;

            Piece target = board.get(r, c);
            if (target == null || target.getColor() != color) {
                moves.add(new Square(r, c));
            }
        }

        return moves;
    }
}