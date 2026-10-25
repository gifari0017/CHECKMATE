package chess.ui;

import chess.model.Color;
import javax.swing.*;
import java.awt.*;

/**
 * Main window. Opens on a START MENU (mode -> level), then shows the board.
 * Bottom bar has New Game (back to menu), Undo, Redo, a Timer selector, Move
 * List / Captured toggles, and Quit. New Game / Quit confirm when mid-game.
 */
public class GameFrame extends JFrame {

    private static final java.awt.Color BG     = new java.awt.Color(0xF1, 0xEC, 0xDD); // cream
    private static final java.awt.Color BAR    = new java.awt.Color(0x20, 0x49, 0x2C); // forest green
    private static final java.awt.Color GREEN2 = new java.awt.Color(0x17, 0x38, 0x1F); // deep green
    private static final java.awt.Color BTN    = new java.awt.Color(0x2B, 0x56, 0x36); // button on green
    private static final java.awt.Color CREAMTX= new java.awt.Color(0xEF, 0xEA, 0xDB); // cream text
    private static final java.awt.Color GOLD   = new java.awt.Color(0xCB, 0xA2, 0x4C);
    private static final String SF = "Segoe UI Symbol";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final BoardPanel board;
    private final SidePanel side;
    private final StartMenu menu;
    private JPanel actionBar;
    private JButton handle;
    private JButton undoBtn, redoBtn, pauseBtn, resignBtn;
    private static final java.awt.Color DIMBTN = new java.awt.Color(0x2A, 0x3E, 0x31); // faded green
    private static final java.awt.Color DIMTX  = new java.awt.Color(0x6C, 0x80, 0x71); // dim text

