package chess.net;

import chess.model.Move;

/**
 * The tiny text protocol the two computers speak over the socket. One message
 * per line. Keeping it human-readable makes it easy to debug and to explain.
 *
 *   START <COLOR>     sent once by the host after a client connects; tells the
 *                     client which colour the HOST is, so the client takes the
 *                     opposite. e.g. "START WHITE".
 *   MOVE fr fc tr tc p   a move: from (row,col) to (row,col) and a promotion
 *                     letter, or '-' for none. e.g. "MOVE 6 4 4 4 -"  (e2-e4).
 *   RESIGN            the sender resigns.
 *
 * Only from/to/promotion travel. The receiver re-generates the legal moves for
 * that square with its own MoveGenerator and picks the matching one, so both
 * sides stay perfectly in sync and an illegal/forged move is simply ignored.
 */
public class Protocol {
    public static final int PORT = 50505;      // the LAN port both sides use

    public static final String START  = "START";
    public static final String MOVE   = "MOVE";
    public static final String RESIGN = "RESIGN";
    public static final String DISCONNECT = "DISCONNECT";   // synthesized locally

    /** Serialize a move to one line. */
    public static String move(Move m) {
        char p = (m.promotion == null) ? '-' : m.promotion.letter;
        return MOVE + " " + m.from.row + " " + m.from.col + " "
                + m.to.row + " " + m.to.col + " " + p;
    }
}