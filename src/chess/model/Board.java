package chess.model;
public class Board {
    private final Piece[][] grid=new Piece[8][8];
    private Square enPassantTarget=null;
    private boolean wK=true,wQ=true,bK=true,bQ=true;
    public Board(){ setup(); }
    public Piece get(int r,int c){ return grid[r][c]; }
    public void set(int r,int c,Piece p){ grid[r][c]=p; }
    /** Deep copy for the AI to search on its own thread, so the live board the
     *  screen is painting is never mutated underneath it (no flicker/glitch). */
    public Board copy(){
        Board b=new Board();
        for(int r=0;r<8;r++) for(int c=0;c<8;c++) b.grid[r][c]=this.grid[r][c];
        b.enPassantTarget=this.enPassantTarget;
        b.wK=this.wK; b.wQ=this.wQ; b.bK=this.bK; b.bQ=this.bQ;
        return b;
    }
    public Square getEnPassantTarget(){ return enPassantTarget; }
    public boolean canCastleKingside(Color c){ return c==Color.WHITE?wK:bK; }
    public boolean canCastleQueenside(Color c){ return c==Color.WHITE?wQ:bQ; }
    private boolean[] snap(){ return new boolean[]{wK,wQ,bK,bQ}; }
    private void restore(boolean[] s){ wK=s[0]; wQ=s[1]; bK=s[2]; bQ=s[3]; }
    private void clearRook(int r,int c){
        if(r==7&&c==0)wQ=false; if(r==7&&c==7)wK=false; if(r==0&&c==0)bQ=false; if(r==0&&c==7)bK=false;
    }
    public void makeMove(Move m){
        m.prevEnPassantTarget=enPassantTarget; m.prevCastling=snap();
        if(m.isCastle){
            grid[m.to.row][m.to.col]=m.movedPiece; grid[m.from.row][m.from.col]=null;
            Piece rk=grid[m.rookFrom.row][m.rookFrom.col];
            grid[m.rookTo.row][m.rookTo.col]=rk; grid[m.rookFrom.row][m.rookFrom.col]=null;
            m.capturedPiece=null;
        } else if(m.isEnPassant){
            int cr=m.from.row,cc=m.to.col; m.capturedPiece=grid[cr][cc]; grid[cr][cc]=null;
            grid[m.to.row][m.to.col]=m.movedPiece; grid[m.from.row][m.from.col]=null;
        } else {
            m.capturedPiece=grid[m.to.row][m.to.col];
            Piece land=m.movedPiece;
            if(m.promotion!=null) land=create(m.promotion,m.movedPiece.getColor());
            grid[m.to.row][m.to.col]=land; grid[m.from.row][m.from.col]=null;
        }
        enPassantTarget=null;
        if(m.movedPiece.getType()==PieceType.PAWN && Math.abs(m.to.row-m.from.row)==2)
            enPassantTarget=new Square((m.to.row+m.from.row)/2,m.from.col);
        if(m.movedPiece.getType()==PieceType.KING){
            if(m.movedPiece.getColor()==Color.WHITE){ wK=false; wQ=false; } else { bK=false; bQ=false; }
        }
        clearRook(m.from.row,m.from.col); clearRook(m.to.row,m.to.col);
    }
    public void unmakeMove(Move m){
        enPassantTarget=m.prevEnPassantTarget; restore(m.prevCastling);
        if(m.isCastle){
            grid[m.from.row][m.from.col]=m.movedPiece; grid[m.to.row][m.to.col]=null;
            Piece rk=grid[m.rookTo.row][m.rookTo.col];
            grid[m.rookFrom.row][m.rookFrom.col]=rk; grid[m.rookTo.row][m.rookTo.col]=null;
        } else if(m.isEnPassant){
            grid[m.from.row][m.from.col]=m.movedPiece; grid[m.to.row][m.to.col]=null;
            grid[m.from.row][m.to.col]=m.capturedPiece;
        } else {
            grid[m.from.row][m.from.col]=m.movedPiece; grid[m.to.row][m.to.col]=m.capturedPiece;
        }
    }
    private Piece create(PieceType t,Color c){
        switch(t){ case QUEEN:return new Queen(c); case ROOK:return new Rook(c);
            case BISHOP:return new Bishop(c); case KNIGHT:return new Knight(c);
            case PAWN:return new Pawn(c); case KING:return new King(c); } return null;
    }
    public boolean isSquareAttacked(int tr,int tc,Color by){
        for(int r=0;r<8;r++)for(int c=0;c<8;c++){ Piece p=grid[r][c];
            if(p==null||p.getColor()!=by)continue;
            if(p.getType()==PieceType.PAWN){ int dir=(by==Color.WHITE)?-1:1;
                if(r+dir==tr&&(c-1==tc||c+1==tc))return true;
            } else for(Square d:p.generateMoves(this,new Square(r,c)))
                if(d.row==tr&&d.col==tc)return true; }
        return false;
    }
    public Square findKing(Color col){
        for(int r=0;r<8;r++)for(int c=0;c<8;c++){ Piece p=grid[r][c];
            if(p!=null&&p.getType()==PieceType.KING&&p.getColor()==col) return new Square(r,c); }
        return null;
    }
    public boolean isInCheck(Color col){ Square k=findKing(col); if(k==null)return false;
        return isSquareAttacked(k.row,k.col,col.opposite()); }
    private void setup(){
        back(0,Color.BLACK); for(int c=0;c<8;c++) grid[1][c]=new Pawn(Color.BLACK);
        for(int c=0;c<8;c++) grid[6][c]=new Pawn(Color.WHITE); back(7,Color.WHITE);
    }
    private void back(int row,Color c){
        grid[row][0]=new Rook(c); grid[row][1]=new Knight(c); grid[row][2]=new Bishop(c);
        grid[row][3]=new Queen(c); grid[row][4]=new King(c); grid[row][5]=new Bishop(c);
        grid[row][6]=new Knight(c); grid[row][7]=new Rook(c);
    }
    public void print(){
        for(int r=0;r<8;r++){ System.out.print(" "+(8-r)+" |");
            for(int c=0;c<8;c++){ Piece p=grid[r][c]; System.out.print(" "+(p==null?'.':p.symbol())); }
            System.out.println(); }
        System.out.println("     a b c d e f g h");
    }
}