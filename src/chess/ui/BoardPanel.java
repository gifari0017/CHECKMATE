package chess.ui;

import chess.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Interactive, animated chess board.
 *  - bigger auto-sizing board, smooth animated flip + sounds + check toast
 *  - NEW: optional chess clock (per-player timer), redo stack, status line
 */
public class BoardPanel extends JPanel {

    private static final int SQUARE = computeSquare();
    private static final int W = SQUARE * 8;
    private static final int STATUS_H = 42;                 // status/clock strip at top
    private static int computeSquare() {
        try { int h=java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
            int s=(h-260)/8; return Math.max(60, Math.min(84, s)); }
        catch (Throwable t) { return 78; }
    }

    private static final java.awt.Color LIGHT    = new java.awt.Color(0xEB, 0xEC, 0xD0); // board light
    private static final java.awt.Color DARK     = new java.awt.Color(0x77, 0x95, 0x56); // board dark
    private static final java.awt.Color BG       = new java.awt.Color(0xF1, 0xEC, 0xDD); // cream
    private static final java.awt.Color PANEL    = new java.awt.Color(0x20, 0x49, 0x2C); // forest green
    private static final java.awt.Color GREEN2   = new java.awt.Color(0x17, 0x38, 0x1F); // deep green
    private static final java.awt.Color CREAMTX  = new java.awt.Color(0xEF, 0xEA, 0xDB); // cream text on green
    private static final java.awt.Color INK      = new java.awt.Color(0x20, 0x33, 0x1E); // dark text on cream
    private static final java.awt.Color GOLD      = new java.awt.Color(0xCB, 0xA2, 0x4C);
    private static final java.awt.Color SELECT   = new java.awt.Color(0xF6, 0xF6, 0x69, 170);
    private static final java.awt.Color LASTMOVE = new java.awt.Color(0xF6, 0xF6, 0x69, 90);
    private static final java.awt.Color DOT      = new java.awt.Color(0x20, 0x20, 0x20, 90);
    private static final java.awt.Color CHECKRED = new java.awt.Color(0xE0, 0x4F, 0x4F, 200);
    private static final java.awt.Color LOWTIME  = new java.awt.Color(0xF2, 0x8B, 0x82);

    private Board board = new Board();
    private GameState state = new GameState();

    private Square selected = null;
    private List<Move> selectedMoves = null;
    private boolean flipped = false;
    private boolean gameOver = false;

    private final Deque<Move> history = new ArrayDeque<>();
    private final Deque<Move> redoStack = new ArrayDeque<>();   // NEW: redo
    private final List<String> sanList = new ArrayList<>();
    private Runnable onChange = () -> {};

    // flip animation
    private boolean animating = false;
    private double animT = 0;
    private boolean targetFlip = false, swapped = false;
    private javax.swing.Timer flipTimer;

    // toast
    private String toastMsg = null;
    private float toastAlpha = 0f;
    private javax.swing.Timer toastTimer;

    // NEW: chess clock
    private ChessClock clock = new ChessClock(0);   // 0 = off by default

    // NEW: AI opponent (Week 7)
    private boolean humanVsAi = false;                 // false = local 2-player
    private chess.model.Color humanColor = chess.model.Color.WHITE;
    private int aiDepth = 3;
    private final chess.engine.SearchEngine ai = new chess.engine.SearchEngine();
    private boolean aiThinking = false;

    // NEW: pause
    private boolean paused = false;
    private JPanel pauseCard;

    // NEW: LAN multiplayer (Week 8)
    private boolean netMode = false;
    private chess.net.NetLink net;
    private chess.model.Color localColor = chess.model.Color.WHITE;
    private boolean applyingRemote = false;

    // NEW: database game metadata (Week 9)
    private String gameMode = "Local";
    private String whiteName = "White", blackName = "Black";
    private boolean saved = false;         // guard against double-saving one game

    public BoardPanel() {
        setPreferredSize(new Dimension(W, W + STATUS_H));
        setBackground(BG);
        setLayout(new GridBagLayout());          // centers the pause card
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
        buildPauseCard();
        wireClock();
    }

