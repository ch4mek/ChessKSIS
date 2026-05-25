package com.chess.common.model;

public enum GameColor {
    WHITE,
    BLACK;

    public GameColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public char toFenChar() {
        return this == WHITE ? 'w' : 'b';
    }

    public static GameColor fromFenChar(char c) {
        return c == 'w' ? WHITE : BLACK;
    }
}
