package com.chess.server.db;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GameDAO {

    private static final Logger LOGGER = Logger.getLogger(GameDAO.class.getName());

    private final DatabaseManager dbManager;

    public GameDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }


    public int saveGame(int whitePlayerId, int blackPlayerId) {
        synchronized (dbManager) {
            String sql = "INSERT INTO games (white_player_id, black_player_id) VALUES (?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, whitePlayerId);
                stmt.setInt(2, blackPlayerId);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error saving game", e);
            }
            return -1;
        }
    }


    public void updateGameResult(int gameId, String result, String reason, String pgn) {
        synchronized (dbManager) {
            String sql = "UPDATE games SET result = ?, reason = ?, pgn = ?, finished_at = NOW() WHERE id = ?";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
                stmt.setString(1, result);
                stmt.setString(2, reason);
                stmt.setString(3, pgn);
                stmt.setInt(4, gameId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error updating game result", e);
            }
        }
    }


    public void saveMove(int gameId, int moveNumber, String fromSquare, String toSquare,
                         String pieceType, boolean isCapture, boolean isCheck, String notation) {
        synchronized (dbManager) {
            String sql = "INSERT INTO moves (game_id, move_number, from_square, to_square, piece_type, is_capture, is_check, notation) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
                stmt.setInt(1, gameId);
                stmt.setInt(2, moveNumber);
                stmt.setString(3, fromSquare);
                stmt.setString(4, toSquare);
                stmt.setString(5, pieceType);
                stmt.setBoolean(6, isCapture);
                stmt.setBoolean(7, isCheck);
                stmt.setString(8, notation);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error saving move", e);
            }
        }
    }
}
