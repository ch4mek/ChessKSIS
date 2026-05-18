package com.chess.server.db;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for user-related database operations.
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    private final DatabaseManager dbManager;

    public UserDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Creates a new user in the database.
     *
     * @param username     the username
     * @param passwordHash the SHA-256 hash of the password
     * @return true if created successfully
     */
    public boolean createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LOGGER.info("User created: " + username);
                return true;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                LOGGER.warning("Username already exists: " + username);
            } else {
                LOGGER.log(Level.SEVERE, "Error creating user: " + username, e);
            }
        }
        return false;
    }

    /**
     * Finds a user by username and verifies password.
     *
     * @param username     the username
     * @param passwordHash the SHA-256 hash of the password
     * @return UserRecord if found and password matches, null otherwise
     */
    public UserRecord findByUsername(String username, String passwordHash) {
        String sql = "SELECT id, username, password_hash, rating, games_played, wins, losses, draws FROM users WHERE username = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (storedHash.equals(passwordHash)) {
                    return new UserRecord(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getInt("rating"),
                            rs.getInt("games_played"),
                            rs.getInt("wins"),
                            rs.getInt("losses"),
                            rs.getInt("draws")
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user: " + username, e);
        }
        return null;
    }

    /**
     * Updates a user's rating.
     */
    public void updateRating(int userId, int newRating) {
        String sql = "UPDATE users SET rating = ? WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, newRating);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating rating for user " + userId, e);
        }
    }

    /**
     * Increments game statistics for a user.
     */
    public void incrementStats(int userId, boolean win, boolean loss, boolean draw) {
        String sql = "UPDATE users SET games_played = games_played + 1, " +
                "wins = wins + ?, losses = losses + ?, draws = draws + ? WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, win ? 1 : 0);
            stmt.setInt(2, loss ? 1 : 0);
            stmt.setInt(3, draw ? 1 : 0);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating stats for user " + userId, e);
        }
    }

    /**
     * Simple data record for a user.
     */
    public static class UserRecord {
        public final int id;
        public final String username;
        public final int rating;
        public final int gamesPlayed;
        public final int wins;
        public final int losses;
        public final int draws;

        public UserRecord(int id, String username, int rating, int gamesPlayed, int wins, int losses, int draws) {
            this.id = id;
            this.username = username;
            this.rating = rating;
            this.gamesPlayed = gamesPlayed;
            this.wins = wins;
            this.losses = losses;
            this.draws = draws;
        }
    }
}
