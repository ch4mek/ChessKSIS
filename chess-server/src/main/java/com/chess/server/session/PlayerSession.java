package com.chess.server.session;

import com.chess.common.model.GameColor;


public class PlayerSession {

    private final int userId;
    private final String username;
    private int rating;
    private GameRoom currentRoom;
    private GameColor assignedColor;

    public PlayerSession(int userId, String username, int rating) {
        this.userId = userId;
        this.username = username;
        this.rating = rating;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(GameRoom room) { this.currentRoom = room; }
    public GameColor getAssignedColor() { return assignedColor; }
    public void setAssignedColor(GameColor color) { this.assignedColor = color; }
    public boolean isInRoom() { return currentRoom != null; }

    @Override
    public String toString() {
        return username + " (rating: " + rating + ")";
    }
}
