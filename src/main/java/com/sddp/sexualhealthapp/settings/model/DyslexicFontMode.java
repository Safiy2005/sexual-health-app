package com.sddp.sexualhealthapp.settings.model;

/**
 * Whether the app should render text using the OpenDyslexic typeface.
 *
 * <p>Modelled as an enum (rather than a plain boolean) to mirror the shape of
 * {@link TextSizeLevel} and {@link DisplayMode}, which keeps the settings
 * package uniform and leaves room for future modes (e.g. OpenDyslexic 3) to be
 * added without changing the Preferences schema.</p>
 */
public enum DyslexicFontMode {
    OFF("Off"),
    ON("On");

    private final String displayName;

    DyslexicFontMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return this == ON;
    }

    /**
     * Parses a stored preference value. Falls back to {@link #OFF} on unknown
     * or blank input so first-run users (and old-app reads) match the
     * historical default.
     */
    public static DyslexicFontMode fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return OFF;
        }

        try {
            return DyslexicFontMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return OFF;
        }
    }

    public static DyslexicFontMode fromBoolean(boolean enabled) {
        return enabled ? ON : OFF;
    }
}
