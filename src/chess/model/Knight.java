package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    public Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public List<Square> generateMoves(Board board, Square from) {
        List<Square> moves = new ArrayList<>();

        // The 8 L-shaped jumps: (row change, col change).
        // Each is "2 in one direction, 1 in the other".
        int[][] jumps = {
                {-2, -1}, {-2, +1},
                {+2, -1}, {+2, +1},
                {-1, -2}, {+1, -2},
                {-1, +2}, {+1, +2}
        };

        for (int[] j : jumps) {
            int r = from.row + j[0];
            int c = from.col + j[1];

            if (!onBoard(r, c)) continue;   // jumped off the board -> skip

            Piece target = board.get(r, c);
            // Land only if empty OR holds an enemy. Never on a friendly piece.
            if (target == null || target.getColor() != color) {
                moves.add(new Square(r, c));
            }
        }

        return moves;
    }
}