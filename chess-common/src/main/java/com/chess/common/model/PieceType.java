package com.chess.common.model;

/**
 * Represents a type of chess piece.
 */
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

    /**
     * Returns the FEN character for this piece type with the given color.
     *
     * @param color the piece color
     * @return uppercase for WHITE, lowercase for BLACK
     */
    public char toFenChar(GameColor color) {
        return color == GameColor.WHITE ? whiteChar : blackChar;
    }

    /**
     * Parses a FEN character into a PieceType.
     *
     * @param c the FEN character (case-insensitive for type)
     * @return the corresponding PieceType, or null if not a valid piece character
     */
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

    /**
     * Determines the color from a FEN character.
     *
     * @param c the FEN character
     * @return WHITE for uppercase, BLACK for lowercase
     */
    public static GameColor colorFromFenChar(char c) {
        return Character.isUpperCase(c) ? GameColor.WHITE : GameColor.BLACK;
    }
}
