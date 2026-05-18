package com.chess.server.db;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for game-related database operations.
 */
public class GameDAO {

    private static final Logger LOGGER = Logger.getLogger(GameDAO.class.getName());

    private final DatabaseManager dbManager;

    public GameDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Saves a new game record.
     *
     * @return the generated game ID, or -1 on failure
     */
    public int saveGame(int whitePlayerId, int blackPlayerId) {
        String sql = "INSERT INTO games (white_player_id, black_player_id) VALUES (?, ?)";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, whitePlayerId);
            stmt.setInt(2, blackPlayerId);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving game", e);
        }
        return -1;
    }

    /**
     * Updates a game with the final result.
     */
    public void updateGameResult(int gameId, String result, String reason, String pgn) {
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

    /**
     * Saves a single move in a game.
     */
    public void saveMove(int gameId, int moveNumber, String fromSquare, String toSquare,
                         String pieceType, boolean isCapture, boolean isCheck, String notation) {
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
