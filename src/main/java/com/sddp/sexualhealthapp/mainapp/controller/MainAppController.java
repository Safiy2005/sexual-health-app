package com.sddp.sexualhealthapp.mainapp.controller;

import com.sddp.sexualhealthapp.article.controller.ArticleCardFactory;
import com.sddp.sexualhealthapp.article.controller.ArticleViewController;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Controller for the main app view with article search.
 * Delegates article display to {@link ArticleViewController}.
 */
public class MainAppController {

    @FXML
    private VBox searchView;
    @FXML
    private VBox articleView;
    @FXML
    private ArticleViewController articleViewController;
    @FXML
    private TextField searchField;
    @FXML
    private VBox articleListContainer;
    @FXML
    private ScrollPane listScrollPane;

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

        // Wire the article view's back button to return to search
        articleViewController.setOnBackToSearch(this::handleBackToSearch);

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
            articleListContainer.getChildren().add(
                    ArticleCardFactory.createArticleCard(article, -1.0, "", this::openArticle));
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
                            ArticleCardFactory.createArticleCard(
                                    result.article(), result.score(), query, this::openArticle));
                }
            });
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void openArticle(Article article) {
        articleViewController.openArticle(article);
        searchView.setVisible(false);
        articleView.setVisible(true);
    }

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
