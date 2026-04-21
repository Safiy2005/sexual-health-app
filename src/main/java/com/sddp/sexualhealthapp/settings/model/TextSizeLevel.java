package com.sddp.sexualhealthapp.settings.model;

public enum TextSizeLevel {
    SMALL("Small"),
    STANDARD("Standard"),
    LARGE("Large"),
    EXTRA_LARGE("Extra Large");

    private final String displayName;

    TextSizeLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TextSizeLevel fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }

        try {
            return TextSizeLevel.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return STANDARD;
        }
    }
}