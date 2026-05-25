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

public class ServerConnection {

    private static final Logger LOGGER = Logger.getLogger(ServerConnection.class.getName());
    private static final int DEFAULT_PORT = 5555;
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int PING_INTERVAL_SEC = 15;
    private static final int PONG_TIMEOUT_MS = 45_000;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private MessageReceiver receiver;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private MessageListener listener;

    private volatile long lastPongTime;
    private ScheduledExecutorService heartbeatExecutor;

    public void connect(String host, int port) throws IOException {
        if (connected.get()) {
            disconnect();
        }

        LOGGER.info("Connecting to " + host + ":" + port + "...");
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT);
        socket.setTcpNoDelay(true);

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

        connected.set(true);

        receiver = new MessageReceiver(reader, this);
        Thread receiverThread = new Thread(receiver, "MessageReceiver");
        receiverThread.setDaemon(true);
        receiverThread.start();

        lastPongTime = System.currentTimeMillis();
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, PING_INTERVAL_SEC, PING_INTERVAL_SEC, TimeUnit.SECONDS);

        LOGGER.info("Connected to " + host + ":" + port);
    }

    public void connect(String host) throws IOException {
        connect(host, DEFAULT_PORT);
    }

    public void disconnect() {
        if (!connected.getAndSet(false)) {
            return;
        }

        LOGGER.info("Disconnecting from server...");

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }

        if (receiver != null) {
            receiver.stop();
            receiver = null;
        }

        if (writer != null) {
            writer.close();
            writer = null;
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
            }
            reader = null;
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }

        LOGGER.info("Disconnected from server");
    }

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

    public void sendMessage(MessageType type) throws IOException {
        sendMessage(new Message(type));
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected.get();
    }

    void onMessageReceived(Message message) {
        if (message.getType() == MessageType.PONG) {
            lastPongTime = System.currentTimeMillis();
            LOGGER.fine("PONG received — connection alive");
            return;
        }
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

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

    void onError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

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
