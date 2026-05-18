package com.chess.common.model;

/**
 * Represents a player/piece color in chess.
 */
public enum GameColor {
    WHITE,
    BLACK;

    /**
     * Returns the opposite color.
     *
     * @return the opposing color
     */
    public GameColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /**
     * Returns the FEN character for this color's turn indicator.
     *
     * @return 'w' for WHITE, 'b' for BLACK
     */
    public char toFenChar() {
        return this == WHITE ? 'w' : 'b';
    }

    /**
     * Parses a FEN turn indicator character.
     *
     * @param c 'w' or 'b'
     * @return the corresponding GameColor
     */
    public static GameColor fromFenChar(char c) {
        return c == 'w' ? WHITE : BLACK;
    }
}
