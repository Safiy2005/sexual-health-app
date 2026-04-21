package com.sddp.sexualhealthapp.settings.service;

import java.util.prefs.Preferences;

import com.sddp.sexualhealthapp.settings.model.TextSizeLevel;

public class TextSizeSettingsService {
    private static final String PREF_NODE = "com.sddp.sexualhealthapp.textsize";
    private static final String KEY_TEXT_SIZE = "text_size_level";

    private static TextSizeSettingsService instance;

    private final Preferences preferences;

    private TextSizeSettingsService() {
        this.preferences = Preferences.userRoot().node(PREF_NODE);
    }

    public static TextSizeSettingsService getInstance() {
        if (instance == null) {
            instance = new TextSizeSettingsService();
        }
        return instance;
    }

    public TextSizeLevel getTextSizeLevel() {
        String stored = preferences.get(KEY_TEXT_SIZE, TextSizeLevel.STANDARD.name());
        return TextSizeLevel.fromStoredValue(stored);
    }

    public void setTextSizeLevel(TextSizeLevel level) {
        if (level == null) {
            level = TextSizeLevel.STANDARD;
        }
        preferences.put(KEY_TEXT_SIZE, level.name());
    }

    public void resetTextSizeLevel() {
        preferences.remove(KEY_TEXT_SIZE);
    }
}