package com.sddp.sexualhealthapp.settings.service;

import java.util.prefs.Preferences;

import com.sddp.sexualhealthapp.settings.model.DisplayMode;

public class DisplaySettingsService {
    private static final String PREF_NODE = "com.sddp.sexualhealthapp.display";
    private static final String KEY_DISPLAY_MODE = "display_mode";

    private static DisplaySettingsService instance;

    private final Preferences preferences;

    private DisplaySettingsService() {
        this.preferences = Preferences.userRoot().node(PREF_NODE);
    }

    public static DisplaySettingsService getInstance() {
        if (instance == null) {
            instance = new DisplaySettingsService();
        }
        return instance;
    }

    public DisplayMode getDisplayMode() {
        String stored = preferences.get(KEY_DISPLAY_MODE, DisplayMode.STANDARD.name());
        return DisplayMode.fromStoredValue(stored);
    }

    public void setDisplayMode(DisplayMode mode) {
        if (mode == null) {
            mode = DisplayMode.STANDARD;
        }
        preferences.put(KEY_DISPLAY_MODE, mode.name());
    }

    public void resetDisplayMode() {
        preferences.remove(KEY_DISPLAY_MODE);
    }
}