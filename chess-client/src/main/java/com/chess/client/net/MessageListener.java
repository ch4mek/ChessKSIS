package com.chess.client.net;

import com.chess.common.protocol.Message;

public interface MessageListener {

    void onMessageReceived(Message message);

    void onConnectionLost();

    void onError(String error);
}
