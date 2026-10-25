package chess.ui;

import chess.model.Piece;
import chess.model.PieceType;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Loads the 12 piece PNGs once and shares them across the UI. */
public class PieceImages {
    private static final Map<String, Image> M = new HashMap<>();
    static {
        String[] colors = {"w", "b"};
        String[] types  = {"K", "Q", "R", "B", "N", "P"};
        for (String c : colors)
            for (String t : types) {
                File f = new File("pieces/" + c + t + ".png");
                if (f.exists()) M.put(c + t, new ImageIcon(f.getPath()).getImage());
            }
    }
    public static Image forPiece(Piece p) {
        return M.get((p.getColor() == chess.model.Color.WHITE ? "w" : "b") + letter(p.getType()));
    }
    public static Image forKey(String key) { return M.get(key); }

    public static String letter(PieceType t) {
        switch (t) { case KING: return "K"; case QUEEN: return "Q"; case ROOK: return "R";
            case BISHOP: return "B"; case KNIGHT: return "N"; default: return "P"; }
    }
    public static String glyph(PieceType t) {
        switch (t) { case KING: return "\u265A"; case QUEEN: return "\u265B"; case ROOK: return "\u265C";
            case BISHOP: return "\u265D"; case KNIGHT: return "\u265E"; default: return "\u265F"; }
    }
}