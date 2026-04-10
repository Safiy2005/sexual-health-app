package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.ArticlePageRecommendationService;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for the article detail view.
 * Manages paginated article sections with swipe navigation and page indicators.
 */
public class ArticleViewController {

    @FunctionalInterface
    interface SectionRecommendationProvider {
        List<SearchResult> recommend(Article article, int sectionIndex, int maxResults);
    }

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
    private BiConsumer<Article, Integer> onSectionViewed;
    private Consumer<Article> onSuggestedArticleSelected;

    private List<ArticlePageBuilder.SectionPage> articlePages;
    private int currentPageIndex;
    private double swipeStartX;
    private Article currentArticle;
    private final SectionRecommendationProvider recommendationProvider;
    private final Map<Integer, List<SearchResult>> cachedSuggestionsByPage = new HashMap<>();
    private final Set<Integer> loadingSuggestionPages = new HashSet<>();
    private long recommendationSessionId = 0L;

    private boolean hasSetUpSwipeEvents = false;

    private static final double SWIPE_THRESHOLD = 50.0;
    private static final int SLIDE_DURATION_MS = 250;
    private static final int MAX_SUGGESTED_ARTICLES = 3;

    /** Matches headings ending with an optional colon + "Part N" suffix. */
    private static final Pattern PART_PATTERN = Pattern.compile("^(.+?)(?::?\\s*Part\\s+\\d+)$",
            Pattern.CASE_INSENSITIVE);

    public ArticleViewController() {
        this(new ArticlePageRecommendationService()::recommendForPage);
    }

    ArticleViewController(SectionRecommendationProvider recommendationProvider) {
        this.recommendationProvider = recommendationProvider;
    }

    /**
     * Sets the callback to invoke when the user presses the Back button.
     *
     * @param callback the callback to run on back navigation
     */
    public void setOnBackToSearch(Runnable callback) {
        this.onBackToSearch = callback;
    }

    public void setOnSectionViewed(BiConsumer<Article, Integer> callback) {
        this.onSectionViewed = callback;
    }

    public void setOnSuggestedArticleSelected(Consumer<Article> callback) {
        this.onSuggestedArticleSelected = callback;
    }

    /**
     * Opens an article in the paginated reader.
     * Builds section pages, sets up swipe listeners, and displays the first page.
     *
     * @param article the article to display
     */
    public void openArticle(Article article) {
        openArticleAtSection(article, -1);
    }

    /**
     * Opens an article with an optional initial section selection.
     *
     * @param article      the article to display
     * @param sectionIndex the zero-based section index to open, or a negative
     *                     value to start on the first section page
     */
    public void openArticleAtSection(Article article, int sectionIndex) {
        openArticleAtSection(article, sectionIndex, true);
    }

