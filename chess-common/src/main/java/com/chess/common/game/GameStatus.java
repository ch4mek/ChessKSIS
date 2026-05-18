package com.chess.common.game;

/**
 * Represents the current status of a chess game.
 */
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
    BLACK_WINS_TIMEOUT;

    /**
     * @return true if the game is over (any terminal state)
     */
    public boolean isGameOver() {
        return this != IN_PROGRESS;
    }

    /**
     * @return true if this status represents a draw
     */
    public boolean isDraw() {
        return this.name().startsWith("DRAW");
    }

    /**
     * @return true if white wins
     */
    public boolean isWhiteWins() {
        return this == WHITE_WINS || this == WHITE_WINS_RESIGN || this == WHITE_WINS_TIMEOUT;
    }

    /**
     * @return true if black wins
     */
    public boolean isBlackWins() {
        return this == BLACK_WINS || this == BLACK_WINS_RESIGN || this == BLACK_WINS_TIMEOUT;
    }
}
