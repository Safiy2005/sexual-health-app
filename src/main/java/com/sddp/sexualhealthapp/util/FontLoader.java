package com.sddp.sexualhealthapp.util;

import java.io.InputStream;
import java.util.List;

import javafx.scene.text.Font;

/**
 * Loads custom font files bundled under {@code /fonts/} so JavaFX CSS can
 * reference them by family name in {@code -fx-font-family}.
 *
 * <p>Fonts must be registered before any CSS that references them is parsed,
 * so this is invoked once from {@link com.sddp.sexualhealthapp.SexualHealthApp}
 * at application startup, prior to scene construction.</p>
 *
 * <p>Missing files are tolerated: the app logs a warning and falls back to the
 * system font stack declared in the stylesheets.</p>
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public final class FontLoader {

    private static final double DEFAULT_LOAD_SIZE = 12.0;

    private static final List<String> OPEN_DYSLEXIC_RESOURCES = List.of(
            "/fonts/OpenDyslexic-Regular.otf",
            "/fonts/OpenDyslexic-Bold.otf",
            "/fonts/OpenDyslexic-Italic.otf",
            "/fonts/OpenDyslexic-BoldItalic.otf"
    );

    private static boolean loaded = false;

    private FontLoader() {
        throw new AssertionError("Cannot instantiate FontLoader class");
    }

    /**
     * Registers every bundled custom font with the JavaFX font system.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    public static synchronized void loadCustomFonts() {
        if (loaded) {
            return;
        }
        loaded = true;

        for (String resource : OPEN_DYSLEXIC_RESOURCES) {
            loadFontResource(resource);
        }
    }

    private static void loadFontResource(String resourcePath) {
        try (InputStream stream = FontLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                // Font file not bundled — keep running, CSS fallback stack covers this.
                System.err.println("[FontLoader] Font resource not found: " + resourcePath
                        + " (dyslexic-font toggle will fall back to system fonts)");
                return;
            }

            Font font = Font.loadFont(stream, DEFAULT_LOAD_SIZE);
            if (font == null) {
                System.err.println("[FontLoader] Font.loadFont returned null for " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[FontLoader] Failed to load font " + resourcePath + ": " + e.getMessage());
        }
    }
}
