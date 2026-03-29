package com.sddp.sexualhealthapp.mainapp.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.sddp.sexualhealthapp.article.controller.ArticleCardFactory;
import com.sddp.sexualhealthapp.article.controller.ArticleViewController;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.calendar.controller.CalendarController;
import com.sddp.sexualhealthapp.calendar.controller.CreateEventController;
import com.sddp.sexualhealthapp.calendar.controller.EventDetailController;
import com.sddp.sexualhealthapp.calendar.controller.EventFeedController;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.settings.controller.SettingsController;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.model.DisplayMode;
import com.sddp.sexualhealthapp.settings.model.TextSizeLevel;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.settings.service.DisplayModeManager;
import com.sddp.sexualhealthapp.settings.service.DisplaySettingsService;
import com.sddp.sexualhealthapp.settings.service.TextSizeManager;
import com.sddp.sexualhealthapp.settings.service.TextSizeSettingsService;
import com.sddp.sexualhealthapp.util.AppConstants;
import com.sddp.sexualhealthapp.util.SvgIcon;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the main app view with article search.
 * Delegates article display to {@link ArticleViewController}.
 */
public class MainAppController {

    private record BrowseCardData(Article article, List<String> preferredMatches) {
    }

    @FXML
    private StackPane contentStack;
    @FXML
    private StackPane articlesRoot;
    @FXML
    private VBox calendarRoot;
    @FXML
    private VBox settingsRoot;
    @FXML
    private VBox settingsView;
    @FXML
    private SettingsController settingsViewController;
    @FXML
    private ToggleGroup navGroup;
    @FXML
    private ToggleButton articlesTab;
    @FXML
    private ToggleButton calendarTab;
    @FXML
    private ToggleButton settingsTab;
    @FXML
    private Button lockTab;
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
    @FXML
    private VBox eventDetailView;
    @FXML
    private EventDetailController eventDetailViewController;

    // TODO: Remove when story 51 (bottom nav) is integrated
    @FXML
    private VBox calendarView;
    @FXML
    private CalendarController calendarViewController;

    @FXML
    private VBox eventFeedView;
    @FXML
    private EventFeedController eventFeedViewController;
    @FXML
    private VBox createEventView;
    @FXML
    private CreateEventController createEventViewController;

    private HybridSearchService searchService;
    private ContentPreferencesService contentPreferencesService;
    private PauseTransition searchDebounce;
    private boolean isViewTransitioning = false;
    private boolean blockedArticlesExpanded = false;
    private Node returnAfterCreateEvent = null;

