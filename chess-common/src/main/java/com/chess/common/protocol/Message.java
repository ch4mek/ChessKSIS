package com.chess.common.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a network protocol message.
 * <p>
 * Wire format: TYPE|param1|param2|...|paramN\n
 * <p>
 * Examples:
 * - AUTH|alice|password123
 * - MOVE|e2|e4
 * - GAME_OVER|WIN|CHECKMATE|+25
 * - PING
 */
public class Message {

    private static final String DELIMITER = "\\|";
    private static final String JOINER = "|";
    private static final int MAX_MESSAGE_LENGTH = 4096;

    private final MessageType type;
    private final String[] params;

    /**
     * Creates a message with the given type and parameters.
     *
     * @param type   the message type
     * @param params the parameters (may be empty)
     */
    public Message(MessageType type, String... params) {
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
        this.params = params != null ? params : new String[0];
    }

    /**
     * Creates a message with no parameters.
     *
     * @param type the message type
     */
    public Message(MessageType type) {
        this(type, new String[0]);
    }

    /**
     * Parses a raw wire-format string into a Message object.
     *
     * @param raw the raw message string (without trailing newline)
     * @return the parsed Message
     * @throws ProtocolException if the message cannot be parsed
     */
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

    /**
     * Serializes this message to wire format (without trailing newline).
     *
     * @return the serialized message string
     */
    public String serialize() {
        if (params.length == 0) {
            return type.name();
        }
        return type.name() + JOINER + String.join(JOINER, params);
    }

    /**
     * Serializes this message to wire format with trailing newline.
     * This is the format suitable for sending over the network.
     *
     * @return the serialized message string with \n
     */
    public String serializeWithNewline() {
        return serialize() + "\n";
    }

    /**
     * @return the message type
     */
    public MessageType getType() {
        return type;
    }

    /**
     * @return the number of parameters
     */
    public int getParamCount() {
        return params.length;
    }

    /**
     * Gets a parameter by index.
     *
     * @param index the parameter index (0-based)
     * @return the parameter value
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String getParam(int index) {
        if (index < 0 || index >= params.length) {
            throw new IndexOutOfBoundsException(
                    "Parameter index " + index + " out of range (0.." + (params.length - 1) + ")");
        }
        return params[index];
    }

    /**
     * Gets a parameter by index, returning a default value if the index is out of range.
     *
     * @param index        the parameter index (0-based)
     * @param defaultValue the default value
     * @return the parameter value or default
     */
    public String getParam(int index, String defaultValue) {
        if (index < 0 || index >= params.length) {
            return defaultValue;
        }
        return params[index];
    }

    /**
     * Gets an integer parameter by index.
     *
     * @param index the parameter index (0-based)
     * @return the parameter value as integer
     * @throws ProtocolException if the parameter is not a valid integer
     */
    public int getIntParam(int index) throws ProtocolException {
        String param = getParam(index);
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            throw new ProtocolException("Parameter " + index + " is not a valid integer: " + param);
        }
    }

    /**
     * @return a copy of the parameters array
     */
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
