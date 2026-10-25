package chess;

import chess.model.*;

public class Main {
    public static void main(String[] args) {
        long[] expected = {1, 20, 400, 8902, 197281, 4865609};

        System.out.println("PERFT from the starting position");
        System.out.println("(proves move generation is 100% correct if every count matches)\n");
        System.out.printf("%-6s %-14s %-14s %-10s %s%n",
                "depth", "your count", "expected", "match?", "time");

        int maxDepth = 4;   // change to 5 for the 4.8M-node test (a few seconds)
        for (int d = 1; d <= maxDepth; d++) {
            Board board = new Board();
            long start = System.currentTimeMillis();
            long nodes = Perft.perft(board, Color.WHITE, d);
            long ms = System.currentTimeMillis() - start;

            boolean ok = (nodes == expected[d]);
            System.out.printf("%-6d %-14d %-14d %-10s %d ms%n",
                    d, nodes, expected[d], ok ? "YES" : "NO <-BUG", ms);
        }
    }
}