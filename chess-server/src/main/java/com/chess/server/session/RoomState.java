package com.chess.server.session;

/**
 * Represents the state of a game room.
 */
public enum RoomState {
    WAITING,    // One player waiting for opponent
    PLAYING,    // Game in progress
    FINISHED    // Game over
}
