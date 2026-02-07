package com.sddp.sexualhealthapp.mainapp.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the main app view with article search and reader.
 */
public class MainAppController {

    @FXML
    private VBox searchView;
    @FXML
    private VBox articleView;
    @FXML
    private TextField searchField;
    @FXML
    private VBox articleListContainer;
    @FXML
    private StackPane articlePageContainer;
    @FXML
    private HBox pageIndicatorContainer;
    @FXML
    private Label pageCounterLabel;
    @FXML
    private ScrollPane listScrollPane;

    private HybridSearchService searchService;
    private PauseTransition searchDebounce;

    // Swipe/pagination state
    private List<VBox> articlePages;
    private int currentPageIndex;
    private double swipeStartX;
    private static final double SWIPE_THRESHOLD = 50.0;
    private static final int SLIDE_DURATION_MS = 250;

    @FXML
    private void initialize() {
        searchService = new HybridSearchService();

        // Debounce: wait 300ms after user stops typing before searching
        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(e -> performSearch());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.playFromStart();
        });

        // Show all articles on initial load
        showAllArticles();
    }

    private void showAllArticles() {
        articleListContainer.getChildren().clear();

        List<Article> articles = ArticleCollection.getInstance().getArticles();

        if (articles.isEmpty()) {
            showEmptyState("No articles found");
            return;
        }

        for (Article article : articles) {
            articleListContainer.getChildren().add(createArticleCard(article, -1.0));
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            showAllArticles();
            return;
        }

        // Show loading state
        articleListContainer.getChildren().clear();
        Label loading = new Label("Searching...");
        loading.getStyleClass().add("search-empty-label");
        articleListContainer.getChildren().add(loading);

        // Run search on background thread (ONNX model can be slow on first call)
        Thread searchThread = new Thread(() -> {
            List<SearchResult> results = searchService.search(query);

            Platform.runLater(() -> {
                articleListContainer.getChildren().clear();

                if (results.isEmpty()) {
                    showEmptyState("No results for \"" + query + "\"");
                    return;
                }

                for (SearchResult result : results) {
                    articleListContainer.getChildren().add(
                            createArticleCard(result.article(), result.score()));
                }
            });
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private VBox createArticleCard(Article article, double score) {
        VBox card = new VBox(4);
        card.getStyleClass().add("article-card");

        // Colour-code the card based on relevance tier (only during search)
        if (score >= 0.5) {
            card.getStyleClass().add("article-card-relevant");
        } else if (score >= 0.25) {
            card.getStyleClass().add("article-card-possible");
        }

        // Title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-card-title");
        title.setWrapText(true);
        title.setMaxWidth(260);
        titleRow.getChildren().add(title);

        // Relevance badge: only shown during search (score >= 0)
        if (score >= 0.5) {
            Label badge = new Label("Relevant");
            badge.getStyleClass().add("article-card-badge");
            titleRow.getChildren().add(badge);
        } else if (score >= 0.25) {
            Label badge = new Label("Possible match");
            badge.getStyleClass().add("article-card-badge-amber");
            titleRow.getChildren().add(badge);
        }

        card.getChildren().add(titleRow);

        // Section count subtitle
        int sectionCount = article.getSections().size();
        Label subtitle = new Label(sectionCount + " section" + (sectionCount != 1 ? "s" : ""));
        subtitle.getStyleClass().add("article-card-subtitle");
        card.getChildren().add(subtitle);

        // Click to open article
        card.setOnMouseClicked(e -> openArticle(article));

        return card;
    }

    private void openArticle(Article article) {
        articlePages = new ArrayList<>();
        currentPageIndex = 0;
        articlePageContainer.getChildren().clear();
        pageIndicatorContainer.getChildren().clear();

        // Page 0: Title page with article overview
        VBox titlePage = createTitlePage(article);
        articlePages.add(titlePage);

        // One page per section
        List<Article.Section> sections = article.getSections();
        for (int i = 0; i < sections.size(); i++) {
            VBox sectionPage = createSectionPage(sections.get(i), i + 1, sections.size());
            articlePages.add(sectionPage);
        }

        // Add all pages to the container (only the first is visible)
        for (int i = 0; i < articlePages.size(); i++) {
            VBox page = articlePages.get(i);
            page.setVisible(i == 0);
            page.setTranslateX(0);
            articlePageContainer.getChildren().add(page);
        }

        // Build page indicator dots
        buildPageIndicators();
        updatePageCounter();

        // Set up swipe gesture handling on the container
        articlePageContainer.setOnMousePressed(e -> swipeStartX = e.getSceneX());
        articlePageContainer.setOnMouseReleased(e -> {
            double deltaX = e.getSceneX() - swipeStartX;
            if (Math.abs(deltaX) >= SWIPE_THRESHOLD) {
                if (deltaX < 0) {
                    navigateToPage(currentPageIndex + 1);
                } else {
                    navigateToPage(currentPageIndex - 1);
                }
            }
        });

        // Switch to article view
        searchView.setVisible(false);
        articleView.setVisible(true);
    }

    /**
     * Creates the title/overview page for an article (page 0).
     */
    private VBox createTitlePage(Article article) {
        VBox page = new VBox(12);
        page.getStyleClass().add("article-page");
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(4, 20, 24, 20));

        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-detail-title");
        title.setWrapText(true);
        page.getChildren().add(title);

        // Show a summary of available sections as a table of contents
        List<Article.Section> sections = article.getSections();
        if (!sections.isEmpty()) {
            Label tocHeader = new Label("In this article (" + sections.size() + " sections)");
            tocHeader.getStyleClass().add("article-toc-header");
            tocHeader.setWrapText(true);
            page.getChildren().add(tocHeader);

            for (int i = 0; i < sections.size(); i++) {
                Label tocItem = new Label((i + 1) + ".  " + sections.get(i).heading());
                tocItem.getStyleClass().add("article-toc-item");
                tocItem.setWrapText(true);
                page.getChildren().add(tocItem);
            }

            Label swipeHint = new Label("Swipe left to start reading →");
            swipeHint.getStyleClass().add("article-swipe-hint");
            swipeHint.setWrapText(true);
            page.getChildren().add(swipeHint);
        }

        // Wrap in ScrollPane so long TOC is scrollable
        ScrollPane scrollWrapper = new ScrollPane(page);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.getStyleClass().add("search-scroll-pane");

        VBox wrapper = new VBox(scrollWrapper);
        wrapper.getStyleClass().add("article-page-wrapper");
        VBox.setVgrow(scrollWrapper, javafx.scene.layout.Priority.ALWAYS);
        return wrapper;
    }

    /**
     * Creates a page for a single article section.
     */
    private VBox createSectionPage(Article.Section section, int sectionNumber, int totalSections) {
        VBox page = new VBox(10);
        page.getStyleClass().add("article-page");
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(4, 20, 24, 20));

        // Section number badge
        Label badge = new Label("Section " + sectionNumber + " of " + totalSections);
        badge.getStyleClass().add("article-section-badge");
        page.getChildren().add(badge);

        // Section heading
        Label heading = new Label(section.heading());
        heading.getStyleClass().add("article-section-heading");
        heading.setWrapText(true);
        page.getChildren().add(heading);

        // Section content
        Label content = new Label(section.content().trim());
        content.getStyleClass().add("article-section-content");
        content.setWrapText(true);
        page.getChildren().add(content);

        // Wrap in ScrollPane so long sections are individually scrollable
        ScrollPane scrollWrapper = new ScrollPane(page);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.getStyleClass().add("search-scroll-pane");

        VBox wrapper = new VBox(scrollWrapper);
        wrapper.getStyleClass().add("article-page-wrapper");
        VBox.setVgrow(scrollWrapper, javafx.scene.layout.Priority.ALWAYS);
        return wrapper;
    }

    /**
     * Animates transition to the target page index.
     */
    private void navigateToPage(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= articlePages.size() || targetIndex == currentPageIndex) {
            return;
        }

        boolean goingForward = targetIndex > currentPageIndex;
        VBox outgoingPage = articlePages.get(currentPageIndex);
        VBox incomingPage = articlePages.get(targetIndex);
        double width = articlePageContainer.getWidth();

        // Position incoming page off-screen in the swipe direction
        incomingPage.setTranslateX(goingForward ? width : -width);
        incomingPage.setVisible(true);

        // Slide outgoing page out
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(SLIDE_DURATION_MS), outgoingPage);
        slideOut.setToX(goingForward ? -width : width);

        // Slide incoming page in
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(SLIDE_DURATION_MS), incomingPage);
        slideIn.setToX(0);

        slideOut.setOnFinished(e -> {
            outgoingPage.setVisible(false);
            outgoingPage.setTranslateX(0);
        });

        slideOut.play();
        slideIn.play();

        currentPageIndex = targetIndex;
        updatePageIndicators();
        updatePageCounter();
    }

    /**
     * Builds the dot indicators at the bottom of the article view.
     */
    private void buildPageIndicators() {
        pageIndicatorContainer.getChildren().clear();
        for (int i = 0; i < articlePages.size(); i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add(i == 0 ? "page-dot-active" : "page-dot");
            final int pageIndex = i;
            dot.setOnMouseClicked(e -> navigateToPage(pageIndex));
            pageIndicatorContainer.getChildren().add(dot);
        }
    }

    /**
     * Updates the dot indicators to reflect the current page.
     */
    private void updatePageIndicators() {
        for (int i = 0; i < pageIndicatorContainer.getChildren().size(); i++) {
            Circle dot = (Circle) pageIndicatorContainer.getChildren().get(i);
            dot.getStyleClass().clear();
            dot.getStyleClass().add(i == currentPageIndex ? "page-dot-active" : "page-dot");
        }
    }

    /**
     * Updates the page counter label (e.g. "1 / 5").
     */
    private void updatePageCounter() {
        pageCounterLabel.setText((currentPageIndex + 1) + " / " + articlePages.size());
    }

    @FXML
    private void handleBackToSearch() {
        articleView.setVisible(false);
        searchView.setVisible(true);
    }

    @FXML
    private void handleBackToCalculator(ActionEvent event) {
        SceneManager.getInstance().transitionToCalculator();
    }

    private void showEmptyState(String message) {
        Label empty = new Label(message);
        empty.getStyleClass().add("search-empty-label");
        empty.setWrapText(true);
        articleListContainer.getChildren().add(empty);
    }
}
