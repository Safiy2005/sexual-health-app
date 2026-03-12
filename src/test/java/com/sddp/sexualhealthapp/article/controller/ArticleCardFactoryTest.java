package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ArticleCardFactoryTest {

    private static boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                javaFxAvailable = false;
            } else {
                javaFxAvailable = true;
            }
        } catch (IllegalStateException alreadyStarted) {
            // JavaFX already started
            javaFxAvailable = true;
        } catch (RuntimeException startupFailure) {
            javaFxAvailable = false;
        }
    }

    @Test
    void createRecentArticleCard_showsExpectedProgressTextAndBar() throws Exception {
        assumeJavaFxAvailable();

        Article article = new Article("""
                # Resume Test

                ## One
                A

                ## Two
                B

                ## Three
                C
                """);
        article.setFileName("resume-test.md");

        RecentlyReadEntry entry = new RecentlyReadEntry("resume-test.md", 1, Instant.parse("2026-03-11T09:00:00Z"));

        final VBox[] cardHolder = new VBox[1];
        runOnFxAndWait(() -> cardHolder[0] = ArticleCardFactory.createRecentArticleCard(article, entry, ignored -> {
        }));

        VBox card = cardHolder[0];
        assertNotNull(card);

        ProgressBar progressBar = findChild(card, ProgressBar.class);
        Label progressLabel = findLabel(card, "2 of 3 sections");

        assertNotNull(progressBar);
        assertEquals(2.0 / 3.0, progressBar.getProgress(), 0.001);
        assertTrue(progressBar.isMouseTransparent());
        assertNotNull(progressLabel);
    }

    private static <T extends Node> T findChild(VBox parent, Class<T> type) {
        for (Node child : parent.getChildren()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
        }
        return null;
    }

    private static Label findLabel(VBox parent, String text) {
        for (Node child : parent.getChildren()) {
            if (child instanceof Label label && text.equals(label.getText())) {
                return label;
            }
        }
        return null;
    }

    private static void runOnFxAndWait(Runnable action) throws Exception {
        assumeJavaFxAvailable();

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

    private static void assumeJavaFxAvailable() {
        Assumptions.assumeTrue(javaFxAvailable);
        try {
            Platform.isFxApplicationThread();
        } catch (RuntimeException noToolkit) {
            javaFxAvailable = false;
            Assumptions.assumeTrue(false);
        }
    }

}
