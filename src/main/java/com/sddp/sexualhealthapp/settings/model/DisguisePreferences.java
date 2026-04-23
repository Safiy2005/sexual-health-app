package com.sddp.sexualhealthapp.settings.model;

public record DisguisePreferences(boolean calcDisguiseEnabled, boolean returnToCalculatorOnLock) {
    public static DisguisePreferences defaultSettings() {
        // Defaults: Calculator disguise is ON, and Lock button returns to calculator (true)
        return new DisguisePreferences(true, true);
    }
}