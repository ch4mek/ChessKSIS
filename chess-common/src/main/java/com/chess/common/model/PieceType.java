package com.chess.common.model;

public enum PieceType {
    PAWN('P', 'p'),
    KNIGHT('N', 'n'),
    BISHOP('B', 'b'),
    ROOK('R', 'r'),
    QUEEN('Q', 'q'),
    KING('K', 'k');

    private final char whiteChar;
    private final char blackChar;

    PieceType(char whiteChar, char blackChar) {
        this.whiteChar = whiteChar;
        this.blackChar = blackChar;
    }

    public char toFenChar(GameColor color) {
        return color == GameColor.WHITE ? whiteChar : blackChar;
    }

    public static PieceType fromFenChar(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'P' -> PAWN;
            case 'N' -> KNIGHT;
            case 'B' -> BISHOP;
            case 'R' -> ROOK;
            case 'Q' -> QUEEN;
            case 'K' -> KING;
            default -> null;
        };
    }

    public static GameColor colorFromFenChar(char c) {
        return Character.isUpperCase(c) ? GameColor.WHITE : GameColor.BLACK;
    }
}
