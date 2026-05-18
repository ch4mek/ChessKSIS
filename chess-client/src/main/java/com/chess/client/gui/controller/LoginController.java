package com.chess.client.gui.controller;

import com.chess.client.gui.SceneNavigator;
import com.chess.client.net.MessageListener;
import com.chess.client.net.ServerConnection;
import com.chess.common.protocol.Message;
import com.chess.common.protocol.MessageType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller for the login screen.
 * Handles server connection and user authentication.
 */
public class LoginController implements MessageListener {

    @FXML private TextField serverHostField;
    @FXML private TextField serverPortField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    private ServerConnection connection;
    private SceneNavigator navigator;

    /**
     * Called by ClientApp to inject shared dependencies.
     */
    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void onLogin() {
        String host = serverHostField.getText().trim();
        String portText = serverPortField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setText("Connecting to server...");

        // Connect in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                if (!connection.isConnected()) {
                    connection.connect(host, port);
                }
                connection.setListener(this);

                // Send AUTH message
                connection.sendMessage(new Message(MessageType.AUTH, username, password));

                Platform.runLater(() -> statusLabel.setText("Authenticating..."));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection failed: " + e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void onGoToRegister() {
        if (navigator != null) {
            RegisterController controller = navigator.navigateToAndGetController(SceneNavigator.REGISTER);
            if (controller != null) {
                controller.setConnection(connection);
                controller.setNavigator(navigator);
                // Preserve server fields
                controller.setServerFields(
                    serverHostField.getText(),
                    serverPortField.getText()
                );
            }
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            System.err.println("[DEBUG] LoginController.onMessageReceived: " + message.getType());
            switch (message.getType()) {
                case AUTH_OK:
                    System.err.println("[DEBUG] AUTH_OK received. Params: " + message.getParamCount());
                    System.err.println("[DEBUG] navigator = " + navigator);
                    System.err.println("[DEBUG] connection = " + connection);
                    statusLabel.setText("Login successful!");
                    statusLabel.setStyle("-fx-text-fill: #5a9e5a; -fx-font-size: 12px;");
                    // Navigate to lobby
                    try {
                        if (navigator != null) {
                            System.err.println("[DEBUG] Calling navigateToAndGetController(LOBBY)...");
                            LobbyController controller = navigator.navigateToAndGetController(SceneNavigator.LOBBY);
                            System.err.println("[DEBUG] Got controller: " + controller);
                            if (controller != null) {
                                controller.setConnection(connection);
                                controller.setNavigator(navigator);
                                System.err.println("[DEBUG] Setting user info: " + message.getParam(0) + ", " + message.getParam(1));
                                controller.setUserInfo(
                                    message.getParam(0),
                                    message.getParam(1)
                                );
                                System.err.println("[DEBUG] Requesting room list...");
                                controller.requestRoomList();
                                System.err.println("[DEBUG] Navigation complete!");
                            } else {
                                System.err.println("[DEBUG] ERROR: controller is null after navigation");
                                statusLabel.setText("Error: lobby controller is null");
                                statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                                loginButton.setDisable(false);
                            }
                        } else {
                            System.err.println("[DEBUG] ERROR: navigator is null!");
                            statusLabel.setText("Error: navigator is null");
                            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                            loginButton.setDisable(false);
                        }
                    } catch (Exception e) {
                        System.err.println("[DEBUG] EXCEPTION during navigation: " + e.getClass().getName() + ": " + e.getMessage());
                        e.printStackTrace();
                        statusLabel.setText("Navigation error: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                        loginButton.setDisable(false);
                    }
                    break;

                case AUTH_FAIL:
                    statusLabel.setText("Authentication failed: " + message.getParam(0));
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    loginButton.setDisable(false);
                    break;

                default:
                    break;
            }
        });
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            statusLabel.setText("Connection to server lost");
            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            loginButton.setDisable(false);
        });
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> {
            statusLabel.setText("Error: " + error);
            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            loginButton.setDisable(false);
        });
    }
}
