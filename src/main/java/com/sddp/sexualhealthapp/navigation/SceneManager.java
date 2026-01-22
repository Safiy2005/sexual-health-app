package com.sddp.sexualhealthapp.navigation;

import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class for managing scene transitions throughout the application.
 * Handles loading FXML files, caching scenes, and providing smooth transitions.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SceneManager {

    private static SceneManager instance;
    private Stage primaryStage;
    private final Map<String, Scene> sceneCache;
    private String currentSceneName;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private SceneManager() {
        this.sceneCache = new HashMap<>();
        this.currentSceneName = "";
    }

    /**
     * Gets the singleton instance of SceneManager.
     *
     * @return the SceneManager instance
     */
    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    /**
     * Initializes the SceneManager with the primary stage.
     * Must be called before using any transition methods.
     *
     * @param stage the primary stage of the application
     */
    public void initialize(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Loads a scene from an FXML file.
     * If the scene has already been loaded, returns the cached version.
     *
     * @param sceneName the name identifier of the scene
     * @param fxmlPath the path to the FXML file
     * @return the loaded Scene, or null if loading fails
     */
    public Scene loadScene(String sceneName, String fxmlPath) {
        // Check cache first
        if (sceneCache.containsKey(sceneName)) {
            return sceneCache.get(sceneName);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Apply CSS if it's the calculator scene
            if (sceneName.equals(AppConstants.SCENE_CALCULATOR)) {
                root.getStylesheets().add(getClass().getResource(AppConstants.CALCULATOR_CSS).toExternalForm());
            }

            Scene scene = new Scene(root);
            sceneCache.put(sceneName, scene);
            return scene;

        } catch (IOException e) {
            System.err.println("Failed to load scene " + sceneName + " from " + fxmlPath);
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error loading scene " + sceneName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Transitions to the calculator scene.
     */
    public void transitionToCalculator() {
        transitionToScene(
            AppConstants.SCENE_CALCULATOR,
            AppConstants.CALCULATOR_FXML
        );
    }

    /**
     * Transitions to the setup wizard scene.
     */
    public void transitionToSetup() {
        transitionToScene(
            AppConstants.SCENE_SETUP,
            AppConstants.SETUP_FXML
        );
    }

    /**
     * Transitions to the main application scene.
     * Includes a brief delay for discretion.
     */
    public void transitionToMainApp() {
        // Add a brief delay to make the transition less obvious
        new Thread(() -> {
            try {
                Thread.sleep(AppConstants.TRANSITION_DELAY_MS);
                javafx.application.Platform.runLater(() -> {
                    transitionToScene(
                        AppConstants.SCENE_MAIN_APP,
                        AppConstants.MAIN_APP_FXML
                    );
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Generic method to transition to any scene.
     *
     * @param sceneName the name identifier of the scene
     * @param fxmlPath the path to the FXML file
     */
    private void transitionToScene(String sceneName, String fxmlPath) {
        if (primaryStage == null) {
            throw new IllegalStateException("SceneManager not initialized. Call initialize() first.");
        }

        Scene scene = loadScene(sceneName, fxmlPath);
        if (scene == null) {
            System.err.println("Failed to transition to scene: " + sceneName);
            return;
        }

        // Apply fade transition if not the first scene
        if (!currentSceneName.isEmpty()) {
            applyFadeTransition(scene);
        } else {
            primaryStage.setScene(scene);
        }

        currentSceneName = sceneName;
    }

    /**
     * Applies a fade transition effect when changing scenes.
     *
     * @param newScene the new scene to transition to
     */
    private void applyFadeTransition(Scene newScene) {
        Scene currentScene = primaryStage.getScene();

        if (currentScene != null && currentScene.getRoot() != null) {
            // Fade out current scene
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentScene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(event -> {
                // Switch to new scene
                primaryStage.setScene(newScene);

                // Fade in new scene
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newScene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();
        } else {
            // No current scene, just set the new one
            primaryStage.setScene(newScene);
        }
    }

    /**
     * Gets the name of the current scene.
     *
     * @return the current scene name
     */
    public String getCurrentSceneName() {
        return currentSceneName;
    }

    /**
     * Clears the scene cache, forcing scenes to be reloaded on next access.
     * Useful for development and testing.
     */
    public void clearCache() {
        sceneCache.clear();
    }

    /**
     * Clears a specific scene from the cache.
     *
     * @param sceneName the name of the scene to clear
     */
    public void clearScene(String sceneName) {
        sceneCache.remove(sceneName);
    }

    /**
     * Gets the primary stage.
     *
     * @return the primary stage, or null if not initialized
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
