package com.sddp.sexualhealthapp.settings.service;

import java.util.prefs.Preferences;

import com.sddp.sexualhealthapp.settings.model.DyslexicFontMode;

/**
 * Persists the user's dyslexic-font choice using the {@link Preferences} API,
 * mirroring {@link TextSizeSettingsService} and {@link DisplaySettingsService}.
 *
 * <p>A brand-new Preferences node is used so old app builds that predate this
 * feature can coexist with this branch: they will never read or write this
 * key, and we default to {@link DyslexicFontMode#OFF} when the key is missing
 * so the first-run behaviour is unchanged.</p>
 */
public class DyslexicFontSettingsService {
    private static final String PREF_NODE = "com.sddp.sexualhealthapp.dyslexicfont";
    private static final String KEY_DYSLEXIC_FONT = "dyslexic_font_mode";

    private static DyslexicFontSettingsService instance;

    private final Preferences preferences;

    private DyslexicFontSettingsService() {
        this.preferences = Preferences.userRoot().node(PREF_NODE);
    }

    public static synchronized DyslexicFontSettingsService getInstance() {
        if (instance == null) {
            instance = new DyslexicFontSettingsService();
        }
        return instance;
    }

    public DyslexicFontMode getMode() {
        String stored = preferences.get(KEY_DYSLEXIC_FONT, DyslexicFontMode.OFF.name());
        return DyslexicFontMode.fromStoredValue(stored);
    }

    public void setMode(DyslexicFontMode mode) {
        if (mode == null) {
            mode = DyslexicFontMode.OFF;
        }
        preferences.put(KEY_DYSLEXIC_FONT, mode.name());
    }

    public void resetMode() {
        preferences.remove(KEY_DYSLEXIC_FONT);
    }

    public boolean isEnabled() {
        return getMode().isEnabled();
    }
}
