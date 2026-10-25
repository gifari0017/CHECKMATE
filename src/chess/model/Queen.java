package chess.model;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {

    public Queen(Color color) {
        super(color, PieceType.QUEEN);
    }

    @Override
    public List<Square> generateMoves(Board board, Square from) {
        List<Square> moves = new ArrayList<>();

        // Queen = rook + bishop. ALL 8 directions.
        int[][] directions = {
                {-1, 0}, {+1, 0}, {0, -1}, {0, +1},
                {-1, -1}, {-1, +1}, {+1, -1}, {+1, +1}
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