package com.chess.client.net;

import com.chess.common.protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background thread that continuously reads messages from the server.
 * Uses BufferedReader.readLine() for TCP framing (newline-delimited protocol).
 * <p>
 * Runs as a daemon thread so it doesn't prevent JVM shutdown.
 */
public class MessageReceiver implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MessageReceiver.class.getName());

    private final BufferedReader reader;
    private final ServerConnection connection;
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Creates a new MessageReceiver.
     *
     * @param reader     the buffered reader connected to the server socket
     * @param connection the parent ServerConnection for callbacks
     */
    public MessageReceiver(BufferedReader reader, ServerConnection connection) {
        this.reader = reader;
        this.connection = connection;
    }

    @Override
    public void run() {
        LOGGER.info("MessageReceiver thread started");
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                // Parse the message — catch protocol/format errors separately
                Message message;
                try {
                    message = Message.parse(line);
                } catch (Exception e) {
                    LOGGER.warning("Failed to parse message: " + line + " - " + e.getMessage());
                    connection.onError("Invalid message from server: " + line);
                    continue;
                }

                LOGGER.fine("Received: " + message.serialize());

                // Dispatch to listener — catch UI/handler bugs separately so they
                // don't silently swallow game-critical messages like OPPONENT_MOVE
                try {
                    connection.onMessageReceived(message);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error handling message " + message.getType() + " in listener", e);
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                // Unexpected disconnect
                LOGGER.log(Level.WARNING, "Connection lost: " + e.getMessage());
                connection.onConnectionLost();
            }
            // else: normal disconnect, ignore
        } finally {
            running.set(false);
            LOGGER.info("MessageReceiver thread stopped");
        }
    }

    /**
     * Stops the receiver thread.
     * Does not close the reader — that's the responsibility of ServerConnection.
     */
    public void stop() {
        running.set(false);
    }

    /**
     * @return true if the receiver is still running
     */
    public boolean isRunning() {
        return running.get();
    }
}
