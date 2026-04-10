package com.sddp.sexualhealthapp.settings.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sddp.sexualhealthapp.settings.model.DisguisePreferences;
import java.io.IOException;
import java.nio.file.*;

public class DisguisePreferencesService {
    private static final String DEFAULT_PATH = "src/main/resources/settings/disguise-preferences.json";
    private static DisguisePreferencesService instance;

    private final Path storagePath;
    private DisguisePreferences preferences;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static synchronized DisguisePreferencesService getInstance() {
        if (instance == null) {
            instance = new DisguisePreferencesService(Paths.get(DEFAULT_PATH));
        }
        return instance;
    }

    private DisguisePreferencesService() {
        this(Paths.get(DEFAULT_PATH));
    }

    // added contructor that uses a path so tests can be run in temp json files
    public DisguisePreferencesService(Path customPath) {
        this.storagePath = customPath;
        this.preferences = load();
    }

    public DisguisePreferences getPreferences() { return preferences; }

    public void save(DisguisePreferences newPrefs) {
        this.preferences = newPrefs;
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            Files.writeString(storagePath, gson.toJson(preferences));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private DisguisePreferences load() {
        try {
            if (!Files.exists(storagePath)) return DisguisePreferences.defaultSettings();
            return gson.fromJson(Files.readString(storagePath), DisguisePreferences.class);
        } catch (Exception e) { return DisguisePreferences.defaultSettings(); }
    }
}