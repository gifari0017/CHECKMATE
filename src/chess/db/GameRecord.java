package chess.db;

/** One finished game, as stored in / read from the database. Plain data holder. */
public class GameRecord {
    public int id;
    public String playedAt;   // "2026-10-26 01:20"
    public String mode;       // "Local" / "vs AI" / "LAN"
    public String white;      // player label
    public String black;
    public String result;     // "White" / "Black" / "Draw"
    public String reason;     // "Checkmate" / "Stalemate" / "Timeout" / "Resign"
    public int moves;         // full-move count
    public String pgn;        // "1. e4 e5 2. Nf3 Nc6 ..."
}