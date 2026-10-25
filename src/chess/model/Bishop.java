package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Bishop extends Piece {

    public Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }

    @Override
    public List<Square> generateMoves(Board board, Square from) {
        List<Square> moves = new ArrayList<>();

        // The 4 diagonal directions: (row step, col step).
        int[][] directions = {
                {-1, -1}, {-1, +1},
                {+1, -1}, {+1, +1}
        };

        // For each direction, SLIDE until something stops us.
        for (int[] d : directions) {
            int r = from.row + d[0];
            int c = from.col + d[1];

            while (onBoard(r, c)) {
                Piece target = board.get(r, c);

                if (target == null) {
                    moves.add(new Square(r, c));   // empty: move here, keep sliding
                } else {
                    if (target.getColor() != color) {
                        moves.add(new Square(r, c)); // enemy: capture...
                    }
                    break;                            // ...then STOP
                }

                r += d[0];   // step again, same direction
                c += d[1];
            }
        }

        return moves;
    }
}