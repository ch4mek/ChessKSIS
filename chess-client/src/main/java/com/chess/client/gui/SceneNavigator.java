package com.chess.client.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneNavigator {

    private static final Logger LOGGER = Logger.getLogger(SceneNavigator.class.getName());

    public static final String LOGIN = "login";
    public static final String REGISTER = "register";
    public static final String LOBBY = "lobby";
    public static final String GAME = "game";

    private final Stage stage;
    private final Map<String, FXMLLoader> loaders = new HashMap<>();
    private final double width;
    private final double height;

    public SceneNavigator(Stage stage, double width, double height) {
        this.stage = stage;
        this.width = width;
        this.height = height;
    }

    public void preloadView(String name, String fxmlPath) {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            LOGGER.severe("FXML resource not found: " + fxmlPath);
            throw new RuntimeException("FXML resource not found: " + fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        loaders.put(name, loader);
        LOGGER.fine("Preloaded view: " + name + " from " + fxmlPath);
    }

    public void navigateTo(String name) {
        try {
            FXMLLoader loader = loaders.get(name);
            if (loader == null) {
                throw new RuntimeException("View not preloaded: " + name);
            }

            Parent root = loader.load();
            Scene scene = stage.getScene();

            if (scene == null) {
                scene = new Scene(root, width, height);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("ChessKSiS — " + name.substring(0, 1).toUpperCase() + name.substring(1));
            stage.sizeToScene();
            stage.show();

            LOGGER.info("Navigated to: " + name);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to " + name, e);
            throw new RuntimeException("Failed to navigate to " + name, e);
        }
    }

    public <T> T navigateToAndGetController(String name) {
        try {
            System.err.println("[DEBUG] SceneNavigator.navigateToAndGetController('" + name + "') called");
            FXMLLoader originalLoader = loaders.get(name);
            if (originalLoader == null) {
                System.err.println("[DEBUG] ERROR: View not preloaded: " + name);
                System.err.println("[DEBUG] Available loaders: " + loaders.keySet());
                throw new RuntimeException("View not preloaded: " + name);
            }

            System.err.println("[DEBUG] Original loader location: " + originalLoader.getLocation());

            FXMLLoader freshLoader = new FXMLLoader(originalLoader.getLocation());
            System.err.println("[DEBUG] Fresh loader created, calling load()...");
            Parent root = freshLoader.load();
            System.err.println("[DEBUG] FXML loaded successfully. Root: " + root.getClass().getName());

            Scene scene = stage.getScene();
            System.err.println("[DEBUG] Current scene: " + scene);

            if (scene == null) {
                scene = new Scene(root, width, height);
                stage.setScene(scene);
                System.err.println("[DEBUG] Created new scene");
            } else {
                scene.setRoot(root);
                System.err.println("[DEBUG] Set root on existing scene");
            }

            stage.setTitle("ChessKSiS — " + name.substring(0, 1).toUpperCase() + name.substring(1));
            stage.sizeToScene();
            stage.show();

            loaders.put(name, freshLoader);

            T controller = freshLoader.getController();
            System.err.println("[DEBUG] Controller: " + controller);
            System.err.println("[DEBUG] Navigation to '" + name + "' COMPLETE");

            LOGGER.info("Navigated to: " + name);
            return controller;
        } catch (IOException e) {
            System.err.println("[DEBUG] IOException during navigation to '" + name + "': " + e.getMessage());
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Failed to navigate to " + name, e);
            throw new RuntimeException("Failed to navigate to " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getController(String name) {
        FXMLLoader loader = loaders.get(name);
        if (loader != null && loader.getController() != null) {
            return (T) loader.getController();
        }
        return null;
    }

    public Stage getStage() {
        return stage;
    }
}
