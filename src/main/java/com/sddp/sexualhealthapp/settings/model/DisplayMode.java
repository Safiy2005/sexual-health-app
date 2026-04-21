package com.sddp.sexualhealthapp.settings.model;

public enum DisplayMode {
    STANDARD("Standard"),
    DARK("Dark"),
    HIGH_CONTRAST("High Contrast");

    private final String displayName;

    DisplayMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DisplayMode fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }

        try {
            return DisplayMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return STANDARD;
        }
    }
}