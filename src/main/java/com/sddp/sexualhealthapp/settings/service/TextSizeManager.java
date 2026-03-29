package com.sddp.sexualhealthapp.settings.service;

import java.util.List;

import com.sddp.sexualhealthapp.settings.model.TextSizeLevel;

import javafx.scene.Parent;

public final class TextSizeManager {
    private static final List<String> SIZE_CLASSES = List.of(
            "text-size-small",
            "text-size-standard",
            "text-size-large",
            "text-size-extra-large"
    );

    private TextSizeManager() {
    }

    public static void applyTextSize(Parent root, TextSizeLevel level) {
        if (root == null) {
            return;
        }

        root.getStyleClass().removeAll(SIZE_CLASSES);

        String styleClass = switch (level) {
            case SMALL -> "text-size-small";
            case STANDARD -> "text-size-standard";
            case LARGE -> "text-size-large";
            case EXTRA_LARGE -> "text-size-extra-large";
        };

        if (!root.getStyleClass().contains(styleClass)) {
            root.getStyleClass().add(styleClass);
        }
    }
}