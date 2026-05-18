package com.chess.server.net;

import com.chess.server.config.ServerConfig;
import com.chess.server.db.DatabaseManager;
import com.chess.server.db.GameDAO;
import com.chess.server.db.UserDAO;
import com.chess.server.service.AuthService;
import com.chess.server.session.GameManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCP Chess Server.
 * Accepts client connections and spawns a ClientHandler thread for each.
 * <p>
 * This is the core networking component of the application.
 * Uses ServerSocket for TCP listening and ExecutorService for thread management.
 */
public class ChessServer {

    private static final Logger LOGGER = Logger.getLogger(ChessServer.class.getName());

    private final ServerConfig config;
    private final GameManager gameManager;
    private final AuthService authService;
    private final DatabaseManager dbManager;
    private final GameDAO gameDAO;
    private final UserDAO userDAO;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;

    public ChessServer(ServerConfig config) {
        this.config = config;
        this.gameManager = new GameManager();
        this.dbManager = new DatabaseManager(config.getDbUrl(), config.getDbUsername(), config.getDbPassword());
        this.userDAO = new UserDAO(dbManager);
        this.gameDAO = new GameDAO(dbManager);
        this.authService = new AuthService(userDAO);
    }

    /**
     * Starts the server: connects to DB and begins accepting connections.
     */
    public void start() {
        try {
            // Connect to database
            dbManager.connect();
            LOGGER.info("Database connected successfully");

            // Create thread pool for client handlers
            threadPool = Executors.newCachedThreadPool();

            // Bind server socket to port (0.0.0.0 = all interfaces)
            serverSocket = new ServerSocket(config.getServerPort(), config.getBacklog());
            running = true;

            LOGGER.info("Chess Server started on port " + config.getServerPort());
            LOGGER.info("Waiting for client connections...");

            // Accept loop — blocks until stop() is called
            acceptLoop();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server IO error", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server startup error", e);
        }
    }

    /**
     * Main accept loop. Blocks on ServerSocket.accept() and spawns
     * a new ClientHandler thread for each connection.
     * <p>
     * This demonstrates the classic TCP server pattern:
     * 1. ServerSocket.accept() returns a Socket for each new client
     * 2. A new thread (via ExecutorService) handles the client
     * 3. The main thread goes back to accepting more connections
     */
    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true); // Disable Nagle's algorithm for lower latency
                clientSocket.setSoTimeout(0); // No read timeout (blocking reads)

                String clientAddr = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();
                LOGGER.info("Client connected: " + clientAddr + ":" + clientPort);

                // Create handler and submit to thread pool
                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.submit(handler);

            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * Stops the server gracefully.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdownNow();
            }
            dbManager.disconnect();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error stopping server", e);
        }
        LOGGER.info("Chess Server stopped");
    }

    // Getters for components used by ClientHandler
    public GameManager getGameManager() { return gameManager; }
    public AuthService getAuthService() { return authService; }
    public DatabaseManager getDbManager() { return dbManager; }
    public GameDAO getGameDAO() { return gameDAO; }
    public UserDAO getUserDAO() { return userDAO; }
    public ServerConfig getConfig() { return config; }
}
