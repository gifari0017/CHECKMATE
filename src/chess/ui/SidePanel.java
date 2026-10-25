package chess.ui;
import javax.swing.*; import java.awt.*; import java.util.List;
public class SidePanel extends JPanel {
    private final BoardPanel bp;
    private final CapturedPanel captured;
    private final JScrollPane movesScroll;
    private final JTextArea moves;
    public SidePanel(BoardPanel b){
        bp=b; setLayout(new BorderLayout());
        setPreferredSize(new Dimension(260,600));
        setBackground(new java.awt.Color(0x17,0x38,0x1F));
        captured=new CapturedPanel(b); add(captured,BorderLayout.NORTH);
        moves=new JTextArea(); moves.setEditable(false);
        moves.setFont(new Font("Monospaced",Font.BOLD,15));
        moves.setBackground(new java.awt.Color(0x12,0x2B,0x18));           // deep scoreboard green
        moves.setForeground(new java.awt.Color(0xEF,0xEA,0xDB));           // cream text
        moves.setCaretColor(new java.awt.Color(0xEF,0xEA,0xDB));
        moves.setMargin(new Insets(10,12,10,10));
        movesScroll=new JScrollPane(moves);
        javax.swing.border.TitledBorder tb=BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0x2E,0x5A,0x39),1), "  MOVES  ");
        tb.setTitleColor(new java.awt.Color(0xCB,0xA2,0x4C));              // gold label
        tb.setTitleFont(new Font("SansSerif",Font.BOLD,12));
        movesScroll.setBorder(tb);
        movesScroll.getViewport().setBackground(new java.awt.Color(0x12,0x2B,0x18));
        add(movesScroll,BorderLayout.CENTER);
        captured.setVisible(false); movesScroll.setVisible(false);
    }
    public void setCapturedVisible(boolean v){ captured.setVisible(v); revalidate(); repaint(); }
    public void setMovesVisible(boolean v){ movesScroll.setVisible(v); revalidate(); repaint(); }
    public boolean anyVisible(){ return captured.isVisible()||movesScroll.isVisible(); }
    public void refresh(){
        List<String> san=bp.getSanList(); StringBuilder sb=new StringBuilder();
        for(int i=0;i<san.size();i+=2){ int no=i/2+1; sb.append(no).append(". ").append(san.get(i));
            if(i+1<san.size()) sb.append("  ").append(san.get(i+1)); sb.append("\n"); }
        moves.setText(sb.toString()); captured.repaint();
    }
}