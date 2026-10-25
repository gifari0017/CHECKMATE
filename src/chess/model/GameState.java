package chess.model;

import java.util.List;

public class GameState {

    private Color turn = Color.WHITE;

    public Color getTurn() {
        return turn;
    }

    public void switchTurn() {
        turn = turn.opposite();
    }

    /**
     * Status for the side whose turn it is.
     *   has a legal move            -> ONGOING
     *   no legal move + in check     -> CHECKMATE
     *   no legal move + NOT in check -> STALEMATE
     */
    public GameResult getResult(Board board) {
        List<Move> legal = MoveGenerator.allLegalMoves(board, turn);

        if (!legal.isEmpty()) {
            return GameResult.ONGOING;
        }
        if (board.isInCheck(turn)) {
            return GameResult.CHECKMATE;
        } else {
            return GameResult.STALEMATE;
        }
    }
}