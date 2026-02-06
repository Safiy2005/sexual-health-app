package com.sddp.sexualhealthapp.mainapp.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Controller for the main app view with article search and reader.
 */
public class MainAppController {

    @FXML private VBox searchView;
    @FXML private VBox articleView;
    @FXML private TextField searchField;
    @FXML private VBox articleListContainer;
    @FXML private VBox articleContentContainer;
    @FXML private ScrollPane listScrollPane;

    private HybridSearchService searchService;
    private PauseTransition searchDebounce;

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
            articleListContainer.getChildren().add(createArticleCard(article, -1));
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
                            createArticleCard(result.article(), result.getRelevancePercent()));
                }
            });
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private VBox createArticleCard(Article article, int relevancePercent) {
        VBox card = new VBox(4);
        card.getStyleClass().add("article-card");

        // Title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-card-title");
        title.setWrapText(true);
        title.setMaxWidth(260);
        titleRow.getChildren().add(title);

        if (relevancePercent >= 0) {
            Label badge = new Label(relevancePercent + "%");
            badge.getStyleClass().add("article-card-badge");
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
        articleContentContainer.getChildren().clear();

        // Article title
        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-detail-title");
        title.setWrapText(true);
        articleContentContainer.getChildren().add(title);

        // Render each section
        for (Article.Section section : article.getSections()) {
            Label heading = new Label(section.heading());
            heading.getStyleClass().add("article-section-heading");
            heading.setWrapText(true);
            articleContentContainer.getChildren().add(heading);

            Label content = new Label(section.content().trim());
            content.getStyleClass().add("article-section-content");
            content.setWrapText(true);
            articleContentContainer.getChildren().add(content);
        }

        // Switch to article view
        searchView.setVisible(false);
        articleView.setVisible(true);
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
