package com.chess.client.net;

import com.chess.common.protocol.Message;
import com.chess.common.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the TCP connection to the chess server.
 * Provides methods to connect, disconnect, and send messages.
 * Uses a background thread (MessageReceiver) to listen for incoming messages.
 * <p>
 * Includes a heartbeat mechanism: sends PING every 15 seconds.
 * If no PONG is received within 45 seconds, the connection is considered dead.
 */
public class ServerConnection {

    private static final Logger LOGGER = Logger.getLogger(ServerConnection.class.getName());
    private static final int DEFAULT_PORT = 5555;
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int PING_INTERVAL_SEC = 15;
    private static final int PONG_TIMEOUT_MS = 45_000;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private MessageReceiver receiver;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private MessageListener listener;

    // Heartbeat state
    private volatile long lastPongTime;
    private ScheduledExecutorService heartbeatExecutor;

    /**
     * Connects to the chess server at the specified host and port.
     *
     * @param host server hostname or IP address
     * @param port server port
     * @throws IOException if connection fails
     */
    public void connect(String host, int port) throws IOException {
        if (connected.get()) {
            disconnect();
        }

        LOGGER.info("Connecting to " + host + ":" + port + "...");
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT);
        socket.setTcpNoDelay(true); // Disable Nagle's algorithm for real-time interaction

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

        connected.set(true);

        // Start the background message receiver thread
        receiver = new MessageReceiver(reader, this);
        Thread receiverThread = new Thread(receiver, "MessageReceiver");
        receiverThread.setDaemon(true);
        receiverThread.start();

        // Start heartbeat
        lastPongTime = System.currentTimeMillis();
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, PING_INTERVAL_SEC, PING_INTERVAL_SEC, TimeUnit.SECONDS);

        LOGGER.info("Connected to " + host + ":" + port);
    }

    /**
     * Connects to the chess server using default port.
     *
     * @param host server hostname or IP address
     * @throws IOException if connection fails
     */
    public void connect(String host) throws IOException {
        connect(host, DEFAULT_PORT);
    }

    /**
     * Disconnects from the server, closing all resources.
     */
    public void disconnect() {
        if (!connected.getAndSet(false)) {
            return; // Already disconnected
        }

        LOGGER.info("Disconnecting from server...");

        // Stop heartbeat
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }

        // Stop the receiver thread
        if (receiver != null) {
            receiver.stop();
            receiver = null;
        }

        // Close the writer
        if (writer != null) {
            writer.close();
            writer = null;
        }

        // Close the reader
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // Ignore
            }
            reader = null;
        }

        // Close the socket
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
            socket = null;
        }

        LOGGER.info("Disconnected from server");
    }

    /**
     * Sends a message to the server.
     * Thread-safe: uses synchronized to prevent concurrent writes.
     *
     * @param message the message to send
     * @throws IOException if not connected or write fails
     */
    public synchronized void sendMessage(Message message) throws IOException {
        if (!connected.get() || writer == null) {
            throw new IOException("Not connected to server");
        }
        String serialized = message.serializeWithNewline();
        writer.print(serialized);
        writer.flush();

        if (writer.checkError()) {
            throw new IOException("Failed to send message (write error)");
        }

        LOGGER.fine("Sent: " + message.serialize());
    }

    /**
     * Sends a message of the given type with no parameters.
     *
     * @param type the message type
     * @throws IOException if not connected or write fails
     */
    public void sendMessage(MessageType type) throws IOException {
        sendMessage(new Message(type));
    }

    /**
     * Sets the listener for incoming messages and connection events.
     *
     * @param listener the listener to set
     */
    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    /**
     * @return true if currently connected to the server
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Called by MessageReceiver when a message is received.
     * Intercepts PONG for heartbeat tracking, then dispatches to the registered listener.
     *
     * @param message the received message
     */
    void onMessageReceived(Message message) {
        if (message.getType() == MessageType.PONG) {
            lastPongTime = System.currentTimeMillis();
            LOGGER.fine("PONG received — connection alive");
            return; // Heartbeat response, don't forward to UI listener
        }
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

    /**
     * Called by MessageReceiver when the connection is lost.
     */
    void onConnectionLost() {
        if (connected.getAndSet(false)) {
            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdownNow();
                heartbeatExecutor = null;
            }
            if (listener != null) {
                listener.onConnectionLost();
            }
        }
    }

    /**
     * Called by MessageReceiver when an error occurs.
     *
     * @param error description of the error
     */
    void onError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    /**
     * Heartbeat task: sends PING and checks for PONG timeout.
     * Runs periodically every PING_INTERVAL_SEC seconds.
     */
    private void sendHeartbeat() {
        try {
            if (!connected.get()) return;

            long elapsed = System.currentTimeMillis() - lastPongTime;
            if (elapsed > PONG_TIMEOUT_MS) {
                LOGGER.warning("No PONG received for " + (elapsed / 1000) + "s — connection dead");
                onConnectionLost();
                return;
            }

            sendMessage(new Message(MessageType.PING));
            LOGGER.fine("PING sent");
        } catch (IOException e) {
            LOGGER.warning("Failed to send PING: " + e.getMessage());
            onConnectionLost();
        }
    }
}
