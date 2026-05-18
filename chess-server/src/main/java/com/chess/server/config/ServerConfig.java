package com.chess.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server configuration loaded from a properties file.
 */
public class ServerConfig {

    private static final Logger LOGGER = Logger.getLogger(ServerConfig.class.getName());

    private int serverPort;
    private int backlog;
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private int reconnectTimeoutSeconds;
    private int defaultRating;
    private int ratingKFactor;

    public ServerConfig() {
        // Defaults
        this.serverPort = 5555;
        this.backlog = 10;
        this.dbUrl = "jdbc:mysql://localhost:3306/chess_db?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
        this.dbUsername = "root";
        this.dbPassword = "";
        this.reconnectTimeoutSeconds = 60;
        this.defaultRating = 1200;
        this.ratingKFactor = 32;
    }

    /**
     * Loads configuration from a properties file.
     *
     * @param filename the properties file path
     * @return loaded configuration
     */
    public static ServerConfig fromFile(String filename) {
        ServerConfig config = new ServerConfig();
        try (InputStream is = new FileInputStream(filename)) {
            config.load(is);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load config file: " + filename + ", using defaults", e);
        }
        return config;
    }

    /**
     * Loads configuration from an InputStream.
     */
    public void load(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);

        if (props.containsKey("server.port")) {
            serverPort = Integer.parseInt(props.getProperty("server.port"));
        }
        if (props.containsKey("server.backlog")) {
            backlog = Integer.parseInt(props.getProperty("server.backlog"));
        }
        if (props.containsKey("db.url")) {
            dbUrl = props.getProperty("db.url");
        }
        if (props.containsKey("db.username")) {
            dbUsername = props.getProperty("db.username");
        }
        if (props.containsKey("db.password")) {
            dbPassword = props.getProperty("db.password");
        }
        if (props.containsKey("reconnect.timeout.seconds")) {
            reconnectTimeoutSeconds = Integer.parseInt(props.getProperty("reconnect.timeout.seconds"));
        }
        if (props.containsKey("rating.default")) {
            defaultRating = Integer.parseInt(props.getProperty("rating.default"));
        }
        if (props.containsKey("rating.change.k_factor")) {
            ratingKFactor = Integer.parseInt(props.getProperty("rating.change.k_factor"));
        }
    }

    public int getServerPort() { return serverPort; }
    public int getBacklog() { return backlog; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public int getReconnectTimeoutSeconds() { return reconnectTimeoutSeconds; }
    public int getDefaultRating() { return defaultRating; }
    public int getRatingKFactor() { return ratingKFactor; }
}
