package com.sddp.sexualhealthapp.mainapp.controller;

import com.sddp.sexualhealthapp.article.controller.ArticleViewController;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.RecentlyReadService;
import com.sddp.sexualhealthapp.testsupport.JavaFxTestSupport;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class MainAppControllerTest {

    private MainAppController controller;
    private RecentlyReadService recentlyReadService;
    private RecordingArticleViewController articleViewController;
    private List<Article> articles;

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestSupport.initialize();
    }

    @BeforeEach
    void setUp() throws Exception {
        JavaFxTestSupport.assumeAvailable();
        controller = new MainAppController();
        articleViewController = new RecordingArticleViewController();
        articles = ArticleCollection.getInstance().getArticles().stream()
                .filter(article -> article.getFileName() != null && !article.getSections().isEmpty())
                .limit(2)
                .toList();

        Path tempFile = Files.createTempFile("recent-main-app-", ".json");
        Files.deleteIfExists(tempFile);
        recentlyReadService = new RecentlyReadService(tempFile, ArticleCollection.getInstance());

        inject(controller, "articleListContainer", new VBox());
        inject(controller, "searchField", new TextField());
        inject(controller, "articleViewController", articleViewController);
        inject(controller, "articleView", visibleBox(false));
        inject(controller, "searchView", new VBox());
        inject(controller, "recentlyReadService", recentlyReadService);
    }

    @Test
    void renderBrowseFeed_withoutRecentEntries_hidesRecentlyReadSection() throws Exception {
        JavaFxTestSupport.runOnFxAndWait(controller::renderBrowseFeed);

        List<String> labels = labelTexts();
        assertFalse(labels.contains("Recently Read"));
        assertTrue(labels.contains("All Articles"));
    }

    @Test
    void renderBrowseFeed_withRecentEntries_placesSectionAboveAllArticles() throws Exception {
        recentlyReadService.saveProgress(articles.get(0).getFileName(), 0, Instant.parse("2026-03-11T09:00:00Z"));

        JavaFxTestSupport.runOnFxAndWait(controller::renderBrowseFeed);

        List<String> labels = labelTexts();
        assertTrue(labels.indexOf("Recently Read") >= 0);
        assertTrue(labels.indexOf("All Articles") >= 0);
        assertTrue(labels.indexOf("Recently Read") < labels.indexOf("All Articles"));
    }

    @Test
    void renderSearchResults_hidesRecentlyReadSection() throws Exception {
        recentlyReadService.saveProgress(articles.get(0).getFileName(), 0, Instant.parse("2026-03-11T09:00:00Z"));

        JavaFxTestSupport.runOnFxAndWait(() -> controller.renderSearchResults(
                "test",
                List.of(new SearchResult(articles.get(0), 0.8, Map.of()))));

        assertFalse(labelTexts().contains("Recently Read"));
    }

    @Test
    void clickingRecentCard_resumesAtSavedSection() throws Exception {
        recentlyReadService.saveProgress(articles.get(0).getFileName(), 1, Instant.parse("2026-03-11T09:00:00Z"));

        JavaFxTestSupport.runOnFxAndWait(controller::renderBrowseFeed);

        VBox recentCard = findRecentCard();
        assertNotNull(recentCard);

        JavaFxTestSupport.runOnFxAndWait(() -> recentCard.getOnMouseClicked().handle(null));

        assertEquals(articles.get(0), articleViewController.lastArticle);
        assertEquals(1, articleViewController.lastSectionIndex);
    }

    @Test
    void sectionViewed_whileArticleOverlayVisible_defersFeedRefreshUntilReturn() throws Exception {
        inject(controller, "articleView", visibleBox(true));

        JavaFxTestSupport.runOnFxAndWait(() -> invokePrivate(
                controller,
                "handleSectionViewed",
                new Class<?>[]{Article.class, Integer.class},
                articles.get(0),
                0));

        assertFalse(labelTexts().contains("Recently Read"));

        inject(controller, "articleView", visibleBox(false));
        JavaFxTestSupport.runOnFxAndWait(() -> invokePrivate(
                controller,
                "refreshBrowseFeedIfNeeded",
                new Class<?>[0]));

        assertTrue(labelTexts().contains("Recently Read"));
    }

    private VBox findRecentCard() throws Exception {
        VBox articleListContainer = get(controller, "articleListContainer", VBox.class);
        for (Node child : articleListContainer.getChildren()) {
            if (child instanceof VBox box && box.getStyleClass().contains("recent-article-card")) {
                return box;
            }
        }
        return null;
    }

    private List<String> labelTexts() throws Exception {
        VBox articleListContainer = get(controller, "articleListContainer", VBox.class);
        return articleListContainer.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .map(Label::getText)
                .toList();
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

    private static VBox visibleBox(boolean visible) {
        VBox box = new VBox();
        box.setVisible(visible);
        return box;
    }

    private static void invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class RecordingArticleViewController extends ArticleViewController {
        private Article lastArticle;
        private Integer lastSectionIndex;

        @Override
        public void openArticle(Article article) {
            this.lastArticle = article;
            this.lastSectionIndex = null;
        }

        @Override
        public void openArticleAtSection(Article article, int sectionIndex) {
            this.lastArticle = article;
            this.lastSectionIndex = sectionIndex;
        }

        @Override
        public void openArticleAtSection(Article article, int sectionIndex, boolean notifyOnOpen) {
            this.lastArticle = article;
            this.lastSectionIndex = sectionIndex;
        }
    }
}
