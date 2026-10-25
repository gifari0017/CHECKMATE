package chess.ui;
import javax.swing.*; import java.awt.*;
import java.awt.event.MouseAdapter; import java.awt.event.MouseEvent;
public class SplashScreen {
    public static void show(Runnable after){
        final JWindow w=new JWindow(); final float[] alpha={0f};
        final int[] el={0}; final boolean[] done={false};
        JPanel p=new JPanel(){
            @Override protected void paintComponent(Graphics g0){
                super.paintComponent(g0); Graphics2D g=(Graphics2D)g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int W=getWidth(),H=getHeight();
                g.setColor(new Color(0xF1,0xEC,0xDD)); g.fillRect(0,0,W,H);   // cream
                float a=Math.min(1f,alpha[0]);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a));
                g.setColor(new Color(0xCB,0xA2,0x4C));                        // gold crown
                int ks=(int)(90+30*a); g.setFont(new Font("Segoe UI Symbol",Font.PLAIN,ks));
                cen(g,"♚",W/2,H/2-40);
                g.setFont(new Font("Bookman Old Style",Font.BOLD,52)); g.setColor(new Color(0x20,0x49,0x2C)); // green
                cen(g,"CHECKMATE",W/2,H/2+60);
                g.setFont(new Font("Bookman Old Style",Font.ITALIC,20)); g.setColor(new Color(0x7F,0x90,0x79)); // sage
                cen(g,"A Chess Engine in Java",W/2,H/2+100);
            }
            private void cen(Graphics2D g,String s,int cx,int cy){ FontMetrics fm=g.getFontMetrics();
                g.drawString(s,cx-fm.stringWidth(s)/2,cy); }
        };
        p.setBackground(new Color(0xF1,0xEC,0xDD));
        w.add(p); w.setSize(640,400); w.setLocationRelativeTo(null); w.setVisible(true);
        Sound.intro();
        final javax.swing.Timer t=new javax.swing.Timer(30,null);
        final Runnable fin=()->{ if(done[0])return; done[0]=true; t.stop(); w.dispose(); after.run(); };
        t.addActionListener(e->{ el[0]+=30; alpha[0]=Math.min(1f,el[0]/650f); p.repaint();
            if(el[0]>=2300) fin.run(); });
        t.start();
        p.addMouseListener(new MouseAdapter(){ public void mousePressed(MouseEvent e){ fin.run(); } });
    }
}