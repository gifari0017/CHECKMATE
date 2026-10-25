package chess.model;

/** How a position stands right now. */
public enum GameResult {
    ONGOING,      // game continues
    CHECKMATE,    // side to move is checkmated (they lose)
    STALEMATE     // side to move has no legal move but isn't in check (draw)
}