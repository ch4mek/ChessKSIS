package com.chess.common.protocol;

/**
 * Enumeration of all message types in the ChessKSiS network protocol.
 * <p>
 * Messages are text-based, pipe-delimited, terminated with newline (\n).
 * Format: TYPE|param1|param2|...|paramN\n
 * <p>
 * Direction indicators:
 * C→S = Client to Server
 * S→C = Server to Client
 */
public enum MessageType {

    // === Authentication (C→S) ===
    REGISTER,           // REGISTER|username|password
    AUTH,               // AUTH|username|password

    // === Authentication responses (S→C) ===
    AUTH_OK,            // AUTH_OK|username|rating
    AUTH_FAIL,          // AUTH_FAIL|reason
    REGISTER_OK,        // REGISTER_OK
    REGISTER_FAIL,      // REGISTER_FAIL|reason

    // === Room management (C→S) ===
    CREATE_ROOM,        // CREATE_ROOM
    LIST_ROOMS,         // LIST_ROOMS
    JOIN_ROOM,          // JOIN_ROOM|roomId
    LEAVE_ROOM,         // LEAVE_ROOM

    // === Room management responses (S→C) ===
    ROOM_CREATED,       // ROOM_CREATED|roomId
    ROOM_LIST,          // ROOM_LIST|roomId:host:status;roomId:host:status;...
    ROOM_JOINED,        // ROOM_JOINED|roomId|color
    ROOM_JOIN_FAIL,     // ROOM_JOIN_FAIL|reason

    // === Game flow (S→C) ===
    GAME_START,         // GAME_START|whitePlayer|blackPlayer
    BOARD_STATE,        // BOARD_STATE|fen

    // === Moves (C→S) ===
    MOVE,               // MOVE|from|to           (e.g. MOVE|e2|e4)
    PROMOTE,            // PROMOTE|pieceType       (e.g. PROMOTE|Q)

    // === Move responses (S→C) ===
    MOVE_OK,            // MOVE_OK|from|to|fen
    MOVE_INVALID,       // MOVE_INVALID|reason
    OPPONENT_MOVE,      // OPPONENT_MOVE|from|to|fen
    PROMOTE_REQUEST,    // PROMOTE_REQUEST

    // === Game end (S→C) ===
    GAME_OVER,          // GAME_OVER|result|reason|ratingChange

    // === Draw (C→S) ===
    OFFER_DRAW,         // OFFER_DRAW
    ACCEPT_DRAW,        // ACCEPT_DRAW
    DECLINE_DRAW,       // DECLINE_DRAW

    // === Draw (S→C) ===
    DRAW_OFFERED,       // DRAW_OFFERED
    DRAW_DECLINED,      // DRAW_DECLINED — opponent declined the draw offer

    // === Resign (C→S) ===
    RESIGN,             // RESIGN

    // === Chat (bidirectional) ===
    CHAT,               // C→S: CHAT|message
    CHAT_MSG,           // S→C: CHAT_MSG|sender|message

    // === Disconnect (C→S) ===
    DISCONNECT,         // DISCONNECT

    // === Opponent events (S→C) ===
    OPPONENT_DISCONNECTED, // OPPONENT_DISCONNECTED

    // === Keepalive ===
    PING,               // C→S: PING
    PONG,               // S→C: PONG

    // === Errors (S→C) ===
    ERROR               // ERROR|message
}
