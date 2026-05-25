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


    public void start() {
        try {

            dbManager.connect();
            LOGGER.info("Database connected successfully");

            threadPool = Executors.newCachedThreadPool();

            serverSocket = new ServerSocket(config.getServerPort(), config.getBacklog());
            running = true;

            LOGGER.info("Chess Server started on port " + config.getServerPort());
            LOGGER.info("Waiting for client connections...");

            acceptLoop();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server IO error", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server startup error", e);
        }
    }


    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);
                clientSocket.setSoTimeout(60_000);

                String clientAddr = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();
                LOGGER.info("Client connected: " + clientAddr + ":" + clientPort);

                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.submit(handler);

            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Error accepting client connection", e);
                }
            }
        }
    }


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

    public GameManager getGameManager() { return gameManager; }
    public AuthService getAuthService() { return authService; }
    public DatabaseManager getDbManager() { return dbManager; }
    public GameDAO getGameDAO() { return gameDAO; }
    public UserDAO getUserDAO() { return userDAO; }
    public ServerConfig getConfig() { return config; }
}
