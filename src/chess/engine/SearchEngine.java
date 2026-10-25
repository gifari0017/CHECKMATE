package chess.engine;

import chess.model.*;
import java.util.List;

/**
 * The AI "brain". Given a position, it looks ahead several moves and returns
 * the move it thinks is best.
 *
 * Technique: MINIMAX with ALPHA-BETA PRUNING (written in the compact "negamax"
 * form, where we always score from the point of view of the side to move and
 * flip the sign at each level -- that one trick removes a lot of duplicate code).
 *
 *   - minimax:   I pick the move that is best for me, assuming my opponent
 *                then picks the move that is best for HIM (= worst for me),
 *                and so on, all the way down the tree.
 *   - alpha-beta: as soon as a branch is proven worse than one we already have,
 *                stop looking at it. Same answer, far fewer positions examined.
 *   - move ordering: try captures first. They cause the most cut-offs, so
 *                alpha-beta prunes earlier and the search runs much faster.
 *
 * The engine knows nothing about the screen -- it only reads the Board, makes a
 * move, recurses, and un-makes it (your perft-verified unmakeMove restores
 * everything perfectly, which is exactly why a search like this is safe).
 */
public class SearchEngine {

    private static final int INF = 1_000_000;

    /** How many positions the last search looked at (nice to show / mention in viva). */
    public long nodes = 0;

    /**
     * The move the AI wants to play for 'side', searching 'depth' plies ahead.
     * Returns null only if 'side' has no legal moves (already mate / stalemate).
     */
    public Move bestMove(Board board, Color side, int depth) {
        nodes = 0;
        List<Move> moves = MoveGenerator.allLegalMoves(board, side);
        if (moves.isEmpty()) return null;
        order(board, moves);

        Move best = moves.get(0);
        int alpha = -INF, beta = INF;

        for (Move m : moves) {
            board.makeMove(m);
            // score of this move = negamax from the opponent's view, negated
            int score = -negamax(board, side.opposite(), depth - 1, 1, -beta, -alpha);
            board.unmakeMove(m);

            if (score > alpha) {   // strictly better -> new best move
                alpha = score;
                best = m;
            }
        }
        return best;
    }

    /**
     * Returns the value of the position for 'side', searching 'depth' more plies.
     * 'ply' is how deep we already are from the root (used to prefer FAST mates).
     */
    private int negamax(Board board, Color side, int depth, int ply, int alpha, int beta) {
        nodes++;

        List<Move> moves = MoveGenerator.allLegalMoves(board, side);

        if (moves.isEmpty()) {
            // No legal moves: checkmate (in check) or stalemate (not in check).
            if (board.isInCheck(side)) return -(Evaluator.MATE - ply); // getting mated is very bad
            return 0;                                                   // stalemate = draw
        }
        if (depth == 0) return evalFor(board, side);

        order(board, moves);
        int best = -INF;

        for (Move m : moves) {
            board.makeMove(m);
            int score = -negamax(board, side.opposite(), depth - 1, ply + 1, -beta, -alpha);
            board.unmakeMove(m);

            if (score > best)  best = score;
            if (best > alpha)  alpha = best;
            if (alpha >= beta) break;   // ALPHA-BETA CUT-OFF: opponent won't allow this line
        }
        return best;
    }

    /** Evaluator is written White-positive; flip it so '+' always means good-for-side. */
    private int evalFor(Board board, Color side) {
        int white = Evaluator.evaluate(board);
        return (side == Color.WHITE) ? white : -white;
    }

    /** Cheap move ordering: biggest captures first (victim value), quiet moves last. */
    private void order(Board board, List<Move> moves) {
        moves.sort((a, b) -> victim(board, b) - victim(board, a));
    }
    private int victim(Board board, Move m) {
        Piece target = board.get(m.to.row, m.to.col);
        return (target == null) ? 0 : target.getType().value;
    }
}