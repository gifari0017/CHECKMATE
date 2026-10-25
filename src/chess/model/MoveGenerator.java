package chess.model;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {

    private static List<Move> candidateMoves(Board board, Square from) {
        List<Move> result = new ArrayList<>();
        Piece piece = board.get(from.row, from.col);
        if (piece == null) return result;

        boolean isPawn = piece.getType() == PieceType.PAWN;
        Square ep = board.getEnPassantTarget();

        for (Square to : piece.generateMoves(board, from)) {
            boolean reachesEnd = isPawn && (to.row == 0 || to.row == 7);
            boolean isEp = isPawn && ep != null && to.row == ep.row && to.col == ep.col;

            if (reachesEnd) {
                result.add(new Move(from, to, piece, PieceType.QUEEN));
                result.add(new Move(from, to, piece, PieceType.ROOK));
                result.add(new Move(from, to, piece, PieceType.BISHOP));
                result.add(new Move(from, to, piece, PieceType.KNIGHT));
            } else {
                Move m = new Move(from, to, piece);
                m.isEnPassant = isEp;
                result.add(m);
            }
        }

        if (piece.getType() == PieceType.KING) {
            addCastlingMoves(board, from, piece, result);
        }
        return result;
    }

    private static void addCastlingMoves(Board board, Square from, Piece king, List<Move> result) {
        Color me = king.getColor();
        int row = (me == Color.WHITE) ? 7 : 0;

        if (from.row != row || from.col != 4) return;
        if (board.isInCheck(me)) return;

        Color enemy = me.opposite();

        if (board.canCastleKingside(me)
                && board.get(row, 5) == null && board.get(row, 6) == null
                && !board.isSquareAttacked(row, 5, enemy)
                && !board.isSquareAttacked(row, 6, enemy)) {
            Move m = new Move(from, new Square(row, 6), king);
            m.isCastle = true;
            m.rookFrom = new Square(row, 7);
            m.rookTo   = new Square(row, 5);
            result.add(m);
        }

        if (board.canCastleQueenside(me)
                && board.get(row, 1) == null && board.get(row, 2) == null && board.get(row, 3) == null
                && !board.isSquareAttacked(row, 3, enemy)
                && !board.isSquareAttacked(row, 2, enemy)) {
            Move m = new Move(from, new Square(row, 2), king);
            m.isCastle = true;
            m.rookFrom = new Square(row, 0);
            m.rookTo   = new Square(row, 3);
            result.add(m);
        }
    }

    public static List<Move> legalMoves(Board board, Square from) {
        List<Move> legal = new ArrayList<>();
        Piece piece = board.get(from.row, from.col);
        if (piece == null) return legal;
        Color me = piece.getColor();

        for (Move move : candidateMoves(board, from)) {
            board.makeMove(move);
            boolean leavesMeInCheck = board.isInCheck(me);
            board.unmakeMove(move);
            if (!leavesMeInCheck) legal.add(move);
        }
        return legal;
    }

    public static List<Move> allLegalMoves(Board board, Color color) {
        List<Move> all = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.get(r, c);
                if (p != null && p.getColor() == color) {
                    all.addAll(legalMoves(board, new Square(r, c)));
                }
            }
        }
        return all;
    }
}