    /**
     * Opens an article with an optional initial section selection and optional
     * initial section-view callback.
     *
     * @param article      the article to display
     * @param sectionIndex the zero-based section index to open, or a negative
     *                     value to start on the first section page
     * @param notifyOnOpen whether to emit an initial section-view callback for
     *                     the starting section page
     */
    public void openArticleAtSection(Article article, int sectionIndex, boolean notifyOnOpen) {
        currentArticle = article;
        articlePages = new ArrayList<>();
        cachedSuggestionsByPage.clear();
        loadingSuggestionPages.clear();
        recommendationSessionId++;
        articlePageContainer.getChildren().clear();

        // One page per section
        List<Article.Section> sections = article.getSections();
        for (int i = 0; i < sections.size(); i++) {
            ArticlePageBuilder.SectionPage sectionPage = ArticlePageBuilder.createSectionPage(
                    sections.get(i), i + 1, sections.size());
            articlePages.add(sectionPage);
        }

        currentPageIndex = clampPageIndex(sectionIndex >= 0 ? sectionIndex : 0);

        // Add all pages to the container (only the selected page is visible)
        for (int i = 0; i < articlePages.size(); i++) {
            VBox pageRoot = articlePages.get(i).root();
            pageRoot.setVisible(i == currentPageIndex);
            pageRoot.setTranslateX(0);
            articlePageContainer.getChildren().add(pageRoot);
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

        if (notifyOnOpen) {
            notifySectionViewed();
        }

        loadSuggestionsForPage(currentPageIndex);
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
        VBox outgoingPage = articlePages.get(currentPageIndex).root();
        VBox incomingPage = articlePages.get(targetIndex).root();
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
        notifySectionViewed();
        loadSuggestionsForPage(currentPageIndex);
    }

    private int clampPageIndex(int pageIndex) {
        if (articlePages == null || articlePages.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(pageIndex, articlePages.size() - 1));
    }

    private void notifySectionViewed() {
        if (onSectionViewed == null || currentArticle == null) {
            return;
        }

        onSectionViewed.accept(currentArticle, currentPageIndex);
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
        if (articlePages == null || articlePages.isEmpty()) {
            leftArrowLabel.setVisible(false);
            rightArrowLabel.setVisible(false);
            return;
        }
        leftArrowLabel.setVisible(currentPageIndex > 0);
        rightArrowLabel.setVisible(currentPageIndex < articlePages.size() - 1);
    }

    /**
     * Updates the page counter label (e.g. "1 / 5").
     */
    private void updatePageCounter() {
        if (articlePages == null || articlePages.isEmpty()) {
            pageCounterLabel.setText("0 / 0");
            return;
        }
        pageCounterLabel.setText((currentPageIndex + 1) + " / " + articlePages.size());
    }

    private void loadSuggestionsForPage(int pageIndex) {
        if (recommendationProvider == null || currentArticle == null
                || articlePages == null || pageIndex < 0 || pageIndex >= articlePages.size()) {
            return;
        }

        ArticlePageBuilder.SectionPage page = articlePages.get(pageIndex);
        List<SearchResult> cached = cachedSuggestionsByPage.get(pageIndex);
        if (cached != null) {
            renderSuggestedArticles(page, pageIndex, cached);
            return;
        }

        if (loadingSuggestionPages.contains(pageIndex)) {
            showSuggestedArticlesLoading(page);
            return;
        }

        Article requestArticle = currentArticle;
        long sessionId = recommendationSessionId;
        loadingSuggestionPages.add(pageIndex);
        showSuggestedArticlesLoading(page);

        Thread recommendationThread = new Thread(() -> {
            List<SearchResult> recommendations;
            try {
                recommendations = recommendationProvider.recommend(
                        requestArticle,
                        pageIndex,
                        MAX_SUGGESTED_ARTICLES);
            } catch (RuntimeException e) {
                recommendations = List.of();
            }

            List<SearchResult> finalRecommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
            Platform.runLater(() -> {
                if (sessionId != recommendationSessionId || currentArticle != requestArticle
                        || pageIndex < 0 || pageIndex >= articlePages.size()) {
                    return;
                }

                loadingSuggestionPages.remove(pageIndex);
                cachedSuggestionsByPage.put(pageIndex, finalRecommendations);

                if (currentPageIndex == pageIndex) {
                    renderSuggestedArticles(articlePages.get(pageIndex), pageIndex, finalRecommendations);
                }
            });
        });
        recommendationThread.setDaemon(true);
        recommendationThread.start();
    }

    private void showSuggestedArticlesLoading(ArticlePageBuilder.SectionPage page) {
        page.relatedArticlesBox().setVisible(true);
        page.relatedArticlesBox().setManaged(true);
        page.relatedArticlesContainer().getChildren().clear();

        Label loadingLabel = new Label("Finding related articles...");
        loadingLabel.getStyleClass().add("article-related-status");
        page.relatedArticlesContainer().getChildren().add(loadingLabel);
    }

    private void renderSuggestedArticles(ArticlePageBuilder.SectionPage page,
            int pageIndex,
            List<SearchResult> recommendations) {
        page.relatedArticlesContainer().getChildren().clear();
        if (recommendations == null || recommendations.isEmpty()) {
            page.relatedArticlesBox().setVisible(false);
            page.relatedArticlesBox().setManaged(false);
            return;
        }

        page.relatedArticlesBox().setVisible(true);
        page.relatedArticlesBox().setManaged(true);

        String queryHint = buildSuggestionQueryHint(pageIndex);
        for (SearchResult recommendation : recommendations) {
            page.relatedArticlesContainer().getChildren().add(
                    ArticleCardFactory.createArticleCard(
                            recommendation.article(),
                            -1.0,
                            queryHint,
                            recommendation.highlightedTags(),
                            recommendation.preferredMatchedTags(),
                            selectedArticle -> {
                                if (onSuggestedArticleSelected != null) {
                                    onSuggestedArticleSelected.accept(selectedArticle);
                                }
                            }));
        }
    }

    private String buildSuggestionQueryHint(int pageIndex) {
        if (currentArticle == null || pageIndex < 0 || pageIndex >= currentArticle.getSections().size()) {
            return "";
        }

        Article.Section currentSection = currentArticle.getSections().get(pageIndex);
        StringBuilder hint = new StringBuilder();
        hint.append(currentArticle.getTitle()).append(' ').append(currentSection.heading());
        if (!currentArticle.getTags().isEmpty()) {
            hint.append(' ').append(String.join(" ", currentArticle.getTags()));
        }
        if (!currentArticle.getKeywords().isEmpty()) {
            hint.append(' ').append(String.join(" ", currentArticle.getKeywords()));
        }
        return hint.toString().trim();
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

    @FXML
    private void handleLeftArrowClick() {
        navigateToPage(currentPageIndex - 1);
    }

    @FXML
    private void handleRightArrowClick() {
        navigateToPage(currentPageIndex + 1);
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
                    final int pageIndex = j;

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
                final int pageIndex = groupStart;
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
                    if (child instanceof FlowPane flowPane) {
                        for (javafx.scene.Node dot : flowPane.getChildren()) {
                            dot.getStyleClass().remove("nav-menu-dot-active");
                            if (dot.getUserData() instanceof Integer dotPage
                                    && dotPage == currentPageIndex) {
                                wrapper.getStyleClass().add("nav-menu-item-active");
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
