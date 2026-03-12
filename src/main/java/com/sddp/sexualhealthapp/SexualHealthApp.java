package com.sddp.sexualhealthapp;

import com.sddp.sexualhealthapp.article.service.SemanticSearchService;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class for the Sexual Health App.
 * This app uses a calculator disguise for discretion - the calculator serves as
 * the entry point and authentication mechanism.
 *
 * <p>On first launch, users are guided to set up a "secret equation" (e.g., 5+3=8).
 * On subsequent launches, the app opens as a fully functional calculator.
 * When the user enters their secret equation in the calculator, the app unlocks
 * and transitions to the main sexual health features.</p>
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SexualHealthApp extends Application {

    /**
     * The main entry point for the JavaFX application.
     * Initializes the scene manager and determines whether to show the setup wizard
     * (first-time users) or the calculator (returning users).
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        // Initialize the scene manager with the primary stage
        SceneManager.getInstance().initialize(primaryStage);

        // Check if a secret equation has been set up
        SecretAuthService authService = new SecretAuthService();
        if (authService.hasSecretEquation()) {
            // Returning user - go straight to calculator
            SceneManager.getInstance().transitionToCalculator();
        } else {
            // First-time user - show onboarding + setup
            SceneManager.getInstance().transitionToSetup();
        }

        // Configure the window
        primaryStage.setTitle(AppConstants.APP_TITLE);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Pre-load the ONNX embedding model in the background so first search is fast
        Thread preloadThread = new Thread(() -> new SemanticSearchService().preload());
        preloadThread.setDaemon(true);
        preloadThread.start();
    }

    @Override
    public void stop() throws Exception {
        // Shut down the notification background thread so the app can fully close
        com.sddp.sexualhealthapp.util.NotificationService.shutdown();

        // Always call the super method
        super.stop();
    }

    /**
     * Main method to launch the application.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
