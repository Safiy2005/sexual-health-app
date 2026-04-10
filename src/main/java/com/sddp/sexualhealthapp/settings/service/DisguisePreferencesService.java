package com.sddp.sexualhealthapp.settings.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sddp.sexualhealthapp.settings.model.DisguisePreferences;
import java.io.IOException;
import java.nio.file.*;

public class DisguisePreferencesService {
    private static final String STORAGE_PATH = "src/main/resources/settings/disguise-preferences.json";
    private static DisguisePreferencesService instance;
    private DisguisePreferences preferences;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static synchronized DisguisePreferencesService getInstance() {
        if (instance == null) instance = new DisguisePreferencesService();
        return instance;
    }

    private DisguisePreferencesService() {
        this.preferences = load();
    }

    public DisguisePreferences getPreferences() { return preferences; }

    public void save(DisguisePreferences newPrefs) {
        this.preferences = newPrefs;
        try {
            Path path = Paths.get(STORAGE_PATH);
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(preferences));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private DisguisePreferences load() {
        try {
            Path path = Paths.get(STORAGE_PATH);
            if (!Files.exists(path)) return DisguisePreferences.defaultSettings();
            return gson.fromJson(Files.readString(path), DisguisePreferences.class);
        } catch (Exception e) { return DisguisePreferences.defaultSettings(); }
    }
}