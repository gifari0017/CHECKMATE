package chess.ui;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Reliable sound engine. Each effect is synthesized ONCE at startup into a pool
 * of reusable Clips. Playing just rewinds and starts a free clip -- no per-call
 * audio-line allocation, so rapid moves never skip a tone. Silent (never crashes)
 * if the machine has no audio device.
 */
public class Sound {
    private static final float RATE = 44100f;
    private static boolean enabled = true;
    private static boolean audioOk = true;

    // a small pool of clips per effect, so overlapping plays don't cut each other
    private static final Map<String, Clip[]> POOL = new HashMap<>();
    private static final int POOL_SIZE = 3;

    static {
        try {
            build("move",    new double[]{660},            new double[]{0.05}, 0.22);
            build("capture", new double[]{300, 200},       new double[]{0.05, 0.05}, 0.25);
            build("swap",    new double[]{720,520,360,520,720}, new double[]{0.04,0.04,0.04,0.04,0.04}, 0.22);
            build("check",   new double[]{880, 0, 880},    new double[]{0.09,0.03,0.09}, 0.30);
            build("win",     new double[]{523,659,784,1047}, new double[]{0.12,0.12,0.12,0.26}, 0.30);
            build("draw",    new double[]{500,380,300},    new double[]{0.12,0.12,0.18}, 0.26);
            build("lose",    new double[]{400,320,250,180},new double[]{0.14,0.14,0.14,0.30}, 0.28);
            build("tick",    new double[]{1000},           new double[]{0.03}, 0.12);
            build("intro",   new double[]{392,523,659,784,659,784,1047},
                    new double[]{0.12,0.12,0.12,0.14,0.10,0.10,0.3}, 0.28);
        } catch (Throwable t) {
            audioOk = false;   // no audio device -> stay silent
        }
    }

    public static void setEnabled(boolean b) { enabled = b; }

    public static void move()    { play("move"); }
    public static void capture() { play("capture"); }
    public static void swap()    { play("swap"); }
    public static void check()   { play("check"); }
    public static void win()     { play("win"); }
    public static void draw()    { play("draw"); }
    public static void lose()    { play("lose"); }
    public static void tick()    { play("tick"); }
    public static void intro()   { play("intro"); }

    /** Rewind and start the first free clip in the effect's pool. */
    private static void play(String key) {
        if (!enabled || !audioOk) return;
        try {
            Clip[] pool = POOL.get(key);
            if (pool == null) return;
            for (Clip c : pool) {
                if (!c.isRunning()) { c.setFramePosition(0); c.start(); return; }
            }
            // all busy: reuse the first
            Clip c = pool[0]; c.stop(); c.setFramePosition(0); c.start();
        } catch (Throwable ignore) { }
    }

    /** Synthesize one effect into a byte buffer and load it into POOL_SIZE clips. */
    private static void build(String key, double[] freq, double[] sec, double vol) throws Exception {
        // Leading silence: the very first start() on a fresh audio line has startup
        // latency that swallows the first few ms. Prepend silence so what gets
        // swallowed is silence, and the actual tone always plays -- even move #1.
        int lead = (int) (0.08 * RATE);               // 80 ms of silence
        int total = lead;
        for (double s : sec) total += (int) (s * RATE);
        byte[] buf = new byte[total * 2];             // zero-filled = silent lead
        int idx = lead * 2;                           // write the tone after the silence
        for (int n = 0; n < freq.length; n++) {
            int samples = (int) (sec[n] * RATE);
            for (int i = 0; i < samples; i++) {
                double env = envelope(i, samples);
                double val = (freq[n] <= 0) ? 0 : Math.sin(2 * Math.PI * freq[n] * i / RATE);
                short s = (short) (val * env * vol * 32767);
                buf[idx++] = (byte) (s & 0xff);
                buf[idx++] = (byte) ((s >> 8) & 0xff);
            }
        }
        AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
        Clip[] pool = new Clip[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            Clip clip = AudioSystem.getClip();
            clip.open(fmt, buf, 0, buf.length);
            pool[i] = clip;
        }
        POOL.put(key, pool);
    }

    private static double envelope(int i, int samples) {
        double a = Math.min(1.0, i / (0.008 * RATE));
        double r = Math.min(1.0, (samples - i) / (0.03 * RATE));
        return Math.max(0, Math.min(a, r));
    }
}