package com.sddp.sexualhealthapp.settings.model;

import java.util.List;

/**
 * Persisted article content preferences.
 */
public record ContentPreferences(
        List<String> blockedTags,
        List<String> preferredTags) {

    public ContentPreferences {
        blockedTags = blockedTags == null ? List.of() : List.copyOf(blockedTags);
        preferredTags = preferredTags == null ? List.of() : List.copyOf(preferredTags);
    }

    public static ContentPreferences empty() {
        return new ContentPreferences(List.of(), List.of());
    }
}
