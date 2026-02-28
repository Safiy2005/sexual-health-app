package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    @FXML
    private Button navMenuButton;
    @FXML
    private VBox navMenuOverlay;
    @FXML
    private VBox navMenuBackdrop;
    @FXML
    private ScrollPane navMenuScroll;
    @FXML
    private VBox navMenuContent;

    private boolean navMenuOpen = false;
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

        // Build the navigation menu items
        buildNavMenu(article);
        hideNavMenu();

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
     * Resets the page's scroll position to the top before animating.
     */
    private void navigateToPage(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= articlePages.size() || targetIndex == currentPageIndex) {
            return;
        }

        boolean goingForward = targetIndex > currentPageIndex;
        VBox outgoingPage = articlePages.get(currentPageIndex);
        VBox incomingPage = articlePages.get(targetIndex);
        double width = articlePageContainer.getWidth();

        // Reset scroll position to top for the incoming page
        resetScrollPosition(incomingPage);

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

    // This may break if the layout of article section pages changes...
    private void resetScrollPosition(VBox pageWrapper) {
        if (!pageWrapper.getChildren().isEmpty()
                && pageWrapper.getChildren().get(0) instanceof ScrollPane scrollPane) {
            scrollPane.setVvalue(0);
        }
    }

    /**
     * Initial setup for indicators.
     */
    private void buildPageIndicators() {
        // Just call update to render the initial window of dots
        updatePageIndicators();
    }

    /**
     * Rebuilds the dots to show a sliding window (e.g., 5 dots)
     * centered on the current page to prevent overflow.
     */
    private void updatePageIndicators() {
        pageIndicatorContainer.getChildren().clear();

        int totalPages = articlePages.size();
        int maxVisible = 5; // How many dots you want to see at once

        // 1. Calculate the window range (Start -> End)
        // Try to center the current page
        int start = Math.max(0, currentPageIndex - (maxVisible / 2));
        int end = Math.min(totalPages, start + maxVisible);

        // Adjust start if we hit the end (so we always show 5 dots if possible)
        if (end - start < maxVisible) {
            start = Math.max(0, end - maxVisible);
        }

        // 2. Create the dots for this window
        for (int i = start; i < end; i++) {
            Circle dot = new Circle(4);

            // Add style classes (make sure these are in your CSS)
            if (i == currentPageIndex) {
                dot.getStyleClass().add("page-dot-active");
            } else {
                dot.getStyleClass().add("page-dot");

                // Optional: Make the edge dots smaller for a smoother "Instagram" look
                if (totalPages > maxVisible && (i == start || i == end - 1)) {
                    dot.setRadius(2.5);
                }
            }

            // Add click listener to jump to that specific page
            final int targetIndex = i;
            dot.setOnMouseClicked(e -> navigateToPage(targetIndex));

            pageIndicatorContainer.getChildren().add(dot);
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
        if (navMenuOpen) {
            hideNavMenu();
        }
        if (onBackToSearch != null) {
            onBackToSearch.run();
        }
    }

    /**
     * Toggles the mini navigation menu overlay.
     */
    @FXML
    private void handleToggleNavMenu() {
        if (navMenuOpen) {
            hideNavMenu();
        } else {
            showNavMenu();
        }
    }

    /**
     * Builds the navigation menu items from the article sections.
     */
    private void buildNavMenu(Article article) {
        navMenuContent.getChildren().clear();

        List<Article.Section> sections = article.getSections();

        // Title page entry
        Label titleItem = new Label("\u2302  " + article.getTitle());
        titleItem.getStyleClass().add("nav-menu-item");
        titleItem.setWrapText(true);
        titleItem.setMaxWidth(Double.MAX_VALUE);
        titleItem.setOnMouseClicked(e -> {
            navigateToPage(0);
            hideNavMenu();
        });
        navMenuContent.getChildren().add(titleItem);

        // One entry per section
        for (int i = 0; i < sections.size(); i++) {
            final int pageIndex = i + 1; // page 0 is the title page
            Article.Section section = sections.get(i);

            Label item = new Label((i + 1) + "  " + section.heading());
            item.getStyleClass().add("nav-menu-item");
            item.setWrapText(true);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnMouseClicked(e -> {
                navigateToPage(pageIndex);
                hideNavMenu();
            });
            navMenuContent.getChildren().add(item);
        }
    }

    private void showNavMenu() {
        navMenuOpen = true;

        // Highlight the current page in the nav menu
        for (int i = 0; i < navMenuContent.getChildren().size(); i++) {
            javafx.scene.Node item = navMenuContent.getChildren().get(i);
            item.getStyleClass().remove("nav-menu-item-active");
            if (i == currentPageIndex) {
                item.getStyleClass().add("nav-menu-item-active");
            }
        }

        navMenuBackdrop.setVisible(true);
        navMenuBackdrop.setManaged(true);
        navMenuBackdrop.setOpacity(0);

        navMenuOverlay.setVisible(true);
        navMenuOverlay.setManaged(true);
        navMenuOverlay.setOpacity(0);

        // Scroll to the current section after layout
        navMenuOverlay.applyCss();
        navMenuOverlay.layout();
        int itemCount = navMenuContent.getChildren().size();
        if (itemCount > 1) {
            navMenuScroll.setVvalue((double) currentPageIndex / (itemCount - 1));
        } else {
            navMenuScroll.setVvalue(0);
        }

        // Fade in both backdrop and overlay together
        FadeTransition backdropFade = new FadeTransition(Duration.millis(150), navMenuBackdrop);
        backdropFade.setToValue(1);
        backdropFade.play();

        FadeTransition overlayFade = new FadeTransition(Duration.millis(150), navMenuOverlay);
        overlayFade.setToValue(1);
        overlayFade.play();
    }

    private void hideNavMenu() {
        navMenuOpen = false;

        navMenuBackdrop.setVisible(false);
        navMenuBackdrop.setManaged(false);

        navMenuOverlay.setVisible(false);
        navMenuOverlay.setManaged(false);
    }
}
