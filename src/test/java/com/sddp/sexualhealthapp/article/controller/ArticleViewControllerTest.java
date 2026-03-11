package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.testsupport.JavaFxTestSupport;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ArticleViewControllerTest {

    private ArticleViewController controller;

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestSupport.initialize();
    }

    @BeforeEach
    void setUp() throws Exception {
        JavaFxTestSupport.assumeAvailable();
        controller = new ArticleViewController();
        inject(controller, "articlePageContainer", new StackPane());
        inject(controller, "pageIndicatorContainer", new HBox());
        inject(controller, "pageCounterLabel", new Label());
        inject(controller, "navMenuButton", new Button());
        inject(controller, "navMenuOverlay", new VBox());
        inject(controller, "navMenuBackdrop", new VBox());
        inject(controller, "navMenuScroll", new ScrollPane());
        inject(controller, "navMenuContent", new VBox());
        inject(controller, "leftArrowLabel", new Label());
        inject(controller, "rightArrowLabel", new Label());
    }

    @Test
    void openArticle_doesNotMarkOverviewPageAsRead() throws Exception {
        Article article = article();
        AtomicInteger callbackCount = new AtomicInteger();
        controller.setOnSectionViewed((ignoredArticle, ignoredIndex) -> callbackCount.incrementAndGet());

        JavaFxTestSupport.runOnFxAndWait(() -> controller.openArticle(article));

        assertEquals(0, callbackCount.get());
    }

    @Test
    void navigateToSection_invokesSectionViewedCallback() throws Exception {
        Article article = article();
        AtomicReference<Article> seenArticle = new AtomicReference<>();
        AtomicReference<Integer> seenIndex = new AtomicReference<>();
        controller.setOnSectionViewed((callbackArticle, sectionIndex) -> {
            seenArticle.set(callbackArticle);
            seenIndex.set(sectionIndex);
        });

        JavaFxTestSupport.runOnFxAndWait(() -> controller.openArticle(article));
        JavaFxTestSupport.runOnFxAndWait(() -> invokeNavigateToPage(1));

        assertEquals(article, seenArticle.get());
        assertEquals(0, seenIndex.get());
    }

    @Test
    void openArticleAtSection_mapsSavedSectionToReaderPage() throws Exception {
        Article article = article();
        AtomicReference<Integer> seenIndex = new AtomicReference<>();
        controller.setOnSectionViewed((ignoredArticle, sectionIndex) -> seenIndex.set(sectionIndex));

        JavaFxTestSupport.runOnFxAndWait(() -> controller.openArticleAtSection(article, 1));

        Label pageCounterLabel = get(controller, "pageCounterLabel", Label.class);
        assertEquals("3 / 4", pageCounterLabel.getText());
        assertEquals(1, seenIndex.get());
    }

    @Test
    void openArticleAtSection_withoutNotifyOnOpen_doesNotInvokeCallback() throws Exception {
        Article article = article();
        AtomicInteger callbackCount = new AtomicInteger();
        controller.setOnSectionViewed((ignoredArticle, ignoredIndex) -> callbackCount.incrementAndGet());

        JavaFxTestSupport.runOnFxAndWait(() -> controller.openArticleAtSection(article, 1, false));

        assertEquals(0, callbackCount.get());
    }

    private static Article article() {
        Article article = new Article("""
                # Reader Test

                ## One
                A

                ## Two
                B

                ## Three
                C
                """);
        article.setFileName("reader-test.md");
        return article;
    }

    private void invokeNavigateToPage(int targetIndex) {
        try {
            var method = ArticleViewController.class.getDeclaredMethod("navigateToPage", int.class);
            method.setAccessible(true);
            method.invoke(controller, targetIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static <T> T get(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

}
