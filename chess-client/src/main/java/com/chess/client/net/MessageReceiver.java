package com.chess.client.net;

import com.chess.common.protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageReceiver implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MessageReceiver.class.getName());

    private final BufferedReader reader;
    private final ServerConnection connection;
    private final AtomicBoolean running = new AtomicBoolean(true);

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
                    continue;
                }

                Message message;
                try {
                    message = Message.parse(line);
                } catch (Exception e) {
                    LOGGER.warning("Failed to parse message: " + line + " - " + e.getMessage());
                    connection.onError("Invalid message from server: " + line);
                    continue;
                }

                LOGGER.fine("Received: " + message.serialize());

                try {
                    connection.onMessageReceived(message);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error handling message " + message.getType() + " in listener", e);
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                LOGGER.log(Level.WARNING, "Connection lost: " + e.getMessage());
                connection.onConnectionLost();
            }
        } finally {
            running.set(false);
            LOGGER.info("MessageReceiver thread stopped");
        }
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }
}
