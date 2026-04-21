package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.settings.service.DisplaySettingsService;
import com.sddp.sexualhealthapp.settings.service.ParentalControlsPinService;
import com.sddp.sexualhealthapp.settings.service.ReminderPreferencesService;
import com.sddp.sexualhealthapp.settings.service.TextSizeSettingsService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Service responsible for completely wiping all local application data
 * and resetting the application state.
 */
public class AppResetService {

    public static void wipeAllDataAndReset() {
        try {
            // delete persistent files. add here when making settings etc
            Files.deleteIfExists(Paths.get("src/main/resources/calendarevents/events.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/settings/content-preferences.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/settings/reminder-preferences.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/settings/disguise-preferences.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/article-state/recently-read.json"));

            NotificationService.clearAllTasks();

            // wipe user settings stored via java.util.prefs. add here when new
            // Preferences-backed settings are added
            DisplaySettingsService.getInstance().resetDisplayMode();
            TextSizeSettingsService.getInstance().resetTextSizeLevel();

            // delete passcode
            com.sddp.sexualhealthapp.calculator.service.SecretAuthService authService = new com.sddp.sexualhealthapp.calculator.service.SecretAuthService();
            authService.deleteSecretEquation();

            // delete parental controls PIN
            ParentalControlsPinService.getInstance().removePinIfPresent();

            // reset singletons and services that might cache
            EventStorageService.getInstance().reloadFromDisk();
            ContentPreferencesService.getInstance().reloadFromDisk();
            ReminderPreferencesService.getInstance().reloadFromDisk();
            new com.sddp.sexualhealthapp.article.service.RecentlyReadService().reloadFromDisk();
            SceneManager.getInstance().clearCache();

            SceneManager.getInstance().transitionToSetup();

        } catch (IOException ex) {
            System.err.println("CRITICAL: Failed to completely wipe data files: " + ex.getMessage());
        }
    }
}