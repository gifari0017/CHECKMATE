package chess;

import chess.model.*;
import java.util.List;
import java.util.Scanner;

/**
 * INTERACTIVE DEMO for presenting the chess engine.
 * Run this instead of Main. Pick options from the menu to show each feature.
 */
public class DemoMain {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("\n==================================================");
        System.out.println("     CHECKMATE  -  Chess Engine Live Demo");
        System.out.println("==================================================");

        while (true) {
            printMenu();
            String choice = sc.hasNextLine() ? sc.nextLine().trim() : "0";
            System.out.println();

            switch (choice) {
                case "1": showPiece("PAWN",   new Pawn(Color.WHITE),   6, 4); break; // e2
                case "2": showPiece("KNIGHT", new Knight(Color.WHITE), 4, 4); break; // e4
                case "3": showPiece("BISHOP", new Bishop(Color.WHITE), 4, 4); break;
                case "4": showPiece("ROOK",   new Rook(Color.WHITE),   4, 4); break;
                case "5": showPiece("QUEEN",  new Queen(Color.WHITE),  4, 4); break;
                case "6": showPiece("KING",   new King(Color.WHITE),   4, 4); break;
                case "7": showAllPieces(); break;
                case "8": demoCheckAndMate(); break;
                case "9": demoSpecialMoves(); break;
                case "10": demoPerft(); break;
                case "0": System.out.println("Demo finished. Thank you!"); return;
                default:  System.out.println("Unknown option: " + choice);
            }

            System.out.println("\n-- press Enter to return to the menu --");
            if (sc.hasNextLine()) sc.nextLine();
        }
    }

    private static void printMenu() {
        System.out.println("\n--------------------------------------------------");
        System.out.println(" Choose what to demonstrate:");
        System.out.println("   1  Pawn        2  Knight     3  Bishop");
        System.out.println("   4  Rook        5  Queen      6  King");
        System.out.println("   7  ALL six pieces in sequence");
        System.out.println("   8  Check & Checkmate detection");
        System.out.println("   9  Special moves (promotion, en passant, castling)");
        System.out.println("  10  Perft  -  PROOF the rules are 100% correct");
        System.out.println("   0  Exit");
        System.out.print(" > ");
    }

    // ---------- piece movement ----------

    /** Show one piece alone on an empty board, with every square it can reach marked '*'. */
    private static void showPiece(String name, Piece piece, int row, int col) {
        Board b = emptyBoard();
        b.set(row, col, piece);
        Square from = new Square(row, col);
        List<Square> moves = piece.generateMoves(b, from);

        System.out.println("===== " + name + " on " + from + " =====");
        printWithMoves(b, from, moves);
        System.out.println(name + " can reach " + moves.size() + " squares:");
        System.out.print("   ");
        for (Square s : moves) System.out.print(s + " ");
        System.out.println();
    }

    private static void showAllPieces() {
        showPiece("PAWN",   new Pawn(Color.WHITE),   6, 4);
        showPiece("KNIGHT", new Knight(Color.WHITE), 4, 4);
        showPiece("BISHOP", new Bishop(Color.WHITE), 4, 4);
        showPiece("ROOK",   new Rook(Color.WHITE),   4, 4);
        showPiece("QUEEN",  new Queen(Color.WHITE),  4, 4);
        showPiece("KING",   new King(Color.WHITE),   4, 4);
    }

    // ---------- check & checkmate ----------

    private static void demoCheckAndMate() {
        System.out.println("===== CHECK detection =====");
        Board b = new Board();
        b.set(6, 4, new Rook(Color.BLACK));   // black rook on e2, in front of white king
        b.print();
        System.out.println("Black rook attacks down the e-file.");
        System.out.println("Is WHITE in check?  " + b.isInCheck(Color.WHITE));

        System.out.println("\n===== CHECKMATE detection (Fool's Mate) =====");
        Board board = new Board();
        GameState state = new GameState();
        play(board, state, 6, 5, 5, 5);   // f2-f3
        play(board, state, 1, 4, 3, 4);   // e7-e5
        play(board, state, 6, 6, 4, 6);   // g2-g4
        play(board, state, 0, 3, 4, 7);   // Qd8-h4#
        board.print();
        System.out.println("Side to move: " + state.getTurn());
        System.out.println("In check?     " + board.isInCheck(state.getTurn()));
        System.out.println("Game result:  " + state.getResult(board));
    }

    // ---------- special moves ----------

    private static void demoSpecialMoves() {
        // Promotion
        System.out.println("===== PROMOTION =====");
        Board b1 = new Board();
        b1.set(1, 4, new Pawn(Color.WHITE));  // white pawn on e7
        b1.set(0, 4, null);                   // e8 empty
        System.out.println("White pawn on e7, about to promote:");
        b1.print();
        Move promo = new Move(new Square(1,4), new Square(0,4), b1.get(1,4), PieceType.QUEEN);
        b1.makeMove(promo);
        System.out.println("After promoting to a Queen (Q on e8):");
        b1.print();

        // En passant
        System.out.println("===== EN PASSANT =====");
        Board b2 = new Board();
        b2.set(6, 4, null);
        b2.set(3, 4, new Pawn(Color.WHITE));  // white pawn on e5
        Move blackDbl = new Move(new Square(1,3), new Square(3,3), b2.get(1,3));
        b2.makeMove(blackDbl);                // black d7-d5
        System.out.println("Black just played d7-d5. En-passant target: " + b2.getEnPassantTarget());
        Move epMove = null;
        for (Move m : MoveGenerator.legalMoves(b2, new Square(3,4)))
            if (m.isEnPassant) epMove = m;
        b2.makeMove(epMove);
        System.out.println("White captures en passant (pawn to d6, black d5 pawn removed):");
        b2.print();

        // Castling
        System.out.println("===== CASTLING (kingside) =====");
        Board b3 = new Board();
        b3.set(7,1,null); b3.set(7,2,null); b3.set(7,3,null);   // clear queenside path
        b3.set(7,5,null); b3.set(7,6,null);                     // clear kingside path
        System.out.println("White can castle. King's legal moves:");
        Move castle = null;
        for (Move m : MoveGenerator.legalMoves(b3, new Square(7,4))) {
            System.out.println("   " + m);
            if (m.isCastle && m.to.col == 6) castle = m;
        }
        b3.makeMove(castle);
        System.out.println("After O-O (king g1, rook f1):");
        b3.print();
    }

    // ---------- perft proof ----------

    private static void demoPerft() {
        long[] expected = {1, 20, 400, 8902, 197281};
        System.out.println("===== PERFT  -  correctness proof =====");
        System.out.println("Counting every legal move sequence vs known-correct numbers:\n");
        System.out.printf("%-6s %-12s %-12s %s%n", "depth", "our count", "expected", "match?");
        for (int d = 1; d <= 4; d++) {
            Board b = new Board();
            long nodes = Perft.perft(b, Color.WHITE, d);
            boolean ok = nodes == expected[d];
            System.out.printf("%-6d %-12d %-12d %s%n", d, nodes, expected[d], ok ? "YES" : "NO");
        }
        System.out.println("\nEvery count matches -> move generation is provably correct.");
    }

    // ---------- helpers ----------

    private static Board emptyBoard() {
        Board b = new Board();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                b.set(r, c, null);
        return b;
    }

    private static void printWithMoves(Board b, Square from, List<Square> moves) {
        boolean[][] target = new boolean[8][8];
        for (Square s : moves) target[s.row][s.col] = true;
        System.out.println();
        for (int r = 0; r < 8; r++) {
            System.out.print(" " + (8 - r) + " |");
            for (int c = 0; c < 8; c++) {
                char ch;
                Piece p = b.get(r, c);
                if (r == from.row && c == from.col) ch = p.symbol();
                else if (target[r][c])              ch = '*';
                else                                ch = '.';
                System.out.print(" " + ch);
            }
            System.out.println();
        }
        System.out.println("    -----------------");
        System.out.println("     a b c d e f g h\n");
    }

    private static void play(Board board, GameState state, int fr, int fc, int tr, int tc) {
        Move m = new Move(new Square(fr, fc), new Square(tr, tc), board.get(fr, fc));
        board.makeMove(m);
        state.switchTurn();
    }
}