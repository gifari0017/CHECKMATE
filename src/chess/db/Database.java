package chess.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC data layer. Stores finished games in a local SQLite file (checkmate.db)
 * using plain java.sql (Connection, PreparedStatement, ResultSet).
 *
 * It degrades gracefully: if the SQLite JDBC driver is not on the classpath yet,
 * every call just no-ops (and the game keeps working) instead of crashing. Add
 * the "sqlite-jdbc" library to the project to turn persistence on.
 *
 * Demonstrates the course's JDBC outcome: CREATE TABLE, parameterised INSERT,
 * and a SELECT read back into objects.
 */
public class Database {
    private static final String URL = "jdbc:sqlite:checkmate.db";
    private static boolean ready = false;

    /** Create the table if needed. Safe to call repeatedly. */
    public static void init() {
        try (Connection c = DriverManager.getConnection(URL);
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS games (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "played_at TEXT, mode TEXT, white TEXT, black TEXT, " +
                    "result TEXT, reason TEXT, moves INTEGER, pgn TEXT)");
            ready = true;
        } catch (Throwable t) {
            ready = false;   // driver missing or file locked -> persistence off
            System.err.println("[DB] disabled: " + t.getMessage());
        }
    }

    public static boolean isReady() { return ready; }

    /** Insert one finished game. */
    public static void save(GameRecord g) {
        if (!ready) init();
        if (!ready) return;
        String sql = "INSERT INTO games(played_at,mode,white,black,result,reason,moves,pgn) " +
                "VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.playedAt);
            ps.setString(2, g.mode);
            ps.setString(3, g.white);
            ps.setString(4, g.black);
            ps.setString(5, g.result);
            ps.setString(6, g.reason);
            ps.setInt(7, g.moves);
            ps.setString(8, g.pgn);
            ps.executeUpdate();
        } catch (Throwable t) {
            System.err.println("[DB] save failed: " + t.getMessage());
        }
    }

    /** All games, newest first. */
    public static List<GameRecord> list() {
        List<GameRecord> out = new ArrayList<>();
        if (!ready) init();
        if (!ready) return out;
        try (Connection c = DriverManager.getConnection(URL);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM games ORDER BY id DESC")) {
            while (rs.next()) {
                GameRecord g = new GameRecord();
                g.id       = rs.getInt("id");
                g.playedAt = rs.getString("played_at");
                g.mode     = rs.getString("mode");
                g.white    = rs.getString("white");
                g.black    = rs.getString("black");
                g.result   = rs.getString("result");
                g.reason   = rs.getString("reason");
                g.moves    = rs.getInt("moves");
                g.pgn      = rs.getString("pgn");
                out.add(g);
            }
        } catch (Throwable t) {
            System.err.println("[DB] list failed: " + t.getMessage());
        }
        return out;
    }
}