package chess.ui;

import chess.model.Color;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Opening menu, shown before the board (as the professor asked):
 *   Mode  ->  Pass & Play / Play vs AI / Play with Friends
 *   if Play vs AI  ->  Difficulty (Easy / Medium / Hard)  +  your Side (White / Black)
 *
 * Styled in the cream / forest-green / gold identity from the Week-6 slide deck,
 * so the whole app reads as one piece with the green board.
 */
public class StartMenu extends JPanel {

    public interface Listener {
        void onStart(boolean vsComputer, Color humanSide, int depth, int minutes);
    }
    public interface NetListener {
        void onHost(Color hostColor);   // this computer hosts, playing hostColor
        void onJoin(String ip);         // this computer joins the host at ip
    }

    private static final java.awt.Color CREAM     = new java.awt.Color(0xF1, 0xEC, 0xDD);
    private static final java.awt.Color CREAM2    = new java.awt.Color(0xE7, 0xE1, 0xCE);
    private static final java.awt.Color GREEN      = new java.awt.Color(0x20, 0x49, 0x2C);
    private static final java.awt.Color GREEN2     = new java.awt.Color(0x17, 0x38, 0x1F);
    private static final java.awt.Color GREEN_LINE = new java.awt.Color(0x2E, 0x5A, 0x39);
    private static final java.awt.Color GOLD       = new java.awt.Color(0xCB, 0xA2, 0x4C);
    private static final java.awt.Color INK        = new java.awt.Color(0x20, 0x33, 0x1E);
    private static final java.awt.Color SAGE       = new java.awt.Color(0x7F, 0x90, 0x79);
    private static final java.awt.Color CREAMTX    = new java.awt.Color(0xEF, 0xEA, 0xDB);
    private static final java.awt.Color DESC       = new java.awt.Color(0xC9, 0xD3, 0xC2);
    private static final String SF = "Segoe UI Symbol";

    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);

    private Runnable onHistory = () -> {};

    public StartMenu(Listener listener, NetListener net, Runnable onHistory) {
        this.onHistory = onHistory;
        setBackground(CREAM);
        setLayout(new GridBagLayout());                 // centers the deck
        setPreferredSize(new Dimension(500, 600));

        deck.setOpaque(false);
        deck.add(mainPanel(listener), "main");
        deck.add(computerPanel(listener), "computer");
        deck.add(lanPanel(net), "lan");
        add(deck);
        cards.show(deck, "main");
    }

    // ----- screen 1: pick a mode -----
    private JPanel mainPanel(Listener listener) {
        JPanel p = column();
        p.add(brand());
        p.add(eyebrow("Choose your battle"));
        p.add(Box.createVerticalStrut(26));

        p.add(modeCard("♟", "Pass & Play",
                "Two players, one screen. Take turns.", true,
                () -> { int min = askTime(); if (min >= 0) listener.onStart(false, Color.WHITE, 0, min); }));
        p.add(Box.createVerticalStrut(13));
        p.add(modeCard("♞", "Play vs AI",
                "Challenge the computer. Pick your level.", true,
                () -> cards.show(deck, "computer")));
        p.add(Box.createVerticalStrut(13));
        p.add(modeCard("♜", "Play with Friends",
                "Face a friend over your local network.", true,
                () -> cards.show(deck, "lan")));

        p.add(Box.createVerticalStrut(22));
        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        links.setOpaque(false);
        links.setAlignmentX(CENTER_ALIGNMENT);
        links.add(textButton("☰  Game History", e -> onHistory.run()));
        links.add(textButton("ⓘ  About Us", e -> showAbout()));
        p.add(links);
        return p;
    }

    // ----- screen 2: difficulty + side (segmented pill toggles) -----
    private final int[] levelPick = {1};   // 0 Easy, 1 Medium, 2 Hard
    private final int[] sidePick  = {0};   // 0 White, 1 Black
    private final int[] timePick  = {0};   // 0 Off, 1=5, 2=10, 3=15 min
    private final int[] hostColorPick = {0}; // 0 White, 1 Black

    private JPanel computerPanel(Listener listener) {
        JPanel p = column();
        p.add(brand());
        p.add(eyebrow("Play vs AI"));
        p.add(Box.createVerticalStrut(22));

        p.add(label("Difficulty"));
        p.add(segment(levelPick, "Easy", "Medium", "Hard"));
        p.add(Box.createVerticalStrut(18));

        p.add(label("Play as"));
        p.add(segment(sidePick, "White", "Black"));
        p.add(Box.createVerticalStrut(16));

        p.add(label("Time control"));
        p.add(segment(timePick, "Off", "5 min", "10 min", "15 min"));
        p.add(Box.createVerticalStrut(24));

        p.add(startButton(e -> {
            int depth = switch (levelPick[0]) { case 0 -> 2; case 2 -> 4; default -> 3; };
            Color human = (sidePick[0] == 1) ? Color.BLACK : Color.WHITE;
            int minutes = new int[]{0, 5, 10, 15}[timePick[0]];
            listener.onStart(true, human, depth, minutes);
        }));
        p.add(Box.createVerticalStrut(12));
        p.add(textButton("←  Back", e -> cards.show(deck, "main")));
        return p;
    }

    /** A row of pill buttons; clicking one selects it and writes the index into pick[0]. */
    private JPanel segment(int[] pick, String... options) {
        JPanel row = new JPanel(new GridLayout(1, options.length, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(300, 46));
        row.setPreferredSize(new Dimension(300, 46));
        JButton[] btns = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            JButton b = new JButton(options[i]);
            b.setFont(new Font("SansSerif", Font.PLAIN, 15));
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { pick[0] = idx; paintSegment(btns, pick[0]); });
            btns[i] = b;
        }
        for (JButton b : btns) row.add(b);
        paintSegment(btns, pick[0]);
        return row;
    }
    private void paintSegment(JButton[] btns, int sel) {
        for (int i = 0; i < btns.length; i++) {
            boolean on = (i == sel);
            btns[i].setBackground(on ? GREEN : java.awt.Color.WHITE);
            btns[i].setForeground(on ? CREAMTX : INK);
            btns[i].setBorder(BorderFactory.createLineBorder(on ? GREEN : new java.awt.Color(0xCF, 0xC6, 0xAD), 2, true));
        }
    }

    // ----- screen 3: LAN (host / join) -----
    private JPanel lanPanel(NetListener net) {
        JPanel p = column();
        p.add(brand());
        p.add(eyebrow("Play with Friends"));
        p.add(Box.createVerticalStrut(18));

        // HOST
        p.add(label("Host a game"));
        p.add(segment(hostColorPick, "Play White", "Play Black"));
        p.add(Box.createVerticalStrut(10));
        p.add(wideButton("⌂  Start Hosting", e -> {
            Color c = (hostColorPick[0] == 1) ? Color.BLACK : Color.WHITE;
            net.onHost(c);
        }));

        p.add(Box.createVerticalStrut(22));
        p.add(hint("—————  or  —————"));
        p.add(Box.createVerticalStrut(16));

        // JOIN
        p.add(label("Join a game"));
        JTextField ip = new JTextField();
        ip.setMaximumSize(new Dimension(300, 38));
        ip.setPreferredSize(new Dimension(300, 38));
        ip.setAlignmentX(CENTER_ALIGNMENT);
        ip.setFont(new Font("SansSerif", Font.PLAIN, 15));
        ip.setHorizontalAlignment(JTextField.CENTER);
        p.add(ip);
        p.add(Box.createVerticalStrut(4));
        p.add(hint("Type the host's IP address (e.g. 192.168.0.12)"));
        p.add(Box.createVerticalStrut(10));
        p.add(wideButton("→  Connect", e -> {
            String addr = ip.getText().trim();
            if (!addr.isEmpty()) net.onJoin(addr);
        }));

        p.add(Box.createVerticalStrut(18));
        p.add(textButton("←  Back", e -> cards.show(deck, "main")));
        return p;
    }
    private JLabel hint(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(SAGE);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }
    private JButton wideButton(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.setFont(new Font("Serif", Font.BOLD, 17));
        b.setForeground(CREAMTX);
        b.setBackground(GREEN);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(300, 46));
        b.setPreferredSize(new Dimension(300, 46));
        b.setBorder(BorderFactory.createLineBorder(GREEN_LINE, 1, true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(a);
        return b;
    }

    // ----- building blocks -----
    private JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(26, 40, 26, 40));
        return p;
    }
    private JPanel brand() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        JLabel crown = new JLabel("♛");
        crown.setFont(new Font(SF, Font.PLAIN, 42));
        crown.setForeground(GOLD);
        JLabel name = new JLabel("CHECKMATE");
        name.setFont(new Font("Serif", Font.BOLD, 44));
        name.setForeground(GREEN);
        row.add(crown); row.add(name);
        return row;
    }
    private JLabel eyebrow(String t) {
        JLabel l = new JLabel(t.toUpperCase());
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(SAGE);
        l.setAlignmentX(CENTER_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return l;
    }
    private JLabel label(String t) {
        JLabel l = new JLabel(t.toUpperCase());
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(SAGE);
        l.setAlignmentX(CENTER_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return l;
    }

    /** A clickable mode card: gold icon, name + description, hover darkens. */
    private JPanel modeCard(String icon, String name, String desc, boolean enabled, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setMaximumSize(new Dimension(460, 78));
        card.setPreferredSize(new Dimension(460, 78));
        card.setAlignmentX(CENTER_ALIGNMENT);
        card.setBackground(enabled ? GREEN : CREAM2);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(enabled ? GREEN_LINE : new java.awt.Color(0xD6, 0xCF, 0xBB), 1, true),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JLabel ic = new JLabel(icon);
        ic.setFont(new Font(SF, Font.PLAIN, 26));
        ic.setForeground(enabled ? GOLD : new java.awt.Color(0xB3, 0xA8, 0x77));
        card.add(ic, BorderLayout.WEST);

        JPanel tx = new JPanel();
        tx.setOpaque(false);
        tx.setLayout(new BoxLayout(tx, BoxLayout.Y_AXIS));
        JLabel nm = new JLabel(name);
        nm.setFont(new Font("Serif", Font.BOLD, 21));
        nm.setForeground(enabled ? CREAMTX : new java.awt.Color(0x7D, 0x74, 0x5C));
        JLabel ds = new JLabel(desc);
        ds.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ds.setForeground(enabled ? DESC : new java.awt.Color(0x9A, 0x92, 0x7C));
        tx.add(nm); tx.add(Box.createVerticalStrut(2)); tx.add(ds);
        card.add(tx, BorderLayout.CENTER);

        if (!enabled) {
            JLabel badge = new JLabel("SOON");
            badge.setFont(new Font("SansSerif", Font.BOLD, 10));
            badge.setForeground(new java.awt.Color(0x6A, 0x62, 0x48));
            card.add(badge, BorderLayout.EAST);
        } else {
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { card.setBackground(GREEN2); }
                @Override public void mouseExited(MouseEvent e)  { card.setBackground(GREEN); }
                @Override public void mousePressed(MouseEvent e) { if (action != null) action.run(); }
            });
        }
        return card;
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(new Font("SansSerif", Font.PLAIN, 15));
        c.setFocusable(false);
        c.setMaximumSize(new Dimension(300, 36));
        c.setPreferredSize(new Dimension(300, 36));
        c.setAlignmentX(CENTER_ALIGNMENT);
        c.setBackground(java.awt.Color.WHITE);
        return c;
    }
    private JButton startButton(java.awt.event.ActionListener a) {
        JButton b = new JButton("▶   Start Game");
        b.setFont(new Font("Serif", Font.BOLD, 20));
        b.setForeground(new java.awt.Color(0x2B, 0x24, 0x10));
        b.setBackground(GOLD);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(300, 52));
        b.setPreferredSize(new Dimension(300, 52));
        b.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(a);
        return b;
    }
    private JButton textButton(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 15));
        b.setForeground(SAGE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (a != null) b.addActionListener(a);
        return b;
    }
    /** Game-ish modal: pick a time control before a Pass & Play game. Returns
     *  minutes (0 = no timer), or -1 if the player closed it without choosing. */
    private int askTime() {
        final int[] result = {-1};
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Time Control", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CREAM);
        p.setBorder(BorderFactory.createEmptyBorder(26, 40, 26, 40));

        JLabel crown = new JLabel("♛");
        crown.setFont(new Font(SF, Font.PLAIN, 34));
        crown.setForeground(GOLD);
        crown.setAlignmentX(CENTER_ALIGNMENT);
        JLabel t = new JLabel("How long per side?");
        t.setFont(new Font("Serif", Font.BOLD, 26));
        t.setForeground(GREEN);
        t.setAlignmentX(CENTER_ALIGNMENT);
        p.add(crown); p.add(Box.createVerticalStrut(6)); p.add(t);
        p.add(eyebrow("Pass & Play — choose your clock"));
        p.add(Box.createVerticalStrut(22));

        String[] labels = {"∞  No timer", "⏱  5 minutes", "⏱  10 minutes", "⏱  15 minutes"};
        int[] mins = {0, 5, 10, 15};
        for (int i = 0; i < labels.length; i++) {
            final int m = mins[i];
            JButton b = new JButton(labels[i]);
            b.setFont(new Font("Serif", Font.BOLD, 18));
            b.setForeground(CREAMTX);
            b.setBackground(GREEN);
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setAlignmentX(CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(280, 46));
            b.setPreferredSize(new Dimension(280, 46));
            b.setBorder(BorderFactory.createLineBorder(GREEN_LINE, 1, true));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { result[0] = m; d.dispose(); });
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { b.setBackground(GREEN2); }
                @Override public void mouseExited(MouseEvent e)  { b.setBackground(GREEN); }
            });
            p.add(b);
            if (i < labels.length - 1) p.add(Box.createVerticalStrut(10));
        }

        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);   // blocks (modal) until a button or close
        return result[0];
    }

    private void showAbout() {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "About Us", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CREAM);
        p.setBorder(BorderFactory.createEmptyBorder(26, 48, 24, 48));

        JLabel crown = new JLabel("♛");
        crown.setFont(new Font(SF, Font.PLAIN, 34));
        crown.setForeground(GOLD); crown.setAlignmentX(CENTER_ALIGNMENT);
        JLabel title = new JLabel("CHECKMATE");
        title.setFont(new Font("Serif", Font.BOLD, 30));
        title.setForeground(GREEN); title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel tag = new JLabel("A Java Chess Game");
        tag.setFont(new Font("Serif", Font.ITALIC, 15));
        tag.setForeground(SAGE); tag.setAlignmentX(CENTER_ALIGNMENT);
        p.add(crown); p.add(Box.createVerticalStrut(4)); p.add(title);
        p.add(Box.createVerticalStrut(2)); p.add(tag);
        p.add(Box.createVerticalStrut(20));

        p.add(label("Developed by"));
        p.add(Box.createVerticalStrut(8));

        // two photos side by side, name + subline below each
        JPanel devs = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        devs.setOpaque(false);
        devs.setAlignmentX(CENTER_ALIGNMENT);
        devs.add(devColumn("about/nukhyai.jpg", "Nukhyai U Marma", "CSE Undergraduate, BUP", "NM"));
        devs.add(devColumn("about/abdus.jpg",   "Abdus Salam Gifari", "CSE Undergraduate, BUP", "AG"));
        // render avatars at 2x and let Swing show them crisp even on high-DPI screens
        p.add(devs);
        p.add(Box.createVerticalStrut(18));

        p.add(label("Supervised by"));
        p.add(aboutLine("Md Musfiqur Rahman Mridha", true));
        p.add(aboutLine("Lecturer, Dept of ICT, BUP", false));
        p.add(Box.createVerticalStrut(6));
        p.add(aboutLine("Anika Ashraf", true));
        p.add(aboutLine("Lecturer, Dept of CSE, FECE, MIST", false));
        p.add(Box.createVerticalStrut(22));

        JButton close = new JButton("Close");
        close.setFont(new Font("Serif", Font.BOLD, 16));
        close.setForeground(new java.awt.Color(0x2B, 0x24, 0x10));
        close.setBackground(GOLD);
        close.setOpaque(true);
        close.setFocusPainted(false);
        close.setAlignmentX(CENTER_ALIGNMENT);
        close.setBorder(BorderFactory.createEmptyBorder(8, 30, 8, 30));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> d.dispose());
        p.add(close);

        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }
    private JLabel aboutLine(String t, boolean strong) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", strong ? Font.BOLD : Font.PLAIN, strong ? 15 : 13));
        l.setForeground(strong ? INK : SAGE);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    /** One developer: circular photo (gold ring) above a name and a subline. */
    private JPanel devColumn(String photoPath, String name, String sub, String initials) {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        // High-res bitmap drawn down at paint time -> stays crisp at any display scale
        Avatar pic = new Avatar(buildAvatar(photoPath, 512, initials), 210);
        pic.setAlignmentX(CENTER_ALIGNMENT);
        JLabel nm = new JLabel(name);
        nm.setFont(new Font("SansSerif", Font.BOLD, 15)); nm.setForeground(INK);
        nm.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sb = new JLabel(sub);
        sb.setFont(new Font("SansSerif", Font.PLAIN, 12)); sb.setForeground(SAGE);
        sb.setAlignmentX(CENTER_ALIGNMENT);
        col.add(pic); col.add(Box.createVerticalStrut(10)); col.add(nm);
        col.add(Box.createVerticalStrut(2)); col.add(sb);
        return col;
    }

    /** Shows a high-resolution avatar bitmap scaled down to its box at paint time.
     *  Downscaling stays sharp whatever the Windows display-scaling factor is
     *  (the earlier icon approach got upscaled by Windows and looked soft). */
    private static class Avatar extends JComponent {
        private final BufferedImage img;
        Avatar(BufferedImage img, int size) {
            this.img = img;
            Dimension dim = new Dimension(size, size);
            setPreferredSize(dim); setMinimumSize(dim); setMaximumSize(dim);
            setOpaque(false); setAlignmentX(CENTER_ALIGNMENT);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
            g.dispose();
        }
    }

    /** Build the avatar bitmap at a given pixel size, with a high-quality
     *  downscale and an antialiased circular mask (smooth, not pixelated). */
    private BufferedImage buildAvatar(String path, int d, String initials) {
        int ring = Math.max(4, Math.round(d / 26f)), tot = d + ring * 2;

        BufferedImage photo = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = photo.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        pg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        BufferedImage src = null;
        try { File f = new File(path); if (f.exists()) src = ImageIO.read(f); } catch (Throwable ignore) {}
        if (src != null) {
            int iw = src.getWidth(), ih = src.getHeight(), s = Math.min(iw, ih);
            BufferedImage sq = src.getSubimage((iw - s) / 2, (ih - s) / 2, s, s); // centre square
            BufferedImage scaled = scaleHQ(sq, d);                                 // quality downscale
            pg.drawImage(scaled, 0, 0, null);
        } else {
            pg.setColor(GREEN2); pg.fillRect(0, 0, d, d);
            pg.setColor(CREAMTX); pg.setFont(new Font("Serif", Font.BOLD, d / 3));
            FontMetrics fm = pg.getFontMetrics();
            pg.drawString(initials, d / 2 - fm.stringWidth(initials) / 2, d / 2 + fm.getAscent() / 2 - 2);
        }
        pg.dispose();

        // antialiased circular mask
        BufferedImage mask = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mask.createGraphics();
        mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mg.setColor(java.awt.Color.WHITE); mg.fill(new Ellipse2D.Float(0, 0, d, d)); mg.dispose();
        Graphics2D pm = photo.createGraphics();
        pm.setComposite(AlphaComposite.DstIn);
        pm.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pm.drawImage(mask, 0, 0, null); pm.dispose();

        // gold ring + round photo
        BufferedImage out = new BufferedImage(tot, tot, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(GOLD); g.fill(new Ellipse2D.Float(0, 0, tot, tot));
        g.drawImage(photo, ring, ring, null); g.dispose();
        return out;
    }

    /** Light unsharp-mask sharpen so faces read crisply at small sizes. */
    private BufferedImage sharpen(BufferedImage img) {
        float c = 0.45f;                       // strength
        float[] k = { 0, -c, 0, -c, 1 + 4 * c, -c, 0, -c, 0 };   // sums to 1
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, k);
        java.awt.image.ConvolveOp op =
                new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        try { op.filter(img, out); return out; } catch (Throwable t) { return img; }
    }

    /** Multi-step bicubic downscale: halve repeatedly until near the target, then
     *  final resize. Far cleaner than a single large-ratio downscale. */
    private BufferedImage scaleHQ(BufferedImage img, int target) {
        BufferedImage cur = img;
        int w = img.getWidth(), h = img.getHeight();
        while (w > target * 2) {
            w /= 2; h /= 2;
            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = tmp.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(cur, 0, 0, w, h, null); g.dispose(); cur = tmp;
        }
        BufferedImage out = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(cur, 0, 0, target, target, null); g.dispose();
        return out;
    }
}