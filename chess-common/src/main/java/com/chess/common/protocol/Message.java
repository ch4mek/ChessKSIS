package com.chess.common.protocol;

import java.util.Arrays;
import java.util.Objects;

public class Message {

    private static final String DELIMITER = "\\|";
    private static final String JOINER = "|";
    private static final int MAX_MESSAGE_LENGTH = 4096;

    private final MessageType type;
    private final String[] params;

    public Message(MessageType type, String... params) {
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
        this.params = params != null ? params : new String[0];
    }

    public Message(MessageType type) {
        this(type, new String[0]);
    }

    public static Message parse(String raw) throws ProtocolException {
        if (raw == null || raw.isBlank()) {
            throw new ProtocolException("Empty message received");
        }

        String trimmed = raw.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new ProtocolException("Message exceeds maximum length: " + trimmed.length());
        }

        String[] parts = trimmed.split(DELIMITER, -1);
        if (parts.length == 0 || parts[0].isBlank()) {
            throw new ProtocolException("Missing message type in: " + trimmed);
        }

        MessageType type;
        try {
            type = MessageType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Unknown message type: " + parts[0]);
        }

        String[] params;
        if (parts.length > 1) {
            params = Arrays.copyOfRange(parts, 1, parts.length);
        } else {
            params = new String[0];
        }

        return new Message(type, params);
    }

    public String serialize() {
        if (params.length == 0) {
            return type.name();
        }
        return type.name() + JOINER + String.join(JOINER, params);
    }

    public String serializeWithNewline() {
        return serialize() + "\n";
    }

    public MessageType getType() {
        return type;
    }

    public int getParamCount() {
        return params.length;
    }

    public String getParam(int index) {
        if (index < 0 || index >= params.length) {
            throw new IndexOutOfBoundsException(
                    "Parameter index " + index + " out of range (0.." + (params.length - 1) + ")");
        }
        return params[index];
    }

    public String getParam(int index, String defaultValue) {
        if (index < 0 || index >= params.length) {
            return defaultValue;
        }
        return params[index];
    }

    public int getIntParam(int index) throws ProtocolException {
        String param = getParam(index);
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            throw new ProtocolException("Parameter " + index + " is not a valid integer: " + param);
        }
    }

    public String[] getParams() {
        return Arrays.copyOf(params, params.length);
    }

    @Override
    public String toString() {
        return "Message{" + serialize() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return type == message.type && Arrays.equals(params, message.params);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + Arrays.hashCode(params);
        return result;
    }
}
