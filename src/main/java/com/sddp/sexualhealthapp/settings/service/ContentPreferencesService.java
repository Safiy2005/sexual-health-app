package com.sddp.sexualhealthapp.settings.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * JSON-backed persistence for article content preferences.
 */
public class ContentPreferencesService {

    private static final String DATA_DIR = "src/main/resources/settings";
    private static final String CONTENT_PREFERENCES_FILE = "content-preferences.json";

    private static ContentPreferencesService instance;

    private final Path storageFilePath;
    private final Gson gson;
    private ContentPreferences preferences;

    public static synchronized ContentPreferencesService getInstance() {
        if (instance == null) {
            instance = new ContentPreferencesService();
        }
        return instance;
    }

    private ContentPreferencesService() {
        this(Paths.get(DATA_DIR, CONTENT_PREFERENCES_FILE));
    }

    public ContentPreferencesService(Path storageFilePath) {
        this.storageFilePath = storageFilePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.preferences = loadFromFile();
    }

    public synchronized ContentPreferences getPreferences() {
        return preferences;
    }

    public synchronized boolean updateBlockedTags(List<String> blockedTags) {
        return savePreferences(new ContentPreferences(blockedTags, preferences.preferredTags()));
    }

    public synchronized boolean updatePreferredTags(List<String> preferredTags) {
        return savePreferences(new ContentPreferences(preferences.blockedTags(),
                preferredTags));
    }

    public synchronized boolean savePreferences(ContentPreferences newPreferences) {
        preferences = newPreferences == null ? ContentPreferences.empty() : newPreferences;
        return saveToFile();
    }

    public synchronized void reloadFromDisk() {
        preferences = loadFromFile();
    }

    private ContentPreferences loadFromFile() {
        if (!Files.exists(storageFilePath)) {
            return ContentPreferences.empty();
        }

        try (Reader reader = Files.newBufferedReader(storageFilePath, StandardCharsets.UTF_8)) {
            ContentPreferences loaded = gson.fromJson(reader, ContentPreferences.class);
            return loaded == null ? ContentPreferences.empty() : loaded;
        } catch (IOException | RuntimeException e) {
            return ContentPreferences.empty();
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
