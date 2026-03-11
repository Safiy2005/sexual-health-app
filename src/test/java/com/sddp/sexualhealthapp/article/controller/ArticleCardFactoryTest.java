package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.testsupport.JavaFxTestSupport;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ArticleCardFactoryTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestSupport.initialize();
    }

    @Test
    void createRecentArticleCard_showsExpectedProgressTextAndBar() throws Exception {
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
        JavaFxTestSupport.runOnFxAndWait(() -> cardHolder[0] = ArticleCardFactory.createRecentArticleCard(article, entry, ignored -> {
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

}
