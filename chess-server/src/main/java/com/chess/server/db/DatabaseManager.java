package com.chess.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the MySQL database connection.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private Connection connection;
    private final String url;
    private final String username;
    private final String password;

    public DatabaseManager(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Establishes the database connection.
     *
     * @throws SQLException if connection fails
     */
    public synchronized void connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        connection = DriverManager.getConnection(url, username, password);
        LOGGER.info("Connected to MySQL database: " + url);
    }

    /**
     * Gets the active connection, reconnecting if necessary.
     * Synchronized to prevent concurrent access to the single JDBC Connection
     * from multiple ClientHandler threads.
     *
     * @return the database connection
     * @throws SQLException if connection fails
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Disconnected from MySQL database");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
}