    public GameFrame() {
        super("CHECKMATE - Chess");

        board = new BoardPanel();
        side = new SidePanel(board);
        board.setOnChange(this::onBoardChange);

        menu = new StartMenu(this::startGame, new StartMenu.NetListener() {
            @Override public void onHost(Color c)   { hostGame(c); }
            @Override public void onJoin(String ip) { joinGame(ip); }
        }, this::showHistory);

        root.setBackground(BG);
        root.add(menu, "menu");
        root.add(buildGamePanel(), "game");

        setContentPane(root);
        cards.show(root, "menu");

        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);            // we confirm ourselves
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { tryQuit(); }
        });
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /** Called by the start menu once the player picks a mode/level/time. */
    private void startGame(boolean vsComputer, Color humanSide, int depth, int minutes) {
        board.startGame(vsComputer, humanSide, depth);
        board.setTimer(minutes);                 // time chosen on the menu / popup
        cards.show(root, "game");
        pack();
        setLocationRelativeTo(null);
    }

    /** Fired after every board change: refresh side panel + grey out dead nav buttons. */
    private void onBoardChange() {
        side.refresh();
        setNav(undoBtn, board.canUndo());
        setNav(redoBtn, board.canRedo());
        setNav(pauseBtn, !board.getHistory().isEmpty() && !board.isPaused() && !board.isNet());
        setNav(resignBtn, board.canResign());
    }
    private void tryResign() {
        if (!board.canResign()) return;
        int ans = JOptionPane.showConfirmDialog(this,
                "Resign this game? Your opponent wins.",
                "Resign", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans == JOptionPane.YES_OPTION) board.resign();
    }

    // ----- LAN: host / join -----
    private void hostGame(Color hostColor) {
        final boolean[] cancelled = {false};
        JDialog dlg = netDialog("Hosting a Game",
                "Waiting for your opponent to join...",
                "Your IP:  " + chess.net.NetLink.localIp() + "        Port:  " + chess.net.Protocol.PORT,
                cancelled);
        new SwingWorker<chess.net.NetLink, Void>() {
            @Override protected chess.net.NetLink doInBackground() throws Exception {
                chess.net.NetLink link = chess.net.NetLink.host(chess.net.Protocol.PORT);
                link.send(chess.net.Protocol.START + " " + hostColor.name());
                return link;
            }
            @Override protected void done() {
                dlg.dispose();
                try {
                    chess.net.NetLink link = get();
                    if (cancelled[0]) { link.close(); return; }
                    board.startNetGame(link, hostColor);
                    showGame();
                } catch (Exception ex) {
                    if (!cancelled[0]) netError("Could not host the game.\n" + rootMsg(ex));
                }
            }
        }.execute();
        dlg.setVisible(true);                 // non-modal: returns immediately
    }

    private void joinGame(String ip) {
        final boolean[] cancelled = {false};
        JDialog dlg = netDialog("Joining a Game", "Connecting to " + ip + " ...", null, cancelled);
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                chess.net.NetLink link = chess.net.NetLink.join(ip, chess.net.Protocol.PORT);
                String hello = link.readLineBlocking();          // "START <hostColor>"
                Color hostColor = (hello != null && hello.contains("BLACK")) ? Color.BLACK : Color.WHITE;
                return new Object[]{ link, hostColor.opposite() };
            }
            @Override protected void done() {
                dlg.dispose();
                try {
                    Object[] r = get();
                    chess.net.NetLink link = (chess.net.NetLink) r[0];
                    if (cancelled[0]) { link.close(); return; }
                    board.startNetGame(link, (Color) r[1]);
                    showGame();
                } catch (Exception ex) {
                    if (!cancelled[0]) netError("Could not connect to " + ip + ".\n" + rootMsg(ex));
                }
            }
        }.execute();
        dlg.setVisible(true);
    }

    private void showGame() {
        cards.show(root, "game");
        pack();
        setLocationRelativeTo(null);
    }
    private String rootMsg(Exception ex) {
        Throwable t = (ex.getCause() != null) ? ex.getCause() : ex;
        String m = t.getMessage();
        return (m == null) ? t.getClass().getSimpleName() : m;
    }
    private void netError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Connection Failed", JOptionPane.ERROR_MESSAGE);
    }

    /** A small cream/green waiting dialog with a Cancel. Non-modal so the worker can close it. */
    private JDialog netDialog(String title, String line1, String line2, boolean[] cancelled) {
        JDialog d = new JDialog(this, title, false);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 36, 20, 36));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Serif", Font.BOLD, 22));
        t.setForeground(BAR);
        t.setAlignmentX(CENTER_ALIGNMENT);
        JLabel l1 = new JLabel(line1);
        l1.setFont(new Font("SansSerif", Font.PLAIN, 14));
        l1.setForeground(new java.awt.Color(0x3A, 0x4A, 0x3E));
        l1.setAlignmentX(CENTER_ALIGNMENT);
        p.add(t); p.add(Box.createVerticalStrut(10)); p.add(l1);
        if (line2 != null) {
            JLabel l2 = new JLabel(line2);
            l2.setFont(new Font("Monospaced", Font.BOLD, 14));
            l2.setForeground(GOLD.darker());
            l2.setAlignmentX(CENTER_ALIGNMENT);
            p.add(Box.createVerticalStrut(8)); p.add(l2);
        }
        JButton cancel = new JButton("Cancel");
        cancel.setAlignmentX(CENTER_ALIGNMENT);
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> { cancelled[0] = true; d.dispose(); });
        p.add(Box.createVerticalStrut(18)); p.add(cancel);

        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        return d;
    }
    private void setNav(JButton b, boolean on) {
        if (b == null) return;
        b.setEnabled(on);
        b.setBackground(on ? BTN : DIMBTN);
        b.setForeground(on ? CREAMTX : DIMTX);
    }
    private void styleToggle(JToggleButton t) {
        boolean on = t.isSelected();
        t.setBackground(on ? GOLD : BTN);
        t.setForeground(on ? new java.awt.Color(0x2B, 0x24, 0x10) : CREAMTX);
    }

    private JPanel buildGamePanel() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBackground(BG);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setBackground(BG);
        holder.add(board);
        gamePanel.add(holder, BorderLayout.CENTER);

        gamePanel.add(side, BorderLayout.EAST);
        side.setVisible(false);

        gamePanel.add(buildSouth(), BorderLayout.SOUTH);
        return gamePanel;
    }

    private JPanel buildSouth() {
        actionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 8));
        actionBar.setBackground(BAR);
        actionBar.add(button("⟳  New Game", e -> tryNewGame()));
        undoBtn = button("↶  Undo", e -> board.undo());
        redoBtn = button("↷  Redo", e -> board.redo());
        pauseBtn = button("⏸  Pause", e -> board.pause());
        resignBtn = button("⚑  Resign", e -> tryResign());
        actionBar.add(undoBtn);
        actionBar.add(redoBtn);
        actionBar.add(pauseBtn);
        actionBar.add(resignBtn);

        JToggleButton movesBtn = toggle("☰  Move List");
        movesBtn.addActionListener(e -> { side.setMovesVisible(movesBtn.isSelected()); styleToggle(movesBtn); updateSide(); });
        actionBar.add(movesBtn);
        JToggleButton capBtn = toggle("♟  Captured");
        capBtn.addActionListener(e -> { side.setCapturedVisible(capBtn.isSelected()); styleToggle(capBtn); updateSide(); });
        actionBar.add(capBtn);
        actionBar.add(button("✕  Quit", e -> tryQuit()));

        setNav(undoBtn, false);   // start greyed (nothing to act on yet)
        setNav(redoBtn, false);
        setNav(pauseBtn, false);
        setNav(resignBtn, false);

        handle = new JButton("▾");
        styleFlat(handle);
        handle.setToolTipText("Hide / show the menu");
        handle.addActionListener(e -> toggleBar());

        JLabel name = new JLabel("♚  CHECKMATE", SwingConstants.CENTER);
        name.setFont(new Font(SF, Font.BOLD, 20));
        name.setForeground(GOLD);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(GREEN2);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        footer.add(handle, BorderLayout.WEST);
        footer.add(name, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BAR);
        south.add(actionBar, BorderLayout.CENTER);
        south.add(footer, BorderLayout.SOUTH);
        return south;
    }

    // ----- confirmations -----
    private boolean gameInProgress() {
        return !board.getHistory().isEmpty();
    }
    /** New Game returns to the start menu so the player can re-pick mode/level. */
    private void tryNewGame() {
        if (gameInProgress()) {
            int ans = JOptionPane.showConfirmDialog(this,
                    "Return to the menu? The current game will be lost.",
                    "New Game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
        }
        board.endNet();                  // leave any LAN game
        board.newGame();
        cards.show(root, "menu");
        pack();
        setLocationRelativeTo(null);
    }
    private void tryQuit() {
        int ans = JOptionPane.showConfirmDialog(this,
                gameInProgress() ? "Quit the game? Your current game will be lost."
                        : "Quit CHECKMATE?",
                "Quit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans == JOptionPane.YES_OPTION) System.exit(0);
    }

    private void updateSide() { side.setVisible(side.anyVisible()); pack(); }
    private void toggleBar() {
        boolean show = !actionBar.isVisible();
        actionBar.setVisible(show);
        handle.setText(show ? "▾" : "▸");
        pack();
    }

    private JButton button(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text); styleFlat(b); b.addActionListener(a); return b;
    }
    private JToggleButton toggle(String text) { JToggleButton b = new JToggleButton(text); styleFlat(b); return b; }
    private void styleFlat(AbstractButton b) {
        b.setFont(new Font(SF, Font.PLAIN, 15));
        b.setFocusPainted(false);
        b.setForeground(CREAMTX);
        b.setBackground(BTN);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(6, 13, 6, 13));
    }

    // ----- Game History browser (Week 9) -----
    private void showHistory() {
        java.util.List<chess.db.GameRecord> games = chess.db.Database.list();

        String[] cols = {"Date", "Mode", "White", "Black", "Result", "Moves"};
        Object[][] data = new Object[games.size()][cols.length];
        for (int i = 0; i < games.size(); i++) {
            chess.db.GameRecord g = games.get(i);
            String res = g.result.equals("Draw") ? "Draw (" + g.reason + ")"
                    : g.result + " (" + g.reason + ")";
            data[i] = new Object[]{ g.playedAt, g.mode, g.white, g.black, res, g.moves };
        }

        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(data, cols) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.setSelectionBackground(GOLD);

        JTextArea pgn = new JTextArea(6, 40);
        pgn.setEditable(false);
        pgn.setLineWrap(true);
        pgn.setWrapStyleWord(true);
        pgn.setFont(new Font("Monospaced", Font.PLAIN, 13));
        pgn.setBackground(new java.awt.Color(0x12, 0x2B, 0x18));
        pgn.setForeground(CREAMTX);
        pgn.setText(games.isEmpty()
                ? (chess.db.Database.isReady()
                ? "No games saved yet. Finish a game and it will appear here."
                : "Database not connected. Add the sqlite-jdbc library to enable saving.")
                : "Select a game to see its moves.");
        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0 && r < games.size()) pgn.setText(games.get(r).pgn);
        });

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("  ♚  Game History");
        title.setFont(new Font("Serif", Font.BOLD, 20));
        title.setForeground(BAR);
        p.add(title, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JScrollPane pgnScroll = new JScrollPane(pgn);
        pgnScroll.setBorder(BorderFactory.createTitledBorder("Moves (PGN)"));
        p.add(pgnScroll, BorderLayout.SOUTH);

        JDialog d = new JDialog(this, "Game History", true);
        d.setContentPane(p);
        d.setSize(640, 460);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    public static void main(String[] args) {
        chess.db.Database.init();          // open / create the games database
        SwingUtilities.invokeLater(() ->
                SplashScreen.show(() -> new GameFrame().setVisible(true)));
    }
}