package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the article detail view.
 * Manages paginated article sections with swipe navigation and page indicators.
 */
public class ArticleViewController {

    @FXML
    private StackPane articlePageContainer;
    @FXML
    private HBox pageIndicatorContainer;
    @FXML
    private Label pageCounterLabel;

    private Runnable onBackToSearch;

    private List<VBox> articlePages;
    private int currentPageIndex;
    private double swipeStartX;

    private boolean hasSetUpSwipeEvents = false;

    private static final double SWIPE_THRESHOLD = 50.0;
    private static final int SLIDE_DURATION_MS = 250;

    /**
     * Sets the callback to invoke when the user presses the Back button.
     *
     * @param callback the callback to run on back navigation
     */
    public void setOnBackToSearch(Runnable callback) {
        this.onBackToSearch = callback;
    }

    /**
     * Opens an article in the paginated reader.
     * Builds title and section pages, sets up swipe listeners, and displays page 0.
     *
     * @param article the article to display
     */
    public void openArticle(Article article) {
        articlePages = new ArrayList<>();
        currentPageIndex = 0;
        articlePageContainer.getChildren().clear();
        pageIndicatorContainer.getChildren().clear();

        // Page 0: Title page with article overview
        VBox titlePage = ArticlePageBuilder.createTitlePage(article);
        articlePages.add(titlePage);

        // One page per section
        List<Article.Section> sections = article.getSections();
        for (int i = 0; i < sections.size(); i++) {
            VBox sectionPage = ArticlePageBuilder.createSectionPage(sections.get(i), i + 1, sections.size());
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

        // Repeated registering of these events will cause pages to be skipped over
        if (!hasSetUpSwipeEvents) {
            hasSetUpSwipeEvents = true;

            // Use event filters (capture phase) so the StackPane sees the press
            // before the child ScrollPane consumes it for its own panning.
            articlePageContainer.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                swipeStartX = e.getSceneX();
            });

            articlePageContainer.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
                double deltaX = e.getSceneX() - swipeStartX;
                if (Math.abs(deltaX) >= SWIPE_THRESHOLD) {
                    if (deltaX < 0) {
                        navigateToPage(currentPageIndex + 1);
                    } else {
                        navigateToPage(currentPageIndex - 1);
                    }
                }
            });
        }
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
    private void handleBack() {
        if (onBackToSearch != null) {
            onBackToSearch.run();
        }
    }
}
