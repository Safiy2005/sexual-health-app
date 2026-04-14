package com.sddp.sexualhealthapp.settings.model;

/**
 * Persisted preferences for event reminder notifications.
 */
public record ReminderPreferences(
        VisibilityMode visibilityMode,
        String customDisguisedTitle,
        String customDisguisedBody) {

    public enum VisibilityMode {
        OFF, DISGUISED, DISCREET, EXPLICIT
    }

    // 1. Define the defaults exactly once here
    public static final String DEFAULT_TITLE = "App Updates";
    public static final String DEFAULT_BODY = "Background Calculator Update pending";

    // 2. The Compact Constructor intercepts nulls from GSON or empty inputs
    public ReminderPreferences {
        if (visibilityMode == null)
            visibilityMode = VisibilityMode.OFF;

        if (customDisguisedTitle == null || customDisguisedTitle.isBlank()) {
            customDisguisedTitle = DEFAULT_TITLE;
        }
        if (customDisguisedBody == null || customDisguisedBody.isBlank()) {
            customDisguisedBody = DEFAULT_BODY;
        }
    }

    // 3. The empty method just relies on the constructor logic
    public static ReminderPreferences empty() {
        return new ReminderPreferences(VisibilityMode.OFF, DEFAULT_TITLE, DEFAULT_BODY);
    }
}