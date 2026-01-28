package com.sddp.sexualhealthapp.navigation;

import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

            // Apply CSS to all scenes (calculator, setup, main app)
            root.getStylesheets().add(getClass().getResource(AppConstants.CSS_STYLESHEET).toExternalForm());

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
     * Clears the setup scene from cache to ensure fresh state on next reset.
     */
    public void transitionToCalculator() {
        // Clear setup scene from cache so it's fresh on next use
        clearScene(AppConstants.SCENE_SETUP);

        transitionToScene(
            AppConstants.SCENE_CALCULATOR,
            AppConstants.CALCULATOR_FXML
        );
    }

    /**
     * Transitions to the setup scene (onboarding tutorial + calculator setup).
     * Clears the setup scene from cache so the carousel always restarts
     * at the first onboarding page, even after a reset-code flow.
     */
    public void transitionToSetup() {
        clearScene(AppConstants.SCENE_SETUP);

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
        
        primaryStage.setScene(scene);

        currentSceneName = sceneName;
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
