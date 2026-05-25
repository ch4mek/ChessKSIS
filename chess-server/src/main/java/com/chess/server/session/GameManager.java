package com.chess.server.session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class GameManager {

    private static final Logger LOGGER = Logger.getLogger(GameManager.class.getName());

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger roomCounter = new AtomicInteger(0);


    public synchronized GameRoom createRoom(PlayerSession host) {
        String roomId = "room_" + roomCounter.incrementAndGet();
        GameRoom room = new GameRoom(roomId, host);
        rooms.put(roomId, room);
        LOGGER.info("Room created: " + roomId + " by " + host.getUsername());
        return room;
    }


    public synchronized GameRoom joinRoom(String roomId, PlayerSession player) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return null;
        }
        if (room.addPlayer(player)) {
            LOGGER.info(player.getUsername() + " joined room " + roomId);
            return room;
        }
        return null;
    }

    public synchronized void removeRoom(String roomId) {
        rooms.remove(roomId);
        LOGGER.info("Room removed: " + roomId);
    }


    public GameRoom getRoomById(String roomId) {
        return rooms.get(roomId);
    }


    public synchronized List<GameRoom> getAvailableRooms() {
        return rooms.values().stream()
                .filter(r -> r.getState() == RoomState.WAITING)
                .collect(Collectors.toList());
    }


    public List<GameRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }


    public synchronized void removePlayerFromRooms(PlayerSession player) {
        List<String> toRemove = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            if (room.hasPlayer(player)) {
                room.removePlayer(player);
                if (room.getState() == RoomState.FINISHED || !room.isFull()) {
                    toRemove.add(room.getRoomId());
                }
            }
        }
        for (String roomId : toRemove) {
            removeRoom(roomId);
        }
    }

    public synchronized String buildRoomListString() {
        return rooms.values().stream()
                .filter(r -> r.getState() == RoomState.WAITING && r.getWhitePlayer() != null)
                .map(r -> r.getRoomId() + ":" + r.getWhitePlayer().getUsername() + ":" + r.getState().name().toLowerCase())
                .collect(Collectors.joining(";"));
    }
}
