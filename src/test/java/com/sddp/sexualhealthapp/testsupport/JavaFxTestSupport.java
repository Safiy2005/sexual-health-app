package com.sddp.sexualhealthapp.testsupport;

import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JavaFxTestSupport {

    private static final Object LOCK = new Object();
    private static volatile boolean initialized;
    private static volatile boolean available;
    private static volatile String unavailableReason = "JavaFX toolkit unavailable";

    private JavaFxTestSupport() {
    }

    public static void initialize() throws Exception {
        if (initialized) {
            return;
        }

        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            try {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.startup(latch::countDown);
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    unavailableReason = "Timed out starting JavaFX toolkit";
                } else {
                    available = true;
                }
            } catch (IllegalStateException alreadyStarted) {
                available = true;
            } catch (RuntimeException e) {
                unavailableReason = e.getMessage() == null
                        ? e.getClass().getSimpleName()
                        : e.getMessage();
            }

            initialized = true;
        }
    }

    public static void assumeAvailable() {
        Assumptions.assumeTrue(available, unavailableReason);
    }

    public static void runOnFxAndWait(Runnable action) throws Exception {
        initialize();
        assumeAvailable();

        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for FX thread");
    }
}
