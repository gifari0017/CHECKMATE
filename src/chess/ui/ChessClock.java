package chess.ui;

import chess.model.Color;
import javax.swing.Timer;
import java.awt.event.ActionListener;

/**
 * A two-player chess clock. Each side has its own remaining time in millis.
 * A Swing Timer ticks ~10x/second and decrements whoever is "running".
 * When a side hits zero, onFlag is fired (that side loses).
 *
 * totalMillis == 0 means "no timer" (the Off mode): the clock stays idle.
 */
public class ChessClock {
    private long whiteMs, blackMs;
    private final long startMs;
    private Color running = null;             // whose clock is counting (null = paused)
    private final Timer timer;
    private boolean flagged = false;

    private Runnable onTick = () -> {};
    private java.util.function.Consumer<Color> onFlag = c -> {};

    public ChessClock(int minutes) {
        this.startMs = minutes * 60_000L;
        this.whiteMs = startMs;
        this.blackMs = startMs;

        // tick every 100 ms
        timer = new Timer(100, (ActionListener) e -> tick());
    }

    public boolean isEnabled() { return startMs > 0; }

    public void setOnTick(Runnable r) { onTick = r; }
    public void setOnFlag(java.util.function.Consumer<Color> c) { onFlag = c; }

    /** Start counting for a given side (call after each move, for the new side to move). */
    public void startFor(Color side) {
        if (!isEnabled() || flagged) return;
        running = side;
        if (!timer.isRunning()) timer.start();
    }

    public void pause() { timer.stop(); running = null; }

    public void reset() {
        timer.stop();
        whiteMs = startMs; blackMs = startMs;
        running = null; flagged = false;
        onTick.run();
    }

    private void tick() {
        if (running == null) return;
        if (running == Color.WHITE) whiteMs -= 100; else blackMs -= 100;

        if (whiteMs <= 0 || blackMs <= 0) {
            Color loser = (whiteMs <= 0) ? Color.WHITE : Color.BLACK;
            whiteMs = Math.max(0, whiteMs);
            blackMs = Math.max(0, blackMs);
            flagged = true;
            timer.stop();
            running = null;
            onTick.run();
            onFlag.accept(loser);
            return;
        }
        onTick.run();
    }

    public long getMillis(Color c) { return c == Color.WHITE ? whiteMs : blackMs; }

    /** Format as M:SS (or MM:SS). */
    public static String format(long ms) {
        long total = Math.max(0, ms) / 1000;
        long m = total / 60, s = total % 60;
        return String.format("%d:%02d", m, s);
    }
}