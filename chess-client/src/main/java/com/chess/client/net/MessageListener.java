package com.chess.client.net;

import com.chess.common.protocol.Message;

/**
 * Callback interface for receiving asynchronous messages from the server.
 * Implemented by controllers/views that need to react to server events.
 */
public interface MessageListener {

    /**
     * Called when a message is received from the server.
     *
     * @param message the received message
     */
    void onMessageReceived(Message message);

    /**
     * Called when the connection to the server is lost unexpectedly.
     */
    void onConnectionLost();

    /**
     * Called when an error occurs during communication.
     *
     * @param error description of the error
     */
    void onError(String error);
}
