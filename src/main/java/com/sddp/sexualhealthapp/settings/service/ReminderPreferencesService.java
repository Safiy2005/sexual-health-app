package com.sddp.sexualhealthapp.settings.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JSON-backed persistence for reminder notification preferences.
 */
public class ReminderPreferencesService {

    private static final String DATA_DIR = "src/main/resources/settings";
    private static final String REMINDER_PREFERENCES_FILE = "reminder-preferences.json";

    private static ReminderPreferencesService instance;

    private final Path storageFilePath;
    private final Gson gson;
    private ReminderPreferences preferences;

    public static synchronized ReminderPreferencesService getInstance() {
        if (instance == null) {
            instance = new ReminderPreferencesService();
        }
        return instance;
    }

    private ReminderPreferencesService() {
        this(Paths.get(DATA_DIR, REMINDER_PREFERENCES_FILE));
    }

    public ReminderPreferencesService(Path storageFilePath) {
        this.storageFilePath = storageFilePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.preferences = loadFromFile();
    }

    public synchronized ReminderPreferences getPreferences() {
        return preferences;
    }

    public synchronized boolean savePreferences(ReminderPreferences newPreferences) {
        preferences = newPreferences == null ? ReminderPreferences.empty() : newPreferences;
        return saveToFile();
    }

    public synchronized void reloadFromDisk() {
        preferences = loadFromFile();
    }

    private ReminderPreferences loadFromFile() {
        if (!Files.exists(storageFilePath)) {
            return ReminderPreferences.empty();
        }

        try (Reader reader = Files.newBufferedReader(storageFilePath, StandardCharsets.UTF_8)) {
            ReminderPreferences loaded = gson.fromJson(reader, ReminderPreferences.class);
            return loaded == null ? ReminderPreferences.empty() : loaded;
        } catch (IOException | RuntimeException e) {
            return ReminderPreferences.empty();
        }
    }

    private boolean saveToFile() {
        try {
            Path parent = storageFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(storageFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(preferences, writer);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}