    @FXML
    private void initialize() {
        searchService = new HybridSearchService();
        contentPreferencesService = ContentPreferencesService.getInstance();

        // Debounce: wait 300ms after user stops typing before searching
        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(e -> performSearch());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.playFromStart();
        });

        // Wire the article view's back button to return to search
        articleViewController.setOnBackToSearch(this::handleBackToSearch);

        // Wire calendar navigation callbacks
        calendarViewController.setOnGoToEventFeed(() -> {
            eventFeedViewController.refresh();
            showView(eventFeedView, calendarView);
        });
        calendarViewController.setOnGoToNewEvent(() -> {
            returnAfterCreateEvent = calendarView;
            createEventViewController.startCreateNew();
            showView(createEventView, calendarView);
        });

        // Wire stub view back-navigation callbacks
        eventFeedViewController.setOnBackToCalendar(() -> showView(calendarView, eventFeedView));
        eventFeedViewController.setOnEventSelected(
                (event, occurrenceDate) -> openEventDetail(event, occurrenceDate, eventFeedView));
        createEventViewController.setOnBackToCalendar(() -> {
            calendarViewController.refresh();
            eventFeedViewController.refresh();

            // go back to where we started editing or creating
            Node target = (returnAfterCreateEvent != null) ? returnAfterCreateEvent : calendarView;
            showOnlyCalendarView(target);

            returnAfterCreateEvent = null;
        });
        eventDetailViewController.setOnArticleSelected(this::openArticleFromEventDetail);

        // Show all articles on initial load
        showAllArticles();
        settingsViewController.setOnPreferencesChanged(this::refreshArticlesView);

        // icon tabs

        articlesTab.setGraphic(SvgIcon.load("/icons/newspaper.svg", "nav-icon"));
        calendarTab.setGraphic(SvgIcon.load("/icons/calendar.svg", "nav-icon"));
        settingsTab.setGraphic(SvgIcon.load("/icons/settings.svg", "nav-icon"));
        lockTab.setGraphic(SvgIcon.load("/icons/lock.svg", "nav-icon"));

        // icons only
        articlesTab.setText(null);
        calendarTab.setText(null);
        settingsTab.setText(null);
        lockTab.setText(null);

        articlesTab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        calendarTab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        settingsTab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        lockTab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        navGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                navGroup.selectToggle(oldToggle);
                return;
            }

            if (newToggle == articlesTab)
                switchToTab("ARTICLES");
            else if (newToggle == calendarTab)
                switchToTab("CALENDAR");
            else if (newToggle == settingsTab)
                switchToTab("SETTINGS");
        });

        // Default tab
        navGroup.selectToggle(articlesTab);
        switchToTab("ARTICLES");

        calendarViewController.setOnEventSelected(
                (event, occurrenceDate) -> openEventDetail(event, occurrenceDate, calendarView));

        settingsViewController.setOnDisplayModeChanged(this::updateDisplayMode);
        settingsViewController.setOnTextSizeChanged(this::updateTextSize);
        contentStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applySavedDisplayMode();
                applySavedTextSize();
            }
        });


    }

    private void switchToTab(String tab) {
        // Hides all
        setVisible_Managed(articlesRoot, false);
        setVisible_Managed(calendarRoot, false);
        setVisible_Managed(settingsRoot, false);

        // When tab selected
        switch (tab) {
            case "ARTICLES" -> {
                setVisible_Managed(articlesRoot, true);
                refreshArticlesView();
            }
            case "CALENDAR" -> {
                setVisible_Managed(calendarRoot, true);
                closeArticleOverlayIfOpen();
            }
            case "SETTINGS" -> {
                setVisible_Managed(settingsRoot, true);
                closeArticleOverlayIfOpen();
                settingsViewController.refresh();
            }
        }
    }

    private void setVisible_Managed(Node node, boolean on) {
        node.setVisible(on);
        node.setManaged(on);
    }

    private void closeArticleOverlayIfOpen() {
        // Resets the overlay state so it doesnt stick when switches tabs
        articleView.setVisible(false);
        articleView.setTranslateX(0);

        searchView.setVisible(true);
        searchView.setManaged(true);

        isViewTransitioning = false;
    }

    private void showAllArticles() {
        ContentPreferences preferences = contentPreferencesService.getPreferences();
        List<Article> allArticles = ArticleCollection.getInstance().getArticles();
        List<Article> articles = ArticlePersonalizationService.filterBlockedArticles(
                allArticles,
                preferences);
        List<Article> blockedArticles = allArticles.stream()
                .filter(article -> ArticlePersonalizationService.isBlocked(article, preferences))
                .toList();

        if (articles.isEmpty() && blockedArticles.isEmpty()) {
            showEmptyState("No articles found");
            return;
        }

        List<BrowseCardData> visibleCards = articles.stream()
                .map(article -> new BrowseCardData(
                        article,
                        ArticlePersonalizationService.getPreferredMatchedTags(article, preferences)))
                .toList();
        List<BrowseCardData> blockedCards = blockedArticles.stream()
                .map(article -> new BrowseCardData(
                        article,
                        ArticlePersonalizationService.getPreferredMatchedTags(article, preferences)))
                .toList();

        renderBrowseCards(visibleCards, blockedCards);
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
            ContentPreferences preferences = contentPreferencesService.getPreferences();
            List<SearchResult> rawResults = searchService.search(query);
            List<SearchResult> visibleResults = ArticlePersonalizationService.personalizeResults(
                    rawResults, query, preferences);
            List<SearchResult> blockedResults = rawResults.stream()
                    .filter(result -> ArticlePersonalizationService.isBlocked(result.article(), preferences))
                    .toList();

            Platform.runLater(() -> {
                renderSearchResults(query, visibleResults, blockedResults);
            });
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void renderBrowseCards(List<BrowseCardData> visibleCards, List<BrowseCardData> blockedCards) {
        articleListContainer.getChildren().clear();

        if (visibleCards.isEmpty() && !blockedCards.isEmpty()) {
            showEmptyState("All matching articles are currently hidden by blocked tags.");
        }

        for (BrowseCardData cardData : visibleCards) {
            articleListContainer.getChildren().add(
                    ArticleCardFactory.createArticleCard(
                            cardData.article(), -1.0, "",
                            List.of(),
                            cardData.preferredMatches(),
                            this::openArticle));
        }

        addBlockedArticlesToggleForBrowse(blockedCards);
    }

    private void renderSearchResults(String query, List<SearchResult> visibleResults, List<SearchResult> blockedResults) {
        articleListContainer.getChildren().clear();

        if (visibleResults.isEmpty() && blockedResults.isEmpty()) {
            showEmptyState("No results for \"" + query + "\"");
            return;
        }

        if (visibleResults.isEmpty() && !blockedResults.isEmpty()) {
            showEmptyState("All results for \"" + query + "\" are currently hidden by blocked tags.");
        }

        for (SearchResult result : visibleResults) {
            articleListContainer.getChildren().add(
                    ArticleCardFactory.createArticleCard(
                            result.article(), result.score(), query,
                            result.highlightedTags(),
                            result.preferredMatchedTags(),
                            this::openArticle));
        }

        addBlockedArticlesToggleForSearch(query, blockedResults);
    }

    private void addBlockedArticlesToggleForBrowse(List<BrowseCardData> blockedCards) {
        if (blockedCards.isEmpty()) {
            blockedArticlesExpanded = false;
            return;
        }

        articleListContainer.getChildren().add(buildBlockedArticlesToggleButton(blockedCards.size()));
        if (!blockedArticlesExpanded) {
            return;
        }

        for (BrowseCardData cardData : blockedCards) {
            articleListContainer.getChildren().add(
                    ArticleCardFactory.createArticleCard(
                            cardData.article(),
                            -1.0,
                            "",
                            List.of(),
                            cardData.preferredMatches(),
                            buildBlockedReason(cardData.article()),
                            true,
                            this::openArticle));
        }
    }

    private void addBlockedArticlesToggleForSearch(String query, List<SearchResult> blockedResults) {
        if (blockedResults.isEmpty()) {
            blockedArticlesExpanded = false;
            return;
        }

        articleListContainer.getChildren().add(buildBlockedArticlesToggleButton(blockedResults.size()));
        if (!blockedArticlesExpanded) {
            return;
        }

        ContentPreferences preferences = contentPreferencesService.getPreferences();
        for (SearchResult result : blockedResults) {
            articleListContainer.getChildren().add(
                    ArticleCardFactory.createArticleCard(
                            result.article(),
                            result.score(),
                            query,
                            ArticlePersonalizationService.getQueryMatchedTags(result.article(), query),
                            ArticlePersonalizationService.getPreferredMatchedTags(result.article(), preferences),
                            buildBlockedReason(result.article()),
                            true,
                            this::openArticle));
        }
    }

    private Button buildBlockedArticlesToggleButton(int hiddenCount) {
        Button toggleButton = new Button(
                (blockedArticlesExpanded ? "Hide" : "Show")
                        + " hidden articles (" + hiddenCount + ") ");
        toggleButton.getStyleClass().add("article-hidden-toggle-button");
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setOnAction(event -> {
            blockedArticlesExpanded = !blockedArticlesExpanded;
            refreshArticlesView();
        });
        return toggleButton;
    }

    private String buildBlockedReason(Article article) {
        List<String> blockedTags = new ArrayList<>();
        ContentPreferences preferences = contentPreferencesService.getPreferences();

        for (String tag : article.getTags()) {
            for (String blockedTag : preferences.blockedTags()) {
                if (ArticlePersonalizationService.canonicalTagKey(tag)
                        .equals(ArticlePersonalizationService.canonicalTagKey(blockedTag))) {
                    blockedTags.add(tag);
                }
            }
        }

        if (blockedTags.isEmpty()) {
            return "Hidden by blocked tags";
        }
        return "Hidden by blocked tag" + (blockedTags.size() > 1 ? "s: " : ": ")
                + String.join(", ", blockedTags);
    }

    private void openArticle(Article article) {
        if (isViewTransitioning)
            return;
        isViewTransitioning = true;

        articleViewController.openArticle(article);

        // Position article view off-screen to the right, then slide in
        articleView.setTranslateX(AppConstants.APP_WIDTH);
        articleView.setVisible(true);

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(AppConstants.VIEW_SLIDE_MS), articleView);
        slide.setFromX(AppConstants.APP_WIDTH);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.setOnFinished(e -> {
            searchView.setVisible(false);
            isViewTransitioning = false;
        });
        slide.play();
    }

    private void handleBackToSearch() {
        if (isViewTransitioning)
            return;
        isViewTransitioning = true;

        // Make search view visible underneath before sliding article away
        searchView.setVisible(true);

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(AppConstants.VIEW_SLIDE_MS), articleView);
        slide.setFromX(0);
        slide.setToX(AppConstants.APP_WIDTH);
        slide.setInterpolator(Interpolator.EASE_IN);
        slide.setOnFinished(e -> {
            articleView.setVisible(false);
            articleView.setTranslateX(0);
            isViewTransitioning = false;
        });
        slide.play();
    }

    // TODO: Remove when story 51 (bottom nav) replaces this toggle
    @FXML
    private void handleToggleCalendar(ActionEvent event) {
        boolean showCalendar = !calendarView.isVisible();
        calendarView.setVisible(showCalendar);
        searchView.setVisible(!showCalendar);
        articleView.setVisible(false);
        if (showCalendar && calendarViewController != null) {
            calendarViewController.refresh();
        }
    }

    /**
     * Switches between views in the StackPane by showing one and hiding another.
     */
    private void showView(Node show, Node hide) {
        hide.setVisible(false);
        show.setVisible(true);
    }

    @FXML
    private void handleBackToCalculator(ActionEvent event) {
        if (SceneManager.getInstance().isTransitioning())
            return;
        SceneManager.getInstance().transitionToCalculator(AppConstants.LOCK_CROSSFADE_MS);
    }

    private void showEmptyState(String message) {
        Label empty = new Label(message);
        empty.getStyleClass().add("search-empty-label");
        empty.setWrapText(true);
        articleListContainer.getChildren().add(empty);
    }

    private void refreshArticlesView() {
        if (searchField == null) {
            return;
        }

        if (searchField.getText() == null || searchField.getText().trim().isEmpty()) {
            showAllArticles();
        } else {
            performSearch();
        }
    }

    private void showOnlyCalendarView(Node toShow) {
        calendarView.setVisible(false);
        eventFeedView.setVisible(false);
        createEventView.setVisible(false);
        eventDetailView.setVisible(false);

        toShow.setVisible(true);

    }

    private void openEventDetail(CalendarEvent event, LocalDate occurrenceDate, Node returnTo) {
        // set data
        eventDetailViewController.setEvent(event, occurrenceDate);

        // go back where u came from
        eventDetailViewController.setOnBack(() -> showOnlyCalendarView(returnTo));
        // When edit event occurs

        eventDetailViewController.setOnEdit((evnt, occDate) -> {
            returnAfterCreateEvent = returnTo;
            boolean recurring = evnt.getRecurrenceRule() != null;

            if (recurring && occDate != null) {
                createEventViewController.startEditSingleOccurrence(evnt, occDate);
            } else {
                createEventViewController.startEditSeries(evnt);
            }

            showOnlyCalendarView(createEventView);
        });

        // If event deleted
        eventDetailViewController.setOnDelete((evnt, occDate) -> {
            EventStorageService storage = EventStorageService.getInstance();

            boolean recurring = evnt.getRecurrenceRule() != null;

            if (recurring && occDate != null) {
                storage.excludeOccurrence(evnt.getId(), occDate);
            } else {
                storage.deleteEvent(evnt.getId());
            }

            calendarViewController.refresh();
            eventFeedViewController.refresh();
            showOnlyCalendarView(returnTo);

        });
        showOnlyCalendarView(eventDetailView);
    }

    private void openArticleFromEventDetail(Article article) {
        if (article == null) {
            return;
        }

        navGroup.selectToggle(articlesTab);
        switchToTab("ARTICLES");
        closeArticleOverlayIfOpen();
        openArticle(article);
    }

    private void applySavedDisplayMode() {
        if (contentStack == null || contentStack.getScene() == null) {
            return;
        }

        DisplayMode mode = DisplaySettingsService.getInstance().getDisplayMode();
        DisplayModeManager.applyDisplayMode(contentStack.getScene().getRoot(), mode);
    }

    private void updateDisplayMode(DisplayMode mode) {
        DisplaySettingsService.getInstance().setDisplayMode(mode);

        if (contentStack == null || contentStack.getScene() == null) {
            return;
        }

        DisplayModeManager.applyDisplayMode(contentStack.getScene().getRoot(), mode);
    }

    private void applySavedTextSize() {
    if (contentStack == null || contentStack.getScene() == null) {
        return;
    }

    TextSizeLevel level = TextSizeSettingsService.getInstance().getTextSizeLevel();
    TextSizeManager.applyTextSize(contentStack.getScene().getRoot(), level);
}

private void updateTextSize(TextSizeLevel level) {
        TextSizeSettingsService.getInstance().setTextSizeLevel(level);

        if (contentStack == null || contentStack.getScene() == null) {
            return;
        }

        TextSizeManager.applyTextSize(contentStack.getScene().getRoot(), level);
    }
}

