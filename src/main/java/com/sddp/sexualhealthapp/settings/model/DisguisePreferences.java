package com.sddp.sexualhealthapp.settings.model;

public record DisguisePreferences (boolean calcDisguiseEnabled) {
    public static DisguisePreferences defaultSettings() {
        return new DisguisePreferences(true); // default is calc on
    }
}
