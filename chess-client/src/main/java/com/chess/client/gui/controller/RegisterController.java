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

public class RegisterController implements MessageListener {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    private ServerConnection connection;
    private SceneNavigator navigator;
    private String serverHost;
    private String serverPort;

    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    public void setServerFields(String host, String port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void onRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Заполните все поля");
            return;
        }

        if (username.length() < 3) {
            statusLabel.setText("Имя пользователя должно содержать минимум 3 символа");
            return;
        }

        if (password.length() < 4) {
            statusLabel.setText("Пароль должен содержать минимум 4 символа");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Пароли не совпадают");
            return;
        }

        registerButton.setDisable(true);
        statusLabel.setText("Подключение к серверу...");
        statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        new Thread(() -> {
            try {
                if (!connection.isConnected()) {
                    int port = Integer.parseInt(serverPort);
                    connection.connect(serverHost, port);
                }
                connection.setListener(this);
                connection.sendMessage(new Message(MessageType.REGISTER, username, password));

                Platform.runLater(() -> {
                    statusLabel.setText("Регистрация...");
                    statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка подключения: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    registerButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void onBackToLogin() {
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
                case REGISTER_OK:
                    statusLabel.setText("Регистрация успешна! Теперь вы можете войти.");
                    statusLabel.setStyle("-fx-text-fill: #5a9e5a; -fx-font-size: 12px;");
                    registerButton.setDisable(false);
                    break;

                case REGISTER_FAIL:
                    statusLabel.setText("Ошибка регистрации: " + message.getParam(0));
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    registerButton.setDisable(false);
                    break;

                default:
                    break;
            }
        });
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            statusLabel.setText("Соединение с сервером потеряно");
            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            registerButton.setDisable(false);
        });
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> {
            statusLabel.setText("Ошибка: " + error);
            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            registerButton.setDisable(false);
        });
    }
}
