package com.chess.common.game;

public enum GameStatus {
    IN_PROGRESS,
    WHITE_WINS,
    BLACK_WINS,
    DRAW_STALEMATE,
    DRAW_AGREEMENT,
    DRAW_FIFTY_MOVE,
    DRAW_THREEFOLD_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL,
    WHITE_WINS_RESIGN,
    BLACK_WINS_RESIGN,
    WHITE_WINS_TIMEOUT,
    BLACK_WINS_TIMEOUT,
    WHITE_WINS_DISCONNECT,
    BLACK_WINS_DISCONNECT;

    public boolean isGameOver() {
        return this != IN_PROGRESS;
    }

    public boolean isDraw() {
        return this.name().startsWith("DRAW");
    }

    public boolean isWhiteWins() {
        return this == WHITE_WINS || this == WHITE_WINS_RESIGN || this == WHITE_WINS_TIMEOUT || this == WHITE_WINS_DISCONNECT;
    }

    public boolean isBlackWins() {
        return this == BLACK_WINS || this == BLACK_WINS_RESIGN || this == BLACK_WINS_TIMEOUT || this == BLACK_WINS_DISCONNECT;
    }
}
