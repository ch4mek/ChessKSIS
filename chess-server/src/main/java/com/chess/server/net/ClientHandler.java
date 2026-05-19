package com.chess.server.net;

import com.chess.common.game.GameStatus;
import com.chess.common.model.GameColor;
import com.chess.common.model.Move;
import com.chess.common.protocol.Message;
import com.chess.common.protocol.MessageType;
import com.chess.common.protocol.ProtocolException;
import com.chess.server.db.UserDAO;
import com.chess.server.session.GameRoom;
import com.chess.server.session.GameRoom.MoveResult;
import com.chess.server.session.PlayerSession;
import com.chess.server.session.RoomState;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles communication with a single client over a TCP socket.
 * <p>
 * Each client connection gets its own ClientHandler running in a separate thread.
 * This implements the "one thread per connection" concurrency model.
 * <p>
 * Network communication uses:
 * - BufferedReader.readLine() for framing (messages delimited by \n)
 * - PrintWriter.println() for sending (automatically adds \n)
 * - TCP sockets via java.net.Socket
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket clientSocket;
    private final ChessServer server;

    // I/O streams for TCP communication
    private BufferedReader in;
    private PrintWriter out;

    private PlayerSession session;
    private volatile boolean running;

    public ClientHandler(Socket clientSocket, ChessServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.running = true;
    }

    /**
     * Main loop: reads messages from the client and processes them.
     * <p>
     * The readLine() call blocks until a full line (\n) is received,
     * which implements the framing mechanism of our application-layer protocol.
     */
    @Override
    public void run() {
        try {
            // Initialize I/O streams from the TCP socket
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            // Main read loop — blocks on readLine() until a message arrives
            String rawMessage;
            while (running && (rawMessage = in.readLine()) != null) {
                try {
                    Message message = Message.parse(rawMessage);
                    handleMessage(message);
                } catch (ProtocolException e) {
                    sendMessage(new Message(MessageType.ERROR, "Invalid message: " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.log(Level.INFO, "Client disconnected: " + getClientInfo(), e);
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Routes incoming messages to the appropriate handler method.
     */
    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            // Authentication
            case AUTH -> handleAuth(msg);
            case REGISTER -> handleRegister(msg);

            // Room management
            case CREATE_ROOM -> handleCreateRoom();
            case LIST_ROOMS -> handleListRooms();
            case JOIN_ROOM -> handleJoinRoom(msg);
            case LEAVE_ROOM -> handleLeaveRoom();

            // Game moves
            case MOVE -> handleMove(msg);
            case PROMOTE -> handlePromote(msg);

            // Game actions
            case RESIGN -> handleResign();
            case OFFER_DRAW -> handleOfferDraw();
            case ACCEPT_DRAW -> handleAcceptDraw();
            case DECLINE_DRAW -> handleDeclineDraw();

            // Chat
            case CHAT -> handleChat(msg);

            // Keepalive
            case PING -> sendMessage(new Message(MessageType.PONG));

            // Disconnect
            case DISCONNECT -> {
                sendMessage(new Message(MessageType.DISCONNECT));
                running = false;
            }

            default -> sendMessage(new Message(MessageType.ERROR, "Unknown command"));
        }
    }

    // ========================================================================
    // Authentication handlers
    // ========================================================================

    private void handleAuth(Message msg) {
        if (session != null) {
            sendMessage(new Message(MessageType.ERROR, "Already authenticated"));
            return;
        }
        String username = msg.getParam(0);
        String password = msg.getParam(1);
        UserDAO.UserRecord user = server.getAuthService().login(username, password);
        if (user != null) {
            // Check if this user is already logged in from another connection
            ClientHandler existingHandler = loggedInUsers.get(user.id);
            if (existingHandler != null && existingHandler != this) {
                sendMessage(new Message(MessageType.AUTH_FAIL, "User '" + username + "' is already logged in from another connection"));
                LOGGER.warning("Duplicate login attempt for user: " + username);
                return;
            }
            session = new PlayerSession(user.id, user.username, user.rating);
            registerHandler();
            sendMessage(new Message(MessageType.AUTH_OK, user.username, String.valueOf(user.rating)));
            LOGGER.info("User authenticated: " + username);
        } else {
            sendMessage(new Message(MessageType.AUTH_FAIL, "Invalid username or password"));
        }
    }

    private void handleRegister(Message msg) {
        String username = msg.getParam(0);
        String password = msg.getParam(1);
        UserDAO.UserRecord user = server.getAuthService().register(username, password);
        if (user != null) {
            sendMessage(new Message(MessageType.REGISTER_OK));
            LOGGER.info("User registered: " + username);
        } else {
            sendMessage(new Message(MessageType.REGISTER_FAIL, "Username taken or invalid"));
        }
    }

    // ========================================================================
    // Room management handlers
    // ========================================================================

    private void handleCreateRoom() {
        if (requireAuth()) return;
        if (session.isInRoom()) {
            sendMessage(new Message(MessageType.ERROR, "Already in a room"));
            return;
        }
        GameRoom room = server.getGameManager().createRoom(session);
        session.setCurrentRoom(room);
        session.setAssignedColor(GameColor.WHITE);
        sendMessage(new Message(MessageType.ROOM_CREATED, room.getRoomId()));
    }

    private void handleListRooms() {
        if (requireAuth()) return;
        java.util.List<GameRoom> rooms = server.getGameManager().getAvailableRooms();
        if (rooms.isEmpty()) {
            sendMessage(new Message(MessageType.ROOM_LIST));
            return;
        }
        String[] params = new String[rooms.size() * 4];
        for (int i = 0; i < rooms.size(); i++) {
            GameRoom room = rooms.get(i);
            params[i * 4] = room.getRoomId();
            params[i * 4 + 1] = room.getWhitePlayer().getUsername();
            params[i * 4 + 2] = "1"; // player count (host is already in)
            params[i * 4 + 3] = room.getState().name();
        }
        sendMessage(new Message(MessageType.ROOM_LIST, params));
    }

    private void handleJoinRoom(Message msg) {
        if (requireAuth()) return;

        // If player is already in a WAITING room (e.g. they created one), leave it first
        if (session.isInRoom()) {
            GameRoom currentRoom = session.getCurrentRoom();
            if (currentRoom.getState() == RoomState.WAITING) {
                // Leave the current room so we can join another
                currentRoom.removePlayer(session);
                session.setCurrentRoom(null);
                server.getGameManager().removeRoom(currentRoom.getRoomId());
            } else {
                sendMessage(new Message(MessageType.ROOM_JOIN_FAIL, "Already in an active game"));
                return;
            }
        }

        String roomId = msg.getParam(0);
        GameRoom room = server.getGameManager().joinRoom(roomId, session);
        if (room != null) {
            String fen = room.getBoard().toFEN();

            // Notify joining player (BLACK)
            sendMessage(new Message(MessageType.ROOM_JOINED, roomId, "BLACK"));
            sendMessage(new Message(MessageType.GAME_START,
                    "BLACK",
                    room.getWhitePlayer().getUsername(),
                    roomId,
                    fen));

            // Notify host player (WHITE)
            PlayerSession opponent = room.getOpponent(session);
            sendToPlayer(opponent, new Message(MessageType.GAME_START,
                    "WHITE",
                    room.getBlackPlayer().getUsername(),
                    roomId,
                    fen));

            // Save game to DB
            int gameId = server.getGameDAO().saveGame(
                    room.getWhitePlayer().getUserId(),
                    room.getBlackPlayer().getUserId());
            room.setGameId(gameId);
        } else {
            sendMessage(new Message(MessageType.ROOM_JOIN_FAIL, "Room not found or full"));
        }
    }

    private void handleLeaveRoom() {
        if (requireAuth()) return;
        if (!session.isInRoom()) return;
        GameRoom room = session.getCurrentRoom();
        room.removePlayer(session);
        session.setCurrentRoom(null);
        server.getGameManager().removeRoom(room.getRoomId());
    }

    // ========================================================================
    // Game move handlers
    // ========================================================================

    private void handleMove(Message msg) {
        if (requireAuth()) return;
        if (!session.isInRoom()) {
            sendMessage(new Message(MessageType.ERROR, "Not in a game"));
            return;
        }

        String fromStr = msg.getParam(0);
        String toStr = msg.getParam(1);
        Move move = Move.fromAlgebraic(fromStr, toStr);

        GameRoom room = session.getCurrentRoom();
        MoveResult result = room.makeMove(session, move);

        if (result.isSuccess()) {
            // Notify current player
            sendMessage(new Message(MessageType.MOVE_OK, fromStr, toStr, result.getFen()));

            // Notify opponent
            PlayerSession opponent = room.getOpponent(session);
            if (opponent != null) {
                sendToPlayer(opponent, new Message(MessageType.OPPONENT_MOVE, fromStr, toStr, result.getFen()));
            }

            // Check for game over
            if (result.getGameStatus().isGameOver()) {
                handleGameOver(room, result.getGameStatus());
            }
        } else {
            sendMessage(new Message(MessageType.MOVE_INVALID, result.getErrorMessage()));
        }
    }

    private void handlePromote(Message msg) {
        // Promotion is handled within the MOVE message in our simplified protocol.
        // A full implementation would track pending promotions.
        sendMessage(new Message(MessageType.ERROR, "Use MOVE with promotion parameter"));
    }

    // ========================================================================
    // Game action handlers
    // ========================================================================

    private void handleResign() {
        if (requireAuth()) return;
        if (!session.isInRoom()) return;

        GameRoom room = session.getCurrentRoom();

        // Determine result based on who resigned
        GameStatus status = session.getAssignedColor() == GameColor.WHITE
                ? GameStatus.BLACK_WINS_RESIGN
                : GameStatus.WHITE_WINS_RESIGN;

        // handleGameOver sends GAME_OVER to both players and cleans up the room
        handleGameOver(room, status);
    }

    private void handleOfferDraw() {
        if (requireAuth()) return;
        if (!session.isInRoom()) return;
        PlayerSession opponent = session.getCurrentRoom().getOpponent(session);
        if (opponent != null) {
            sendToPlayer(opponent, new Message(MessageType.DRAW_OFFERED));
        }
    }

    private void handleAcceptDraw() {
        if (requireAuth()) return;
        if (!session.isInRoom()) return;
        GameRoom room = session.getCurrentRoom();
        handleGameOver(room, GameStatus.DRAW_AGREEMENT);
    }

    private void handleDeclineDraw() {
        if (requireAuth()) return;
        if (!session.isInRoom()) return;
        // Notify the proposer that their draw offer was declined
        PlayerSession proposer = session.getCurrentRoom().getOpponent(session);
        if (proposer != null) {
            sendToPlayer(proposer, new Message(MessageType.DRAW_OFFERED, "DECLINED"));
        }
    }

    private void handleChat(Message msg) {
        if (requireAuth()) return;
        if (!session.isInRoom()) return;
        String text = msg.getParam(0);
        PlayerSession opponent = session.getCurrentRoom().getOpponent(session);
        if (opponent != null) {
            sendToPlayer(opponent, new Message(MessageType.CHAT_MSG, session.getUsername(), text));
        }
    }

    // ========================================================================
    // Game over handling
    // ========================================================================

    private String handleGameOver(GameRoom room, GameStatus status) {
        // Calculate rating changes
        int whiteRating = room.getWhitePlayer().getRating();
        int blackRating = room.getBlackPlayer().getRating();
        int kFactor = server.getConfig().getRatingKFactor();

        int whiteChange = 0;
        int blackChange = 0;
        String resultStr;

        if (status.isWhiteWins()) {
            whiteChange = calculateEloChange(whiteRating, blackRating, kFactor, 1.0);
            blackChange = calculateEloChange(blackRating, whiteRating, kFactor, 0.0);
            resultStr = "white_win";
        } else if (status.isBlackWins()) {
            whiteChange = calculateEloChange(whiteRating, blackRating, kFactor, 0.0);
            blackChange = calculateEloChange(blackRating, whiteRating, kFactor, 1.0);
            resultStr = "black_win";
        } else {
            whiteChange = calculateEloChange(whiteRating, blackRating, kFactor, 0.5);
            blackChange = calculateEloChange(blackRating, whiteRating, kFactor, 0.5);
            resultStr = "draw";
        }

        // Update ratings
        room.getWhitePlayer().setRating(whiteRating + whiteChange);
        room.getBlackPlayer().setRating(blackRating + blackChange);
        server.getUserDAO().updateRating(room.getWhitePlayer().getUserId(), whiteRating + whiteChange);
        server.getUserDAO().updateRating(room.getBlackPlayer().getUserId(), blackRating + blackChange);

        // Update stats
        server.getUserDAO().incrementStats(room.getWhitePlayer().getUserId(),
                status.isWhiteWins(), status.isBlackWins(), status.isDraw());
        server.getUserDAO().incrementStats(room.getBlackPlayer().getUserId(),
                status.isBlackWins(), status.isWhiteWins(), status.isDraw());

        // Save game result
        if (room.getGameId() > 0) {
            server.getGameDAO().updateGameResult(room.getGameId(), resultStr,
                    status.name().toLowerCase(), room.toPGN());
        }

        // Send GAME_OVER to both players
        String reason = status.name().toLowerCase();
        sendToPlayer(room.getWhitePlayer(), new Message(MessageType.GAME_OVER,
                status.isWhiteWins() ? "WIN" : (status.isBlackWins() ? "LOSE" : "DRAW"),
                reason, (whiteChange >= 0 ? "+" : "") + whiteChange));
        sendToPlayer(room.getBlackPlayer(), new Message(MessageType.GAME_OVER,
                status.isBlackWins() ? "WIN" : (status.isWhiteWins() ? "LOSE" : "DRAW"),
                reason, (blackChange >= 0 ? "+" : "") + blackChange));

        // Clean up
        server.getGameManager().removeRoom(room.getRoomId());
        room.getWhitePlayer().setCurrentRoom(null);
        room.getBlackPlayer().setCurrentRoom(null);

        return String.valueOf(whiteChange);
    }

    /**
     * Simplified Elo rating calculation.
     */
    private int calculateEloChange(int playerRating, int opponentRating, int kFactor, double score) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));
        return (int) Math.round(kFactor * (score - expectedScore));
    }

    // ========================================================================
    // Network I/O
    // ========================================================================

    /**
     * Sends a message to this client.
     * <p>
     * Synchronized to prevent concurrent writes to the output stream
     * from multiple threads (e.g., when opponent moves and server notification arrive).
     *
     * @param msg the message to send
     */
    public synchronized void sendMessage(Message msg) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(msg.serialize());
        }
    }

    /**
     * Sends a message to another player's handler.
     * Finds the handler by searching active connections.
     */
    private void sendToPlayer(PlayerSession player, Message msg) {
        if (player == null) return;
        // We need to find the ClientHandler for this player
        // For simplicity, we store the handler reference in the session
        // This is set when the handler is created
        // Alternative: server maintains a map of userId -> ClientHandler
        // For now, we use a simpler approach: store handler in session
        ClientHandler opponentHandler = getHandlerForSession(player);
        if (opponentHandler != null) {
            opponentHandler.sendMessage(msg);
        }
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private boolean requireAuth() {
        if (session == null) {
            sendMessage(new Message(MessageType.ERROR, "Not authenticated"));
            return true;
        }
        return false;
    }

    private String getClientInfo() {
        return clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() +
                (session != null ? " (" + session.getUsername() + ")" : "");
    }

    private void disconnect() {
        running = false;
        try {
            if (session != null) {
                LOGGER.info("Player disconnected: " + session.getUsername());

                // Remove from logged-in users map
                loggedInUsers.remove(session.getUserId(), this);

                // Notify opponent
                if (session.isInRoom()) {
                    GameRoom room = session.getCurrentRoom();
                    PlayerSession opponent = room.getOpponent(session);
                    if (opponent != null) {
                        sendToPlayer(opponent, new Message(MessageType.OPPONENT_DISCONNECTED));
                    }
                    server.getGameManager().removePlayerFromRooms(session);
                }
            }

            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during disconnect", e);
        }
    }

    // Session-to-handler mapping (simplified approach)
    private static final java.util.concurrent.ConcurrentHashMap<PlayerSession, ClientHandler> handlerMap
            = new java.util.concurrent.ConcurrentHashMap<>();

    // Tracks logged-in users by userId to prevent duplicate sessions
    private static final java.util.concurrent.ConcurrentHashMap<Integer, ClientHandler> loggedInUsers
            = new java.util.concurrent.ConcurrentHashMap<>();

    public PlayerSession getSession() { return session; }

    /**
     * Registers this handler in the global map when session is created.
     */
    private void registerHandler() {
        if (session != null) {
            handlerMap.put(session, this);
            loggedInUsers.put(session.getUserId(), this);
        }
    }

    private ClientHandler getHandlerForSession(PlayerSession player) {
        return handlerMap.get(player);
    }
}
