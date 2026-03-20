package com.sddp.sexualhealthapp.mainapp.controller;

import com.sddp.sexualhealthapp.article.controller.ArticleViewController;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.ArticleBrowseRankingService;
import com.sddp.sexualhealthapp.article.service.RecentlyReadService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class MainAppControllerTest {

    private static boolean javaFxAvailable = false;
    private MainAppController controller;
    private RecentlyReadService recentlyReadService;
    private RecordingArticleViewController articleViewController;
    private List<Article> articles;

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

    @BeforeEach
    void setUp() throws Exception {
        assumeJavaFxAvailable();
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
        inject(controller, "browseRankingService", new ArticleBrowseRankingService());
    }

    @Test
    void renderBrowseFeed_withoutRecentEntries_hidesRecentlyReadSection() throws Exception {
        runOnFxAndWait(controller::renderBrowseFeed);

        List<String> labels = labelTexts();
        assertFalse(labels.contains("Recently Read"));
        assertTrue(labels.contains("Articles For You"));
    }

    @Test
    void renderBrowseFeed_withRecentEntries_placesSectionAboveAllArticles() throws Exception {
        recentlyReadService.saveProgress(articles.get(0).getFileName(), 0, Instant.parse("2026-03-11T09:00:00Z"));

        runOnFxAndWait(controller::renderBrowseFeed);

        List<String> labels = labelTexts();
        assertTrue(labels.indexOf("Recently Read") >= 0);
        assertTrue(labels.indexOf("Articles For You") >= 0);
        assertTrue(labels.indexOf("Recently Read") < labels.indexOf("Articles For You"));
    }

    @Test
    void renderSearchResults_hidesRecentlyReadSection() throws Exception {
        recentlyReadService.saveProgress(articles.get(0).getFileName(), 0, Instant.parse("2026-03-11T09:00:00Z"));

        runOnFxAndWait(() -> controller.renderSearchResults(
                "test",
                List.of(new SearchResult(articles.get(0), 0.8, Map.of()))));

        assertFalse(labelTexts().contains("Recently Read"));
    }

    @Test
    void renderBrowseFeed_usesRankedOrderForArticleCards() throws Exception {
        inject(controller, "browseRankingService", new ArticleBrowseRankingService() {
            @Override
            public List<Article> rankArticles(List<Article> articles,
                    List<com.sddp.sexualhealthapp.article.model.RecentlyReadEntry> recentEntries,
                    com.sddp.sexualhealthapp.settings.model.ContentPreferences preferences) {
                return articles.stream().sorted((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle())).toList();
            }
        });

        runOnFxAndWait(controller::renderBrowseFeed);
        waitForFxCondition(() -> {
            VBox firstCard = firstStandardArticleCard();
            if (firstCard == null) {
                return false;
            }
            Label titleLabel = findLabelWithStyle(firstCard, "article-card-title");
            if (titleLabel == null) {
                return false;
            }
            List<Article> expected = ArticleCollection.getInstance().getArticles().stream()
                    .sorted((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()))
                    .toList();
            return expected.get(0).getTitle().equals(titleLabel.getText());
        });

        VBox firstCard = firstStandardArticleCard();
        assertNotNull(firstCard);
        Label titleLabel = findLabelWithStyle(firstCard, "article-card-title");
        assertNotNull(titleLabel);

        List<Article> expected = ArticleCollection.getInstance().getArticles().stream()
                .sorted((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()))
                .toList();
        assertEquals(expected.get(0).getTitle(), titleLabel.getText());
    }

    @Test
    void renderBrowseFeed_showsInitialCardsBeforeBackgroundRankingCompletes() throws Exception {
        CountDownLatch rankingStarted = new CountDownLatch(1);
        CountDownLatch releaseRanking = new CountDownLatch(1);
        List<Article> rankedOrder = ArticleCollection.getInstance().getArticles().stream()
                .sorted((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()))
                .toList();

        inject(controller, "browseRankingService", new ArticleBrowseRankingService() {
            @Override
            public List<Article> rankArticles(List<Article> articles,
                    List<com.sddp.sexualhealthapp.article.model.RecentlyReadEntry> recentEntries,
                    com.sddp.sexualhealthapp.settings.model.ContentPreferences preferences) {
                rankingStarted.countDown();
                try {
                    releaseRanking.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return rankedOrder;
            }
        });

        String initialTitle = ArticleCollection.getInstance().getArticles().get(0).getTitle();

        runOnFxAndWait(controller::renderBrowseFeed);
        assertTrue(rankingStarted.await(5, TimeUnit.SECONDS), "Ranking did not start");

        List<String> labels = labelTexts();
        assertTrue(labels.contains("Articles For You"));
        assertTrue(labels.contains("Personalising articles..."));
        assertEquals(initialTitle, firstStandardArticleTitle());

        releaseRanking.countDown();
        waitForFxCondition(() -> rankedOrder.get(0).getTitle().equals(firstStandardArticleTitle()));
    }

    @Test
    void renderBrowseFeed_ignoresStaleBackgroundRankingResults() throws Exception {
        CountDownLatch firstCallStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstCall = new CountDownLatch(1);
        List<List<Article>> responses = new ArrayList<>();
        responses.add(ArticleCollection.getInstance().getArticles());
        responses.add(ArticleCollection.getInstance().getArticles().stream()
                .sorted((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()))
                .toList());

        inject(controller, "browseRankingService", new ArticleBrowseRankingService() {
            private int invocationCount = 0;

            @Override
            public synchronized List<Article> rankArticles(List<Article> articles,
                    List<com.sddp.sexualhealthapp.article.model.RecentlyReadEntry> recentEntries,
                    com.sddp.sexualhealthapp.settings.model.ContentPreferences preferences) {
                invocationCount++;
                if (invocationCount == 1) {
                    firstCallStarted.countDown();
                    try {
                        releaseFirstCall.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return responses.get(Math.min(invocationCount - 1, responses.size() - 1));
            }
        });

        runOnFxAndWait(controller::renderBrowseFeed);
        assertTrue(firstCallStarted.await(5, TimeUnit.SECONDS), "First ranking did not start");

        runOnFxAndWait(controller::renderBrowseFeed);
        waitForFxCondition(() -> responses.get(1).get(0).getTitle().equals(firstStandardArticleTitle()));

        releaseFirstCall.countDown();
        Thread.sleep(100);
        waitForFxSettled();
        assertEquals(responses.get(1).get(0).getTitle(), firstStandardArticleTitle());
    }

    @Test
    void clickingRecentCard_resumesAtSavedSection() throws Exception {
        recentlyReadService.saveProgress(articles.get(0).getFileName(), 1, Instant.parse("2026-03-11T09:00:00Z"));

        runOnFxAndWait(controller::renderBrowseFeed);

        VBox recentCard = findRecentCard();
        assertNotNull(recentCard);

        runOnFxAndWait(() -> recentCard.getOnMouseClicked().handle(null));

        assertEquals(articles.get(0), articleViewController.lastArticle);
        assertEquals(1, articleViewController.lastSectionIndex);
    }

    @Test
    void sectionViewed_whileArticleOverlayVisible_defersFeedRefreshUntilReturn() throws Exception {
        inject(controller, "articleView", visibleBox(true));

        runOnFxAndWait(() -> invokePrivate(
                controller,
                "handleSectionViewed",
                new Class<?>[] { Article.class, Integer.class },
                articles.get(0),
                0));

        assertFalse(labelTexts().contains("Recently Read"));

        inject(controller, "articleView", visibleBox(false));
        runOnFxAndWait(() -> invokePrivate(
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

    private VBox firstStandardArticleCard() throws Exception {
        VBox articleListContainer = get(controller, "articleListContainer", VBox.class);
        for (Node child : articleListContainer.getChildren()) {
            if (child instanceof VBox box
                    && box.getStyleClass().contains("article-card")
                    && !box.getStyleClass().contains("recent-article-card")) {
                return box;
            }
        }
        return null;
    }

    private String firstStandardArticleTitle() throws Exception {
        VBox firstCard = firstStandardArticleCard();
        if (firstCard == null) {
            return null;
        }
        Label titleLabel = findLabelWithStyle(firstCard, "article-card-title");
        return titleLabel == null ? null : titleLabel.getText();
    }

    private Label findLabelWithStyle(VBox box, String styleClass) {
        for (Node child : box.getChildren()) {
            if (child instanceof Label label && label.getStyleClass().contains(styleClass)) {
                return label;
            }
            if (child instanceof VBox nested) {
                Label nestedMatch = findLabelWithStyle(nested, styleClass);
                if (nestedMatch != null) {
                    return nestedMatch;
                }
            }
            if (child instanceof javafx.scene.layout.HBox hBox) {
                for (Node hChild : hBox.getChildren()) {
                    if (hChild instanceof Label label && label.getStyleClass().contains(styleClass)) {
                        return label;
                    }
                }
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

    private static void waitForFxCondition(Condition condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (runOnFxAndReturn(condition::test)) {
                return;
            }
            Thread.sleep(25);
        }
        assertTrue(runOnFxAndReturn(condition::test), "Timed out waiting for FX condition");
    }

    private static void waitForFxSettled() throws Exception {
        runOnFxAndWait(() -> {
        });
    }

    private static boolean runOnFxAndReturn(Condition condition) throws Exception {
        assumeJavaFxAvailable();

        if (Platform.isFxApplicationThread()) {
            try {
                return condition.test();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        boolean[] result = new boolean[1];
        RuntimeException[] failure = new RuntimeException[1];
        Platform.runLater(() -> {
            try {
                result[0] = condition.test();
            } catch (Exception e) {
                failure[0] = new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for FX thread");
        if (failure[0] != null) {
            throw failure[0];
        }
        return result[0];
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

    @FunctionalInterface
    private interface Condition {
        boolean test() throws Exception;
    }
}
