package com.sddp.sexualhealthapp;

import com.sddp.sexualhealthapp.article.service.ArticleBrowseRankingService;
import com.sddp.sexualhealthapp.article.service.ArticleServiceRegistry;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.settings.service.DisguisePreferencesService;
import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class for the Sexual Health App.
 * This app uses a calculator disguise for discretion - the calculator serves as
 * the entry point and authentication mechanism.
 *
 * <p>
 * On first launch, users are guided to set up a "secret equation" (e.g.,
 * 5+3=8).
 * On subsequent launches, the app opens as a fully functional calculator.
 * When the user enters their secret equation in the calculator, the app unlocks
 * and transitions to the main sexual health features.
 * </p>
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
        boolean hasSecretEquation = authService.hasSecretEquation();
        // check the disguise setting
        boolean isDisguiseEnabled = DisguisePreferencesService.getInstance().getPreferences().calcDisguiseEnabled();

        // Start background warm-up as early as possible so the unlock path can
        // reuse already-prepared article/search state.
        Thread preloadThread = new Thread(ArticleServiceRegistry::preloadSearchInfrastructure);
        preloadThread.setDaemon(true);
        preloadThread.start();

        Thread articleWarmupThread = new Thread(ArticleBrowseRankingService::preload);
        articleWarmupThread.setDaemon(true);
        articleWarmupThread.start();

        if (!hasSecretEquation) {
            // first time user
            SceneManager.getInstance().transitionToSetup();
        } else if (isDisguiseEnabled) {
            // setting enabled, repeat user
            SceneManager.getInstance().transitionToCalculator();
        } else {
            // setting disabled
            SceneManager.getInstance().transitionToMainApp();
        }

        // Configure the window
        primaryStage.setTitle(AppConstants.APP_TITLE);
        primaryStage.setResizable(false);
        primaryStage.show();
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
