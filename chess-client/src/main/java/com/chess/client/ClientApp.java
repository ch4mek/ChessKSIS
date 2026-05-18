package com.chess.client;

import com.chess.client.gui.SceneNavigator;
import com.chess.client.gui.controller.LoginController;
import com.chess.client.net.ServerConnection;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the Chess Client application.
 * Initializes the JavaFX stage, scene navigator, and server connection.
 */
public class ClientApp extends Application {

    private ServerConnection connection;

    @Override
    public void start(Stage primaryStage) {
        connection = new ServerConnection();

        SceneNavigator navigator = new SceneNavigator(primaryStage, 800, 600);

        // Preload all views
        navigator.preloadView(SceneNavigator.LOGIN, "/fxml/login.fxml");
        navigator.preloadView(SceneNavigator.REGISTER, "/fxml/register.fxml");
        navigator.preloadView(SceneNavigator.LOBBY, "/fxml/lobby.fxml");
        navigator.preloadView(SceneNavigator.GAME, "/fxml/game.fxml");

        // Navigate to login screen
        LoginController controller = navigator.navigateToAndGetController(SceneNavigator.LOGIN);
        if (controller != null) {
            controller.setConnection(connection);
            controller.setNavigator(navigator);
        }

        primaryStage.setTitle("ChessKSiS — Online Chess");
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Clean up connection on application exit
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
