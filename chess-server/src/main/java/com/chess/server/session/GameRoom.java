package com.chess.server.session;

import com.chess.common.game.Board;
import com.chess.common.game.GameStatus;
import com.chess.common.model.GameColor;
import com.chess.common.model.Move;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class GameRoom {

    private static final Logger LOGGER = Logger.getLogger(GameRoom.class.getName());

    private final String roomId;
    private PlayerSession whitePlayer;
    private PlayerSession blackPlayer;
    private Board board;
    private RoomState state;
    private final List<String> moveNotations;
    private int gameId;

    public GameRoom(String roomId, PlayerSession host) {
        this.roomId = roomId;
        this.whitePlayer = host;
        this.board = new Board();
        this.state = RoomState.WAITING;
        this.moveNotations = new ArrayList<>();
    }


    public synchronized boolean addPlayer(PlayerSession player) {
        if (state != RoomState.WAITING || blackPlayer != null) {
            return false;
        }
        blackPlayer = player;
        state = RoomState.PLAYING;

        whitePlayer.setAssignedColor(GameColor.WHITE);
        blackPlayer.setAssignedColor(GameColor.BLACK);
        whitePlayer.setCurrentRoom(this);
        blackPlayer.setCurrentRoom(this);

        LOGGER.info("Game started in room " + roomId + ": " +
                whitePlayer.getUsername() + " (W) vs " + blackPlayer.getUsername() + " (B)");
        return true;
    }

    public synchronized MoveResult makeMove(PlayerSession player, Move move) {
        if (state != RoomState.PLAYING) {
            return MoveResult.failure("Game is not in progress");
        }

        GameColor expectedColor = board.getCurrentTurn();
        if (player.getAssignedColor() != expectedColor) {
            return MoveResult.failure("Not your turn");
        }

        boolean success = board.makeMove(move);
        if (!success) {
            return MoveResult.failure("Invalid move");
        }

        String notation = move.getFrom().toAlgebraic() + move.getTo().toAlgebraic();
        moveNotations.add(notation);

        GameStatus status = board.getGameStatus();
        if (status.isGameOver()) {
            state = RoomState.FINISHED;
            LOGGER.info("Game over in room " + roomId + ": " + status);
        }

        return MoveResult.success(board.toFEN(), status);
    }

    public synchronized void removePlayer(PlayerSession player) {
        player.setCurrentRoom(null);
        if (state == RoomState.PLAYING) {
            state = RoomState.FINISHED;
        }
    }

    public PlayerSession getOpponent(PlayerSession player) {
        if (player == whitePlayer) return blackPlayer;
        if (player == blackPlayer) return whitePlayer;
        return null;
    }

    public boolean hasPlayer(PlayerSession player) {
        return player == whitePlayer || player == blackPlayer;
    }

    public String getRoomId() { return roomId; }
    public PlayerSession getWhitePlayer() { return whitePlayer; }
    public PlayerSession getBlackPlayer() { return blackPlayer; }
    public Board getBoard() { return board; }
    public RoomState getState() { return state; }
    public boolean isFull() { return whitePlayer != null && blackPlayer != null; }
    public List<String> getMoveNotations() { return moveNotations; }
    public int getGameId() { return gameId; }
    public void setGameId(int gameId) { this.gameId = gameId; }

    public String toPGN() {
        StringBuilder pgn = new StringBuilder();
        pgn.append("[White \"").append(whitePlayer.getUsername()).append("\"]\n");
        pgn.append("[Black \"").append(blackPlayer != null ? blackPlayer.getUsername() : "?").append("\"]\n");
        pgn.append("[Result \"").append(getResultString()).append("\"]\n\n");

        int moveNum = 1;
        for (int i = 0; i < moveNotations.size(); i++) {
            if (i % 2 == 0) pgn.append(moveNum).append(". ");
            pgn.append(moveNotations.get(i)).append(" ");
            if (i % 2 == 1) moveNum++;
        }
        pgn.append(getResultString());
        return pgn.toString();
    }

    private String getResultString() {
        if (state != RoomState.FINISHED) return "*";
        GameStatus status = board.getGameStatus();
        if (status.isWhiteWins()) return "1-0";
        if (status.isBlackWins()) return "0-1";
        if (status.isDraw()) return "1/2-1/2";
        return "*";
    }

    @Override
    public String toString() {
        return "Room{" + roomId + ": " + whitePlayer.getUsername() +
                " vs " + (blackPlayer != null ? blackPlayer.getUsername() : "waiting") +
                " [" + state + "]}";
    }


    public static class MoveResult {
        private final boolean success;
        private final String fen;
        private final GameStatus gameStatus;
        private final String errorMessage;

        private MoveResult(boolean success, String fen, GameStatus status, String error) {
            this.success = success;
            this.fen = fen;
            this.gameStatus = status;
            this.errorMessage = error;
        }

        public static MoveResult success(String fen, GameStatus status) {
            return new MoveResult(true, fen, status, null);
        }

        public static MoveResult failure(String error) {
            return new MoveResult(false, null, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getFen() { return fen; }
        public GameStatus getGameStatus() { return gameStatus; }
        public String getErrorMessage() { return errorMessage; }
    }
}
