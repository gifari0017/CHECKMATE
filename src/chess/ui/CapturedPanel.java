package chess.ui;
import chess.model.Move; import chess.model.Piece;
import javax.swing.*; import java.awt.*; import java.util.List;
public class CapturedPanel extends JPanel {
    private final BoardPanel bp; private static final int SZ=28;
    public CapturedPanel(BoardPanel b){ bp=b; setPreferredSize(new Dimension(260,80));
        setBackground(new java.awt.Color(0x17,0x38,0x1F)); }
    @Override protected void paintComponent(Graphics g0){
        super.paintComponent(g0); Graphics2D g=(Graphics2D)g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0xCB,0xA2,0x4C)); g.setFont(new Font("SansSerif",Font.BOLD,11));
        g.drawString("Captured by White:",6,14); g.drawString("Captured by Black:",6,54);
        List<Move> h=bp.getHistory(); int wx=6,bx=6;
        for(int i=h.size()-1;i>=0;i--){ Move m=h.get(i); Piece cap=m.capturedPiece; if(cap==null)continue;
            Image img=PieceImages.forPiece(cap);
            if(cap.getColor()==chess.model.Color.BLACK){ if(img!=null) g.drawImage(img,wx,18,SZ,SZ,this); wx+=SZ; }
            else { if(img!=null) g.drawImage(img,bx,58,SZ,SZ,this); bx+=SZ; } }
    }
}