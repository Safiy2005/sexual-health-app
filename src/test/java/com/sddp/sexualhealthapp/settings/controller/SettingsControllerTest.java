package com.sddp.sexualhealthapp.settings.controller;

import com.sddp.sexualhealthapp.security.SecureStorage;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.settings.service.ParentalControlsPinService;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class SettingsControllerTest {

    private static boolean fxStarted;
    private Path tempFile;

    @BeforeAll
    static void initJavaFx() throws Exception {
        if (fxStarted) {
            return;
        }

        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFx Platform failed to start");
            }
        } catch (IllegalStateException alreadyStarted) {
            // already started
        }
        fxStarted = true;
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void settingsHome_rendersCards_andCardOpensDetailPage() throws Exception {
        tempFile = Files.createTempFile("settings-controller-", ".json");
        SettingsController controller = new SettingsController(
                new ContentPreferencesService(tempFile),
                () -> java.util.List.of("STIs", "LGBTQ+", "Mental Health & Wellbeing"),
                new ParentalControlsPinService(new NoPinSecureStorage()));

        runOnFxAndWait(() -> {
            try {
                inject(controller, "settingsHomeView", new VBox());
                inject(controller, "settingsDetailView", new VBox());
                inject(controller, "settingsCardContainer", new VBox());
                inject(controller, "settingsDetailTitle", new Label());
                inject(controller, "settingsDetailContent", new VBox());
                inject(controller, "settingsHomeScrollPane", new ScrollPane());
                inject(controller, "settingsDetailScrollPane", new ScrollPane());

                Method initialize = SettingsController.class.getDeclaredMethod("initialize");
                initialize.setAccessible(true);
                initialize.invoke(controller);

                VBox settingsCardContainer = get(controller, "settingsCardContainer", VBox.class);
                VBox homeView = get(controller, "settingsHomeView", VBox.class);
                VBox detailView = get(controller, "settingsDetailView", VBox.class);

                assertFalse(settingsCardContainer.getChildren().isEmpty());
                assertTrue(homeView.isVisible());
                assertFalse(detailView.isVisible());

                VBox firstCard = (VBox) settingsCardContainer.getChildren().get(0);
                firstCard.getOnMouseClicked().handle(null);

                Label detailTitle = get(controller, "settingsDetailTitle", Label.class);
                assertEquals("Content preferences", detailTitle.getText());
                assertFalse(homeView.isVisible());
                assertTrue(detailView.isVisible());

                Method back = SettingsController.class.getDeclaredMethod("handleBackToSettingsHome");
                back.setAccessible(true);
                back.invoke(controller);

                assertTrue(homeView.isVisible());
                assertFalse(detailView.isVisible());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T get(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void runOnFxAndWait(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for JavaFx action");
        }
    }

    /**
     * SecureStorage double that reports "no PIN ever set". Used so the
     * controller never opens the real PIN prompt during this rendering test,
     * and so we don't touch the developer's actual OS preferences.
     * We pass a null Preferences into the parent so its no-arg constructor
     * (which hits Preferences.userRoot()) is skipped.
     */
    private static final class NoPinSecureStorage extends SecureStorage {
        NoPinSecureStorage() {
            super((Preferences) null);
        }

        @Override
        public boolean exists(String key) {
            return false;
        }

        @Override
        public boolean verifyHash(String key, String plainValue) {
            return false;
        }

        @Override
        public boolean saveHashed(String key, String plainValue) {
            return true;
        }

        @Override
        public boolean delete(String key) {
            return true;
        }
    }
}
