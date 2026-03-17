package com.sddp.sexualhealthapp.settings.service;

import java.util.List;

import com.sddp.sexualhealthapp.settings.model.DisplayMode;

import javafx.scene.Parent;

public final class DisplayModeManager {
    private static final List<String> MODE_CLASSES = List.of(
            "theme-standard",
            "theme-dark",
            "theme-high-contrast"
    );

    private DisplayModeManager() {
    }

    public static void applyDisplayMode(Parent root, DisplayMode mode) {
        if (root == null) {
            return;
        }

        root.getStyleClass().removeAll(MODE_CLASSES);

        String styleClass = switch (mode) {
            case DARK -> "theme-dark";
            case HIGH_CONTRAST -> "theme-high-contrast";
            case STANDARD -> "theme-standard";
        };

        if (!root.getStyleClass().contains(styleClass)) {
            root.getStyleClass().add(styleClass);
        }
    }
}