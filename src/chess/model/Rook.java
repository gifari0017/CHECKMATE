package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {

    public Rook(Color color) {
        super(color, PieceType.ROOK);
    }

    @Override
    public List<Square> generateMoves(Board board, Square from) {
        List<Square> moves = new ArrayList<>();

        // The 4 STRAIGHT directions: up, down, left, right.
        int[][] directions = {
                {-1, 0}, {+1, 0},
                {0, -1}, {0, +1}
        };

        for (int[] d : directions) {
            int r = from.row + d[0];
            int c = from.col + d[1];

            while (onBoard(r, c)) {
                Piece target = board.get(r, c);
                if (target == null) {
                    moves.add(new Square(r, c));
                } else {
                    if (target.getColor() != color) {
                        moves.add(new Square(r, c));
                    }
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }

        return moves;
    }
}