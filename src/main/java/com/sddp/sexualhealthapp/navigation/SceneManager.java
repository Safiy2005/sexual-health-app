package com.sddp.sexualhealthapp.navigation;

import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class for managing scene transitions throughout the application.
 * <p>
 * Uses a single persistent {@link Scene} for the lifetime of the app and swaps
 * only the root node. Transitions crossfade between the live old and new root
 * nodes — no snapshots, no scene swapping, no flashes.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SceneManager {

    /** App background colour, used as Scene fill to prevent flashes. */
    private static final Color APP_BG = Color.web("#1C1C1E");

    private static SceneManager instance;
    private Stage primaryStage;

    /** The single Scene that stays attached to the Stage for the app's lifetime. */
    private Scene persistentScene;

    /** Cached FXML roots keyed by scene name. */
    private final Map<String, Parent> rootCache;

    private String currentSceneName;
    private boolean isTransitioning = false;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private SceneManager() {
        this.rootCache = new HashMap<>();
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
     * Creates the single persistent Scene that will be used for the app's lifetime.
     * Must be called before using any transition methods.
     *
     * @param stage the primary stage of the application
     */
    public void initialize(Stage stage) {
        this.primaryStage = stage;

        StackPane placeholder = new StackPane();
        placeholder.setStyle("-fx-background-color: #1C1C1E;");
        this.persistentScene = new Scene(placeholder, AppConstants.APP_WIDTH, AppConstants.APP_HEIGHT);
        this.persistentScene.setFill(APP_BG);
        this.primaryStage.setScene(persistentScene);
    }

    /**
     * Returns whether a scene transition animation is currently in progress.
     * Controllers can use this to guard against double-triggers.
     *
     * @return true if a transition is currently animating
     */
    public boolean isTransitioning() {
        return isTransitioning;
    }

    /**
     * Loads an FXML root node, applying the app stylesheet.
     * Returns a cached root if already loaded for the given scene name.
     *
     * @param sceneName the name identifier for caching
     * @param fxmlPath  the path to the FXML file
     * @return the loaded Parent root, or null if loading fails
     */
    private Parent loadRoot(String sceneName, String fxmlPath) {
        if (rootCache.containsKey(sceneName)) {
            return rootCache.get(sceneName);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            root.getStylesheets().add(
                getClass().getResource(AppConstants.CSS_STYLESHEET).toExternalForm());
            rootCache.put(sceneName, root);
            return root;

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
     * Transitions to the calculator scene with the default crossfade duration.
     * Clears the setup scene from cache to ensure fresh state on next reset.
     */
    public void transitionToCalculator() {
        transitionToCalculator(AppConstants.SCENE_CROSSFADE_MS);
    }

    /**
     * Transitions to the calculator scene with a custom crossfade duration.
     *
     * @param durationMs the crossfade duration in milliseconds
     */
    public void transitionToCalculator(int durationMs) {
        clearScene(AppConstants.SCENE_SETUP);

        crossfadeToRoot(
            AppConstants.SCENE_CALCULATOR,
            AppConstants.CALCULATOR_FXML,
            durationMs
        );
    }

    /**
     * Transitions to the setup scene (onboarding tutorial + calculator setup).
     * Clears the setup scene from cache so the carousel always restarts
     * at the first onboarding page, even after a reset-code flow.
     */
    public void transitionToSetup() {
        clearScene(AppConstants.SCENE_SETUP);

        crossfadeToRoot(
            AppConstants.SCENE_SETUP,
            AppConstants.SETUP_FXML,
            AppConstants.SCENE_CROSSFADE_MS
        );
    }

    /**
     * Transitions to the main application scene.
     * <p>
     * Pre-loads the main app root synchronously before the discretion delay.
     * On first call this is heavy (FXML + controller init + article loading),
     * but it happens while the calculator is still showing — the user just
     * pressed '=' and expects a brief computation pause. By the time the
     * delay finishes and the crossfade starts, the root is cached and the
     * FX thread is free to render every frame of the animation.
     */
    public void transitionToMainApp() {
        if (isTransitioning) return;
        isTransitioning = true;

        // Pre-load now so the heavy FXML init doesn't block the fade later.
        loadRoot(AppConstants.SCENE_MAIN_APP, AppConstants.MAIN_APP_FXML);

        PauseTransition delay = new PauseTransition(
            Duration.millis(AppConstants.TRANSITION_DELAY_MS)
        );
        delay.setOnFinished(e -> {
            isTransitioning = false;
            crossfadeToRoot(
                AppConstants.SCENE_MAIN_APP,
                AppConstants.MAIN_APP_FXML,
                AppConstants.REVEAL_FADE_MS
            );
        });
        delay.play();
    }

    /**
     * Performs a live-node crossfade between the current root and a new root.
     * <p>
     * Rather than taking a snapshot (which can have DPI/sizing issues), this
     * method keeps the real old root node in a temporary StackPane wrapper
     * and fades the new root in on top of it. Because both are real nodes
     * at their natural layout size, there are no dimension mismatches or
     * rendering artefacts.
     * <ol>
     *   <li>Load the target root</li>
     *   <li>Place both [oldRoot, newRoot] in a StackPane wrapper</li>
     *   <li>Start newRoot at opacity 0, fade to 1</li>
     *   <li>On finish, set the target root as the sole scene root</li>
     * </ol>
     *
     * @param sceneName  the target scene identifier
     * @param fxmlPath   the FXML path for the target root
     * @param durationMs the fade duration in milliseconds
     */
    private void crossfadeToRoot(String sceneName, String fxmlPath, int durationMs) {
        if (isTransitioning) return;
        if (primaryStage == null) {
            throw new IllegalStateException("SceneManager not initialized. Call initialize() first.");
        }

        isTransitioning = true;

        Parent currentRoot = persistentScene.getRoot();

        // Load the target root
        Parent targetRoot = loadRoot(sceneName, fxmlPath);
        if (targetRoot == null) {
            System.err.println("Failed to transition to scene: " + sceneName);
            isTransitioning = false;
            return;
        }

        currentSceneName = sceneName;

        // If already showing this root (e.g. double-trigger), just finish
        if (currentRoot == targetRoot) {
            isTransitioning = false;
            return;
        }

        // Start the new root fully transparent
        targetRoot.setOpacity(0.0);

        // Block input on the old root during the fade
        currentRoot.setMouseTransparent(true);

        // Wrapper holds both roots: old underneath, new on top
        StackPane wrapper = new StackPane(currentRoot, targetRoot);
        wrapper.setPrefSize(AppConstants.APP_WIDTH, AppConstants.APP_HEIGHT);
        wrapper.setMaxSize(AppConstants.APP_WIDTH, AppConstants.APP_HEIGHT);
        wrapper.setMinSize(AppConstants.APP_WIDTH, AppConstants.APP_HEIGHT);
        persistentScene.setRoot(wrapper);

        // Force CSS and layout before the first animation frame so freshly
        // loaded roots render with correct styling (not default white).
        wrapper.applyCss();
        wrapper.layout();

        // Fade the new root in over the old one
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), targetRoot);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setOnFinished(e -> {
            // Clean up: restore old root state, install new root as sole scene root
            currentRoot.setMouseTransparent(false);
            currentRoot.setOpacity(1.0);
            wrapper.getChildren().clear();
            persistentScene.setRoot(targetRoot);
            isTransitioning = false;
        });
        fade.play();
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
     * Clears the root cache, forcing roots to be reloaded on next access.
     * Useful for development and testing.
     */
    public void clearCache() {
        rootCache.clear();
    }

    /**
     * Clears a specific root from the cache.
     *
     * @param sceneName the name of the scene to clear
     */
    public void clearScene(String sceneName) {
        rootCache.remove(sceneName);
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
