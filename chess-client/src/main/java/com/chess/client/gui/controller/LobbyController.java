package com.chess.client.gui.controller;

import com.chess.client.gui.SceneNavigator;
import com.chess.client.net.MessageListener;
import com.chess.client.net.ServerConnection;
import com.chess.common.protocol.Message;
import com.chess.common.protocol.MessageType;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the lobby screen.
 * Shows available game rooms and allows creating/joining rooms.
 */
public class LobbyController implements MessageListener {

    @FXML private Label userLabel;
    @FXML private Label ratingLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<RoomInfo> roomsTable;
    @FXML private TableColumn<RoomInfo, String> roomNameColumn;
    @FXML private TableColumn<RoomInfo, String> playersColumn;
    @FXML private TableColumn<RoomInfo, String> statusColumn;

    private ServerConnection connection;
    private SceneNavigator navigator;
    private String username;
    private String rating;

    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    public void setUserInfo(String username, String rating) {
        this.username = username;
        this.rating = rating;
        userLabel.setText("Player: " + username);
        ratingLabel.setText("Rating: " + rating);
    }

    @FXML
    private void initialize() {
        roomNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        playersColumn.setCellValueFactory(new PropertyValueFactory<>("players"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        roomsTable.setRowFactory(tv -> {
            TableRow<RoomInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onJoinRoom();
                }
            });
            return row;
        });

        statusLabel.setText("Connected to server");
    }

    public void requestRoomList() {
        try {
            connection.setListener(this);
            connection.sendMessage(new Message(MessageType.LIST_ROOMS));
        } catch (IOException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onCreateRoom() {
        try {
            connection.sendMessage(new Message(MessageType.CREATE_ROOM));
            statusLabel.setText("Creating room...");
        } catch (IOException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onRefreshRooms() {
        requestRoomList();
        statusLabel.setText("Refreshing room list...");
    }

    @FXML
    private void onJoinRoom() {
        RoomInfo selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a room to join");
            return;
        }

        try {
            connection.sendMessage(new Message(MessageType.JOIN_ROOM, selected.getId()));
            statusLabel.setText("Joining room " + selected.getName() + "...");
        } catch (IOException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        connection.disconnect();
        if (navigator != null) {
            LoginController controller = navigator.navigateToAndGetController(SceneNavigator.LOGIN);
            if (controller != null) {
                controller.setConnection(connection);
                controller.setNavigator(navigator);
            }
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case ROOM_LIST:
                    handleRoomList(message);
                    break;

                case ROOM_CREATED:
                    statusLabel.setText("Room created! Waiting for opponent...");
                    break;

                case ROOM_JOINED:
                    statusLabel.setText("Joined room " + message.getParam(0) + ". Waiting for opponent...");
                    break;

                case GAME_START:
                    handleGameStart(message);
                    break;

                case ROOM_JOIN_FAIL:
                    statusLabel.setText("Failed to join: " + message.getParam(0));
                    break;

                case ERROR:
                    statusLabel.setText("Error: " + message.getParam(0));
                    break;

                default:
                    break;
            }
        });
    }

    private void handleRoomList(Message message) {
        List<RoomInfo> rooms = new ArrayList<>();
        int paramCount = message.getParamCount();

        // Room list format: roomId|roomName|playerCount|status per entry
        for (int i = 0; i + 3 < paramCount; i += 4) {
            rooms.add(new RoomInfo(
                message.getParam(i),
                message.getParam(i + 1),
                message.getParam(i + 2),
                message.getParam(i + 3)
            ));
        }

        ObservableList<RoomInfo> roomList = FXCollections.observableArrayList(rooms);
        roomsTable.setItems(roomList);
        statusLabel.setText("Rooms: " + rooms.size());
    }

    private void handleGameStart(Message message) {
        // GAME_START params: color|opponentName|roomId|fen
        String color = message.getParam(0);
        String opponent = message.getParam(1);
        String roomId = message.getParam(2);
        String fen = message.getParam(3);

        if (navigator != null) {
            GameController controller = navigator.navigateToAndGetController(SceneNavigator.GAME);
            if (controller != null) {
                controller.setConnection(connection);
                controller.setNavigator(navigator);
                controller.initGame(color, opponent, roomId, fen);
            }
        }
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> statusLabel.setText("Connection to server lost"));
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> statusLabel.setText("Error: " + error));
    }

    /**
     * Data class for room table entries.
     */
    public static class RoomInfo {
        private final String id;
        private final String name;
        private final String players;
        private final String status;

        public RoomInfo(String id, String name, String players, String status) {
            this.id = id;
            this.name = name;
            this.players = players;
            this.status = status;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPlayers() { return players; }
        public String getStatus() { return status; }
    }
}
