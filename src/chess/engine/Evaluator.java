package chess.engine;

import chess.model.*;

/**
 * Scores a chess position as a single number.
 *   positive  = White is better
 *   negative  = Black is better
 *   0         = roughly equal
 *
 * Two ingredients:
 *   1. MATERIAL  -- add up the value of every piece (pawn 100, knight 320, ...).
 *   2. PIECE-SQUARE TABLES (PST) -- a small bonus/penalty for WHERE a piece sits.
 *      A knight in the centre is worth more than one in the corner; a pawn one
 *      step from promoting is worth more than one still at home. Each table is
 *      just 64 numbers, laid out exactly like the board prints (row 0 = rank 8,
 *      the top). The numbers are written from WHITE's point of view; for a BLACK
 *      piece we flip the table top-to-bottom (row 7 - r) so it's fair for both.
 *
 * These tables are the standard "simplified evaluation" values -- copied, not
 * invented. That is normal and expected for a chess engine.
 */
public class Evaluator {

    // Returned when the side to move is checkmated. Huge, but below the King's
    // own value so it never overflows when combined.
    public static final int MATE = 100_000;

    private static final int[] PAWN = {
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] KNIGHT = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] BISHOP = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] ROOK = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] QUEEN = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] KING = {   // middlegame: king wants to hide in a corner
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    /** The whole-board score, from White's point of view. */
    public static int evaluate(Board b) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = b.get(r, c);
                if (p == null) continue;
                int piece = p.getType().value + pst(p.getType(), r, c, p.getColor());
                score += (p.getColor() == Color.WHITE) ? piece : -piece;
            }
        }
        return score;
    }

    /** Look up the square bonus for one piece. Black reads the table mirrored. */
    private static int pst(PieceType t, int row, int col, Color color) {
        int r = (color == Color.WHITE) ? row : 7 - row;   // flip for Black
        int i = r * 8 + col;
        switch (t) {
            case PAWN:   return PAWN[i];
            case KNIGHT: return KNIGHT[i];
            case BISHOP: return BISHOP[i];
            case ROOK:   return ROOK[i];
            case QUEEN:  return QUEEN[i];
            case KING:   return KING[i];
            default:     return 0;
        }
    }
}