    // ---------- pause ----------
    private void buildPauseCard() {
        pauseCard = new JPanel();
        pauseCard.setLayout(new BoxLayout(pauseCard, BoxLayout.Y_AXIS));
        pauseCard.setBackground(GREEN2);
        pauseCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2, true),
                BorderFactory.createEmptyBorder(22, 40, 22, 40)));

        JLabel icon = new JLabel("⏸");                       // pause glyph
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 40));
        icon.setForeground(GOLD);
        icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel title = new JLabel("GAME PAUSED");
        title.setFont(new Font("Serif", Font.BOLD, 28));
        title.setForeground(CREAMTX);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JButton resume = new JButton("▶  Resume");
        resume.setFont(new Font("Serif", Font.BOLD, 18));
        resume.setForeground(new java.awt.Color(0x2B, 0x24, 0x10));
        resume.setBackground(GOLD);
        resume.setOpaque(true);
        resume.setFocusPainted(false);
        resume.setAlignmentX(CENTER_ALIGNMENT);
        resume.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resume.setBorder(BorderFactory.createEmptyBorder(9, 26, 9, 26));
        resume.addActionListener(e -> resume());

        pauseCard.add(icon);
        pauseCard.add(Box.createVerticalStrut(6));
        pauseCard.add(title);
        pauseCard.add(Box.createVerticalStrut(16));
        pauseCard.add(resume);
        pauseCard.setVisible(false);
        add(pauseCard);
    }

    public boolean isPaused() { return paused; }
    /** Pause: freeze clocks, block the board, show the on-board popup. */
    public void pause() {
        if (paused || gameOver || aiThinking || history.isEmpty() || netMode) return;
        paused = true;
        clock.pause();
        pauseCard.setVisible(true);
        onChange.run();
        revalidate(); repaint();
    }
    public void resume() {
        if (!paused) return;
        paused = false;
        pauseCard.setVisible(false);
        if (clock.isEnabled() && !gameOver) clock.startFor(state.getTurn());
        onChange.run();
        revalidate(); repaint();
    }

    public void setOnChange(Runnable r) { this.onChange = r; }
    public List<String> getSanList() { return sanList; }
    public List<Move> getHistory() { return new ArrayList<>(history); }
    public ChessClock getClock() { return clock; }
    public boolean canUndo() { return !history.isEmpty() && !aiThinking && !paused && !netMode; }
    public boolean canRedo() { return !redoStack.isEmpty() && !aiThinking && !paused && !netMode; }

    // ---------- clock ----------
    private void wireClock() {
        clock.setOnTick(() -> SwingUtilities.invokeLater(this::repaint));
        clock.setOnFlag(loser -> SwingUtilities.invokeLater(() -> {
            gameOver = true;
            Sound.lose();
            String winner = (loser == chess.model.Color.WHITE) ? "Black" : "White";
            saveGame(winner, "Timeout");
            popupLater("⏱  Time out!  " + winner + " wins on time.", "Time Out");
        }));
    }

    /** Called by the menu's timer selector. minutes==0 means Off. */
    public void setTimer(int minutes) {
        clock.pause();
        clock = new ChessClock(minutes);
        wireClock();
        // start the clock for the side to move right away (counts from move 1)
        if (clock.isEnabled() && !gameOver)
            clock.startFor(state.getTurn());
        onChange.run();
        repaint();
    }

    // ---------- menu actions ----------
    public void newGame() {
        stopAnim();
        board = new Board(); state = new GameState();
        selected = null; selectedMoves = null;
        history.clear(); redoStack.clear(); sanList.clear();
        flipped = false; toastMsg = null; gameOver = false;
        paused = false; if (pauseCard != null) pauseCard.setVisible(false);
        saved = false;
        clock.reset();
        flipped = desiredFlip();
        onChange.run(); repaint();
        maybeTriggerAI();                 // if AI plays White, it moves first
    }

    // ---------- game mode (Week 7) ----------
    /** Set up a fresh game. mode: false = local 2-player, true = vs Computer. */
    public void startGame(boolean vsComputer, chess.model.Color humanSide, int depth) {
        endNet();                        // leave any LAN game first
        this.humanVsAi = vsComputer;
        this.humanColor = humanSide;
        this.aiDepth = depth;
        if (vsComputer) {
            gameMode = "vs AI";
            String lvl = (depth <= 2) ? "Easy" : (depth >= 4) ? "Hard" : "Medium";
            String comp = "Computer (" + lvl + ")";
            whiteName = (humanSide == chess.model.Color.WHITE) ? "You" : comp;
            blackName = (humanSide == chess.model.Color.WHITE) ? comp : "You";
        } else {
            gameMode = "Local"; whiteName = "White"; blackName = "Black";
        }
        newGame();
    }
    public boolean isVsComputer() { return humanVsAi; }
    public boolean isAiThinking() { return aiThinking; }

    // ---------- LAN multiplayer ----------
    /** Begin a networked game: this computer controls 'local', moves relay over 'link'. */
    public void startNetGame(chess.net.NetLink link, chess.model.Color local) {
        this.net = link;
        this.netMode = true;
        this.humanVsAi = false;
        this.localColor = local;
        gameMode = "LAN";
        whiteName = (local == chess.model.Color.WHITE) ? "You" : "Opponent";
        blackName = (local == chess.model.Color.WHITE) ? "Opponent" : "You";
        newGame();                       // fresh board; desiredFlip orients to our side
        setTimer(0);                     // no clock in LAN games (keeps the two sides simple)
        link.listen(msg -> SwingUtilities.invokeLater(() -> onNetMessage(msg)));
    }
    public boolean isNet() { return netMode; }
    public void endNet() {
        if (net != null) { net.close(); net = null; }
        netMode = false;
    }

    /** The side to move (or, online, this player) resigns. Opponent wins. */
    public void resign() {
        if (gameOver || history.isEmpty()) return;
        chess.model.Color loser;
        if (netMode) { loser = localColor; if (net != null) net.send(chess.net.Protocol.RESIGN); }
        else         { loser = state.getTurn(); }
        gameOver = true; clock.pause();
        String winner = (loser == chess.model.Color.WHITE) ? "Black" : "White";
        saveGame(winner, "Resign");
        Sound.lose();
        popupLater((netMode ? "You resigned.  " : loser + " resigned.  ") + winner + " wins.", "Resign");
        onChange.run(); repaint();
    }
    public boolean canResign() { return !history.isEmpty() && !gameOver && !paused; }

    /** Handle one line from the opponent (already marshalled onto the EDT). */
    private void onNetMessage(String msg) {
        if (msg == null) return;
        if (msg.startsWith(chess.net.Protocol.MOVE)) {
            String[] t = msg.trim().split("\\s+");
            if (t.length < 6) return;
            int fr = Integer.parseInt(t[1]), fc = Integer.parseInt(t[2]);
            int tr = Integer.parseInt(t[3]), tc = Integer.parseInt(t[4]);
            char promo = t[5].charAt(0);
            Move m = findRemoteMove(fr, fc, tr, tc, promo);
            if (m != null) { applyingRemote = true; applyMove(m); applyingRemote = false; }
        } else if (msg.equals(chess.net.Protocol.RESIGN)) {
            gameOver = true; clock.pause(); Sound.win();
            saveGame(localColor == chess.model.Color.WHITE ? "White" : "Black", "Resign");
            popupLater("Opponent resigned — you win!", "Victory");
        } else if (msg.equals(chess.net.Protocol.DISCONNECT)) {
            if (!gameOver) {
                gameOver = true; clock.pause();
                popupLater("Opponent disconnected. Game over.", "Disconnected");
            }
        }
    }
    /** Rebuild the exact Move locally from coords, so both engines stay in sync. */
    private Move findRemoteMove(int fr, int fc, int tr, int tc, char promo) {
        for (Move m : MoveGenerator.legalMoves(board, new Square(fr, fc))) {
            if (m.to.row == tr && m.to.col == tc) {
                if (m.promotion == null) { if (promo == '-') return m; }
                else if (Character.toUpperCase(m.promotion.letter) == Character.toUpperCase(promo)) return m;
            }
        }
        return null;
    }

    /** Board orientation: fixed to the human's side vs computer, auto-flip in 2-player. */
    private boolean desiredFlip() {
        if (netMode)   return localColor == chess.model.Color.BLACK;   // fixed to our side
        if (humanVsAi) return humanColor == chess.model.Color.BLACK;
        return state.getTurn() == chess.model.Color.BLACK;
    }

    /** If it's the computer's turn, think on a background thread, then play. */
    private void maybeTriggerAI() {
        if (!humanVsAi || gameOver || aiThinking || paused) return;
        if (state.getTurn() != humanColor.opposite()) return;

        aiThinking = true;
        repaint();                           // show "Computer thinking…" immediately
        final Board snapshot = board.copy(); // AI searches a COPY, never the live board
        final chess.model.Color side = humanColor.opposite();
        final int depth = aiDepth;

        new SwingWorker<Move, Void>() {
            @Override protected Move doInBackground() {
                long t0 = System.currentTimeMillis();
                Move mv = ai.bestMove(snapshot, side, depth);     // heavy work OFF the UI thread
                // human-ish pause: the AI often decides in a few ms, which looks like a
                // glitch and freezes its clock. Hold a short, slightly random moment so
                // the move feels deliberate and the clock visibly ticks down.
                long elapsed = System.currentTimeMillis() - t0;
                long think = 1200 + (long) (Math.random() * 900);  // 1.2 - 2.1 s
                if (elapsed < think) {
                    try { Thread.sleep(think - elapsed); } catch (InterruptedException ignore) {}
                }
                return mv;
            }
            @Override protected void done() {
                aiThinking = false;
                Move m;
                try { m = get(); } catch (Exception ex) { return; }
                if (m == null || gameOver) return;
                applyMove(m);                                // back ON the UI thread
            }
        }.execute();
    }

    public void undo() {
        if (history.isEmpty() || aiThinking || paused) return;
        stopAnim();
        undoOnePly();
        // vs Computer: also take back the AI's reply so it's the human's turn again
        if (humanVsAi && !history.isEmpty() && state.getTurn() != humanColor)
            undoOnePly();
        selected = null; selectedMoves = null;
        gameOver = false;
        flipped = desiredFlip();
        if (clock.isEnabled()) clock.startFor(state.getTurn());
        onChange.run(); repaint();
    }
    private void undoOnePly() {
        Move m = history.pop();
        board.unmakeMove(m);
        state.switchTurn();
        if (!sanList.isEmpty()) sanList.remove(sanList.size() - 1);
        redoStack.push(m);
    }

    public void redo() {
        if (redoStack.isEmpty() || aiThinking || paused) return;
        stopAnim();
        redoOnePly();
        // vs Computer: replay the AI's reply too, so a redo restores a full round
        if (humanVsAi && !redoStack.isEmpty() && state.getTurn() != humanColor)
            redoOnePly();
        boolean newFlip = desiredFlip();
        if (newFlip != flipped) startFlip(newFlip); else repaint();
        if (clock.isEnabled()) clock.startFor(state.getTurn());
        onChange.run();
    }
    private void redoOnePly() {
        Move m = redoStack.pop();
        boolean capture = (m.capturedPiece != null) || m.isEnPassant;
        board.makeMove(m);
        state.switchTurn();
        history.push(m);
        GameResult result = state.getResult(board);
        boolean inCheck = board.isInCheck(state.getTurn());
        boolean mate = (result == GameResult.CHECKMATE);
        if (mate || result == GameResult.STALEMATE) gameOver = true;
        sanList.add(san(m, inCheck && !mate, mate));
        if (capture) Sound.capture(); else Sound.move();
    }

    // ---------- flip mapping ----------
    private int viewRow(int r) { return flipped ? 7 - r : r; }
    private int viewCol(int c) { return flipped ? 7 - c : c; }
    private int modelRow(int v) { return flipped ? 7 - v : v; }
    private int modelCol(int v) { return flipped ? 7 - v : v; }

    // ---------- clicks ----------
    private void handleClick(int px, int py) {
        if (animating || gameOver || aiThinking || paused) return;
        if (netMode && state.getTurn() != localColor) return;   // not your turn online
        int boardY = py - STATUS_H;             // account for status strip
        if (boardY < 0) return;
        int vc = px / SQUARE, vr = boardY / SQUARE;
        if (vr < 0 || vr > 7 || vc < 0 || vc > 7) return;
        int row = modelRow(vr), col = modelCol(vc);
        Square clicked = new Square(row, col);

        if (selected == null) {
            Piece p = board.get(row, col);
            if (p != null && p.getColor() == state.getTurn()) {
                selected = clicked; selectedMoves = MoveGenerator.legalMoves(board, clicked);
            }
        } else {
            Move chosen = findMoveTo(clicked);
            if (chosen != null) { applyMove(chosen); selected = null; selectedMoves = null; }
            else {
                Piece p = board.get(row, col);
                if (p != null && p.getColor() == state.getTurn()) {
                    selected = clicked; selectedMoves = MoveGenerator.legalMoves(board, clicked);
                } else { selected = null; selectedMoves = null; }
            }
        }
        repaint();
    }

    private Move findMoveTo(Square target) {
        if (selectedMoves == null) return null;
        for (Move m : selectedMoves)
            if (m.to.row == target.row && m.to.col == target.col) {
                if (m.promotion != null) return choosePromotion(target);
                return m;
            }
        return null;
    }
    private Move choosePromotion(Square target) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int pick = JOptionPane.showOptionDialog(this, "Promote your pawn to:", "Promotion!",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (pick < 0) pick = 0;
        PieceType[] types = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
        for (Move m : selectedMoves)
            if (m.to.row == target.row && m.to.col == target.col && m.promotion == types[pick]) return m;
        return null;
    }

    private void applyMove(Move m) {
        boolean capture = (m.capturedPiece != null) || m.isEnPassant;
        board.makeMove(m);
        state.switchTurn();
        history.push(m);
        redoStack.clear();                       // a new move invalidates redo

        // LAN: if WE made this move, relay it to the opponent
        if (netMode && !applyingRemote && net != null) net.send(chess.net.Protocol.move(m));

        GameResult result = state.getResult(board);
        boolean inCheck = board.isInCheck(state.getTurn());
        boolean mate = (result == GameResult.CHECKMATE);
        sanList.add(san(m, inCheck && !mate, mate));
        onChange.run();

        if (capture) Sound.capture(); else Sound.move();

        boolean newFlip = desiredFlip();
        if (newFlip != flipped) startFlip(newFlip); else repaint();

        if (result == GameResult.CHECKMATE) {
            gameOver = true; clock.pause(); Sound.win();
            String winner = (state.getTurn() == chess.model.Color.WHITE) ? "Black" : "White";
            saveGame(winner, "Checkmate");
            popupLater("♛  Checkmate!  " + winner + " wins!  ♛", "Victory");
        } else if (result == GameResult.STALEMATE) {
            gameOver = true; clock.pause(); Sound.draw();
            saveGame("Draw", "Stalemate");
            popupLater("Stalemate — it's a draw.", "Game Over");
        } else {
            if (inCheck) { Sound.check(); showToast("Check!"); }
            if (clock.isEnabled()) clock.startFor(state.getTurn());   // hand clock to next player
            maybeTriggerAI();                                         // computer's turn? let it think
        }
    }

    private String san(Move m, boolean check, boolean mate) {
        String base;
        if (m.isCastle) base = (m.to.col == 6) ? "O-O" : "O-O-O";
        else {
            boolean pawn = m.movedPiece.getType() == PieceType.PAWN;
            String pl = pawn ? "" : PieceImages.letter(m.movedPiece.getType());
            boolean cap = (m.capturedPiece != null) || m.isEnPassant;
            String capStr = cap ? ((pawn ? "" + (char) ('a' + m.from.col) : "") + "x") : "";
            String promo = (m.promotion != null) ? "=" + PieceImages.letter(m.promotion) : "";
            base = pl + capStr + m.to.toString() + promo;
        }
        if (mate) base += "#"; else if (check) base += "+";
        return base;
    }

    // ---------- flip animation ----------
    private void startFlip(boolean target) {
        Sound.swap();
        animating = true; animT = 0; targetFlip = target; swapped = false;
        if (flipTimer != null && flipTimer.isRunning()) flipTimer.stop();
        flipTimer = new javax.swing.Timer(15, e -> {
            animT += 15.0 / 320.0;
            if (animT >= 0.5 && !swapped) { flipped = targetFlip; swapped = true; }
            if (animT >= 1.0) { animT = 1; animating = false; flipTimer.stop(); }
            repaint();
        });
        flipTimer.start();
    }
    private void stopAnim() { if (flipTimer != null) flipTimer.stop(); animating = false; animT = 0; swapped = false; }

    // ---------- toast ----------
    private void showToast(String msg) {
        toastMsg = msg; toastAlpha = 1f;
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new javax.swing.Timer(40, e -> {
            toastAlpha -= 0.04f;
            if (toastAlpha <= 0) { toastAlpha = 0; toastMsg = null; toastTimer.stop(); }
            repaint();
        });
        toastTimer.start();
    }
    private void popupLater(String msg, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE));
    }

    // ---------- database save (Week 9) ----------
    private void saveGame(String result, String reason) {
        if (saved || sanList.isEmpty()) return;
        saved = true;
        chess.db.GameRecord g = new chess.db.GameRecord();
        g.playedAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        g.mode = gameMode; g.white = whiteName; g.black = blackName;
        g.result = result; g.reason = reason;
        g.moves = (sanList.size() + 1) / 2;
        g.pgn = buildPgn();
        chess.db.Database.save(g);
    }
    private String buildPgn() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sanList.size(); i += 2) {
            sb.append(i / 2 + 1).append(". ").append(sanList.get(i));
            if (i + 1 < sanList.size()) sb.append(" ").append(sanList.get(i + 1));
            sb.append("  ");
        }
        return sb.toString().trim();
    }

    // ---------- painting ----------
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawStatus(g);                           // NEW: status + clocks strip

        g.translate(0, STATUS_H);
        if (animating) {
            BufferedImage buf = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = buf.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawBoard(bg);
            bg.dispose();
            double sx = Math.abs(Math.cos(animT * Math.PI));
            int bw = Math.max(1, (int) (W * sx));
            g.setColor(BG); g.fillRect(0, 0, W, W);
            g.drawImage(buf, (W - bw) / 2, 0, bw, W, this);
        } else {
            drawBoard(g);
        }
        if (paused) {                               // dim the frozen board behind the popup
            g.setColor(new java.awt.Color(0x12, 0x2B, 0x18, 180));
            g.fillRect(0, 0, W, W);
        }
        drawToast(g);
        g.translate(0, -STATUS_H);
    }

    /** Top strip: white clock far left, black clock far right,
     *  whose-turn pill centered (pill color follows the side to move). */
    private void drawStatus(Graphics2D g) {
        g.setColor(PANEL);
        g.fillRect(0, 0, W, STATUS_H);

        // clocks pinned to the two corners
        if (clock.isEnabled()) {
            drawClock(g, chess.model.Color.WHITE, 12, true);     // left corner
            drawClock(g, chess.model.Color.BLACK, W - 12, false); // right corner
        }

        // whose-turn pill, centered, colored by side
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        String turnTxt = gameOver ? "Game over"
                : aiThinking ? "Computer thinking…"
                : (state.getTurn() + " to move");
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(turnTxt);
        int px = W / 2 - tw / 2;
        boolean whiteTurn = (state.getTurn() == chess.model.Color.WHITE);
        java.awt.Color pill = gameOver ? GREEN2
                : (whiteTurn ? new java.awt.Color(0xEF, 0xEA, 0xDB)   // cream = white
                : GREEN2);                                // deep green = black
        g.setColor(pill);
        g.fillRoundRect(px - 14, 6, tw + 28, STATUS_H - 12, 12, 12);
        g.setColor((whiteTurn && !gameOver) ? INK : CREAMTX);
        g.drawString(turnTxt, px, STATUS_H / 2 + 5);
    }

    /** anchorLeft: x is the left edge. !anchorLeft: x is the right edge. */
    private void drawClock(Graphics2D g, chess.model.Color c, int x, boolean anchorLeft) {
        long ms = clock.getMillis(c);
        boolean active = (!gameOver && state.getTurn() == c);
        boolean low = ms <= 30_000;
        String label = (c == chess.model.Color.WHITE ? "White " : "Black ") + ChessClock.format(ms);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(label);
        int left = anchorLeft ? x : x - w;
        int y = STATUS_H / 2 + 6;
        if (active) {
            g.setColor(new java.awt.Color(0x2B, 0x56, 0x36));
            g.fillRoundRect(left - 8, 6, w + 16, STATUS_H - 12, 10, 10);
        }
        g.setColor(low ? LOWTIME : (active ? GOLD : CREAMTX));
        g.drawString(label, left, y);
    }

    private void drawBoard(Graphics2D g) {
        Move last = history.peek();
        Square checkedKing = board.isInCheck(state.getTurn()) ? board.findKing(state.getTurn()) : null;

        for (int vr = 0; vr < 8; vr++)
            for (int vc = 0; vc < 8; vc++) {
                int row = modelRow(vr), col = modelCol(vc);
                int x = vc * SQUARE, y = vr * SQUARE;
                g.setColor(((row + col) % 2 == 0) ? LIGHT : DARK);
                g.fillRect(x, y, SQUARE, SQUARE);
                if (last != null && onSquare(last, row, col)) { g.setColor(LASTMOVE); g.fillRect(x, y, SQUARE, SQUARE); }
                if (checkedKing != null && checkedKing.row == row && checkedKing.col == col) { g.setColor(CHECKRED); g.fillRect(x, y, SQUARE, SQUARE); }
                if (selected != null && selected.row == row && selected.col == col) { g.setColor(SELECT); g.fillRect(x, y, SQUARE, SQUARE); }
                drawCoord(g, vr, vc, row, col, x, y);
                Piece p = board.get(row, col);
                if (p != null) drawPiece(g, p, x, y);
            }

        if (selectedMoves != null) {
            g.setColor(DOT);
            for (Move m : selectedMoves) {
                int x = viewCol(m.to.col) * SQUARE + SQUARE / 2;
                int y = viewRow(m.to.row) * SQUARE + SQUARE / 2;
                int r = SQUARE / 6;
                g.fillOval(x - r, y - r, r * 2, r * 2);
            }
        }
    }

    private void drawToast(Graphics2D g) {
        if (toastMsg == null || toastAlpha <= 0) return;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, toastAlpha)));
        g.setFont(new Font("Bookman Old Style", Font.BOLD, 32));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(toastMsg), th = fm.getHeight();
        int bw = tw + 60, bh = th + 24;
        int bx = (W - bw) / 2, by = (W - bh) / 2;
        g.setColor(new java.awt.Color(0x0E, 0x15, 0x26)); g.fillRoundRect(bx, by, bw, bh, 20, 20);
        g.setColor(GOLD); g.drawRoundRect(bx, by, bw, bh, 20, 20);
        g.setColor(java.awt.Color.WHITE); g.drawString(toastMsg, (W - tw) / 2, by + bh - 18);
        g.setComposite(old);
    }

    private boolean onSquare(Move m, int row, int col) {
        return (m.from.row == row && m.from.col == col) || (m.to.row == row && m.to.col == col);
    }
    private void drawCoord(Graphics2D g, int vr, int vc, int row, int col, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(((row + col) % 2 == 0) ? DARK : LIGHT);
        if (vr == 7) g.drawString("" + (char) ('a' + col), x + SQUARE - 14, y + SQUARE - 5);
        if (vc == 0) g.drawString("" + (8 - row), x + 4, y + 15);
    }
    private void drawPiece(Graphics2D g, Piece p, int x, int y) {
        Image img = PieceImages.forPiece(p);
        if (img != null) g.drawImage(img, x + 7, y + 7, SQUARE - 14, SQUARE - 14, this);
        else {
            g.setColor(p.getColor() == chess.model.Color.WHITE ? java.awt.Color.WHITE : java.awt.Color.BLACK);
            g.setFont(new Font("Dialog", Font.PLAIN, (int) (SQUARE * 0.8)));
            String ch = PieceImages.glyph(p.getType());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ch, x + (SQUARE - fm.stringWidth(ch)) / 2, y + (SQUARE - fm.getHeight()) / 2 + fm.getAscent());
        }
    }
}