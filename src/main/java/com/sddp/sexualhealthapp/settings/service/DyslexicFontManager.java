package com.sddp.sexualhealthapp.settings.service;

import com.sddp.sexualhealthapp.settings.model.DyslexicFontMode;

import javafx.scene.Parent;

/**
 * Toggles the single CSS hook ({@value #STYLE_CLASS}) on a scene root so the
 * universal rule in {@code accessibility.css} can swap every node's
 * {@code -fx-font-family} to OpenDyslexic.
 *
 * <p>Mirrors the pattern used by {@link TextSizeManager} and
 * {@link DisplayModeManager}: add/remove a style class on the root, let CSS
 * handle propagation. Keeping this logic in one place means future developers
 * adding new FXML screens don't need to remember anything; the style class on
 * the scene root is the only integration point.</p>
 */
public final class DyslexicFontManager {

    /** Style class that activates the global OpenDyslexic override in CSS. */
    public static final String STYLE_CLASS = "dyslexic-font";

    private DyslexicFontManager() {
    }

    public static void applyMode(Parent root, DyslexicFontMode mode) {
        if (root == null) {
            return;
        }

        boolean shouldBeOn = mode != null && mode.isEnabled();
        boolean isOn = root.getStyleClass().contains(STYLE_CLASS);

        if (shouldBeOn && !isOn) {
            root.getStyleClass().add(STYLE_CLASS);
        } else if (!shouldBeOn && isOn) {
            root.getStyleClass().remove(STYLE_CLASS);
        }
    }
}
