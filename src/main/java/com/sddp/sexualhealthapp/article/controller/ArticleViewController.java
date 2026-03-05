package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @FXML
    private Label leftArrowLabel;
    @FXML
    private Label rightArrowLabel;

    private boolean navMenuOpen = false;
    private Runnable onBackToSearch;

    private List<VBox> articlePages;
    private int currentPageIndex;
    private double swipeStartX;

    private boolean hasSetUpSwipeEvents = false;

    private static final double SWIPE_THRESHOLD = 50.0;
    private static final int SLIDE_DURATION_MS = 250;

    /** Matches headings ending with an optional colon + "Part N" suffix. */
    private static final Pattern PART_PATTERN = Pattern.compile("^(.+?)(?::?\\s*Part\\s+\\d+)$",
            Pattern.CASE_INSENSITIVE);

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

        // Update arrow indicators
        updateArrowIndicators();
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
        updateArrowIndicators();
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
     * Shows/hides the left and right arrow labels based on the current page.
     */
    private void updateArrowIndicators() {
        leftArrowLabel.setVisible(currentPageIndex > 0);
        rightArrowLabel.setVisible(currentPageIndex < articlePages.size() - 1);
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
     * Returns the base heading for grouping, stripping any ": Part N" suffix.
     */
    private String getBaseHeading(String heading) {
        Matcher m = PART_PATTERN.matcher(heading);
        return m.matches() ? m.group(1).trim() : heading;
    }

    /**
     * Builds the navigation menu items from the article sections.
     * Consecutive sections whose headings share the same base (before ": Part N")
     * are grouped under a single header with indented sub-items.
     */
    private void buildNavMenu(Article article) {
        navMenuContent.getChildren().clear();

        List<Article.Section> sections = article.getSections();

        // Title page entry
        HBox titleRow = createNavMenuItem("\u2302", article.getTitle());
        titleRow.setUserData(0);
        titleRow.setOnMouseClicked(e -> {
            navigateToPage(0);
            hideNavMenu();
        });
        navMenuContent.getChildren().add(titleRow);

        // Group consecutive sections that share a base heading
        int i = 0;
        int displayNumber = 1; // sequential counter, groups count as one
        while (i < sections.size()) {
            String baseHeading = getBaseHeading(sections.get(i).heading());

            // Count how many consecutive sections share this base
            int groupStart = i;
            while (i < sections.size()
                    && getBaseHeading(sections.get(i).heading()).equals(baseHeading)) {
                i++;
            }
            int groupSize = i - groupStart;

            if (groupSize > 1) {
                // Wrapper VBox to visually group heading + dots
                VBox groupWrapper = new VBox(0);
                groupWrapper.getStyleClass().add("nav-menu-group-wrapper");
                groupWrapper.setUserData(-1);

                // Group header with number (non-clickable)
                HBox groupRow = createNavMenuItem(String.valueOf(displayNumber), baseHeading);
                groupRow.setMouseTransparent(true);
                groupRow.getStyleClass().add("nav-menu-group-item");
                groupWrapper.getChildren().add(groupRow);

                // Sub-item dots for each part
                FlowPane dotsRow = new FlowPane(6, 6);
                dotsRow.getStyleClass().add("nav-menu-dots-row");
                dotsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                for (int j = groupStart; j < groupStart + groupSize; j++) {
                    final int pageIndex = j + 1;

                    Label dot = new Label(String.valueOf(j - groupStart + 1));
                    dot.getStyleClass().add("nav-menu-dot-button");
                    dot.setUserData(pageIndex);
                    dot.setOnMouseClicked(e -> {
                        navigateToPage(pageIndex);
                        hideNavMenu();
                    });
                    dotsRow.getChildren().add(dot);
                }
                groupWrapper.getChildren().add(dotsRow);
                navMenuContent.getChildren().add(groupWrapper);
            } else {
                // Single section – render normally
                final int pageIndex = groupStart + 1;
                Article.Section section = sections.get(groupStart);

                HBox row = createNavMenuItem(String.valueOf(displayNumber), section.heading());
                row.setUserData(pageIndex);
                row.setOnMouseClicked(e -> {
                    navigateToPage(pageIndex);
                    hideNavMenu();
                });
                navMenuContent.getChildren().add(row);
            }
            displayNumber++;
        }
    }

    /**
     * Creates a nav menu item row with a bold number badge and a heading label.
     */
    private HBox createNavMenuItem(String number, String heading) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("nav-menu-item-number");
        numberLabel.setMinWidth(Label.USE_PREF_SIZE);

        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("nav-menu-item-heading");
        headingLabel.setWrapText(true);

        HBox row = new HBox(8, numberLabel, headingLabel);
        row.getStyleClass().add("nav-menu-item");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private void showNavMenu() {
        navMenuOpen = true;

        // Highlight the current page in the nav menu using stored page indices
        int activeChildIndex = 0;
        for (int i = 0; i < navMenuContent.getChildren().size(); i++) {
            javafx.scene.Node item = navMenuContent.getChildren().get(i);
            item.getStyleClass().remove("nav-menu-item-active");

            // Check top-level items (HBox rows with a page index)
            if (item.getUserData() instanceof Integer pageIndex
                    && pageIndex == currentPageIndex) {
                item.getStyleClass().add("nav-menu-item-active");
                activeChildIndex = i;
            }

            // Check dot-button children inside group wrappers
            if (item instanceof VBox wrapper
                    && wrapper.getStyleClass().contains("nav-menu-group-wrapper")) {
                for (javafx.scene.Node child : wrapper.getChildren()) {
                    if (child instanceof HBox hbox) {
                        for (javafx.scene.Node dot : hbox.getChildren()) {
                            dot.getStyleClass().remove("nav-menu-dot-active");
                            if (dot.getUserData() instanceof Integer dotPage
                                    && dotPage == currentPageIndex) {
                                dot.getStyleClass().add("nav-menu-dot-active");
                                activeChildIndex = i;
                            }
                        }
                    }
                }
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
            navMenuScroll.setVvalue((double) activeChildIndex / (itemCount - 1));
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
