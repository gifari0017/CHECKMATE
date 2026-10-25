package chess.model;

/**
 * Perft: count every legal sequence of moves to a given depth. Matching the
 * known-correct numbers PROVES the move generation is correct.
 */
public class Perft {

    public static long perft(Board board, Color side, int depth) {
        if (depth == 0) return 1;

        long nodes = 0;
        for (Move move : MoveGenerator.allLegalMoves(board, side)) {
            board.makeMove(move);
            nodes += perft(board, side.opposite(), depth - 1);
            board.unmakeMove(move);
        }
        return nodes;
    }
}