package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.settings.service.ReminderPreferencesService;

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
            // 1. Physically delete all persistent storage files
            Files.deleteIfExists(Paths.get("src/main/resources/calendarevents/events.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/settings/content-preferences.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/settings/reminder-preferences.json"));
            Files.deleteIfExists(Paths.get("src/main/resources/settings/disguise-preferences.json"));

            // 2. Kill all background notification timers instantly
            NotificationService.clearAllTasks();

            // 3. Force the Singleton Services to reload their state from the now-deleted files (defaults to empty)
            EventStorageService.getInstance().reloadFromDisk();
            ContentPreferencesService.getInstance().reloadFromDisk();
            ReminderPreferencesService.getInstance().reloadFromDisk();

            // 4. Send the user back to the setup wizard
            SceneManager.getInstance().transitionToSetup();

        } catch (IOException ex) {
            System.err.println("CRITICAL: Failed to completely wipe data files: " + ex.getMessage());
            // Optionally, show a JavaFX Alert to the user here that the wipe failed
        }
    }
}