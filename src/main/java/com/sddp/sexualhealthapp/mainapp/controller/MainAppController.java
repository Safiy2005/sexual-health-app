package com.sddp.sexualhealthapp.mainapp.controller;

import java.time.LocalDate;
import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sddp.sexualhealthapp.article.controller.ArticleCardFactory;
import com.sddp.sexualhealthapp.article.controller.ArticleViewController;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.ArticleBrowseRankingService;
import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import com.sddp.sexualhealthapp.article.service.ArticleServiceRegistry;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.article.service.RecentlyReadService;
import com.sddp.sexualhealthapp.calendar.controller.CalendarController;
import com.sddp.sexualhealthapp.calendar.controller.CreateEventController;
import com.sddp.sexualhealthapp.calendar.controller.EventDetailController;
import com.sddp.sexualhealthapp.calendar.controller.EventFeedController;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.settings.controller.SettingsController;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;
import com.sddp.sexualhealthapp.util.AppConstants;
import com.sddp.sexualhealthapp.util.SvgIcon;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
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
    private StackPane articleViewHost;
    @FXML
    private VBox calendarRoot;
    @FXML
    private StackPane calendarHost;
    @FXML
    private StackPane eventFeedHost;
    @FXML
    private StackPane createEventHost;
    @FXML
    private StackPane eventDetailHost;
    @FXML
    private VBox settingsRoot;
    @FXML
    private StackPane settingsHost;
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
    private TextField searchField;
    @FXML
    private VBox articleListContainer;
    @FXML
    private ScrollPane listScrollPane;

    private Node articleViewNode;
    private ArticleViewController articleViewController;
    private Node calendarViewNode;
    private CalendarController calendarViewController;
    private Node eventFeedViewNode;
    private EventFeedController eventFeedViewController;
    private Node createEventViewNode;
    private CreateEventController createEventViewController;
    private Node eventDetailViewNode;
    private EventDetailController eventDetailViewController;
    private Node settingsViewNode;
    private SettingsController settingsViewController;

    private HybridSearchService searchService;
    private ArticleBrowseRankingService browseRankingService;
    private ContentPreferencesService contentPreferencesService;
    private RecentlyReadService recentlyReadService = new RecentlyReadService();
    private final ExecutorService recentlyReadWriter = Executors.newSingleThreadExecutor(r -> {
        Thread writerThread = new Thread(r, "recently-read-writer");
        writerThread.setDaemon(true);
        return writerThread;
    });
    private PauseTransition searchDebounce;
    private boolean isViewTransitioning = false;
    private boolean blockedArticlesExpanded = false;
    private boolean recentlyReadFeedDirty = false;
    private Node returnAfterCreateEvent = null;
    private long browseRankingRequestId = 0L;
    private List<Article> cachedBrowseRankedArticles = List.of();
    private boolean initialBrowseRenderPending = false;

    @FXML
    private void initialize() {
        contentPreferencesService = ContentPreferencesService.getInstance();
        initialBrowseRenderPending = true;

        // Debounce: wait 300ms after user stops typing before searching
        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(e -> performSearch());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.playFromStart();
        });

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
    }

    private void switchToTab(String tab) {
        switchToTab(tab, true);
    }

    private void switchToTab(String tab, boolean refreshArticles) {
        // Hides all
        setVisible_Managed(articlesRoot, false);
        setVisible_Managed(calendarRoot, false);
        setVisible_Managed(settingsRoot, false);

        // When tab selected
        switch (tab) {
            case "ARTICLES" -> {
                setVisible_Managed(articlesRoot, true);
                if (refreshArticles && !initialBrowseRenderPending) {
                    refreshArticlesView();
                } else if (articleListContainer != null && articleListContainer.getChildren().isEmpty()) {
                    addBrowseLoadingHint();
                }
            }
            case "CALENDAR" -> {
                setVisible_Managed(calendarRoot, true);
                ensureCalendarViewLoaded();
                closeArticleOverlayIfOpen();
            }
            case "SETTINGS" -> {
                setVisible_Managed(settingsRoot, true);
                ensureSettingsViewLoaded();
                closeArticleOverlayIfOpen();
                settingsViewController.refresh();
            }
        }
    }

    public void showInitialBrowseFeed() {
        if (searchField == null) {
            return;
        }

        if (!initialBrowseRenderPending) {
            return;
        }

        initialBrowseRenderPending = false;
        Platform.runLater(() -> {
            if (searchField.getText() == null || searchField.getText().trim().isEmpty()) {
                renderBrowseFeed();
            } else {
                performSearch();
            }
        });
    }

    private void setVisible_Managed(Node node, boolean on) {
        node.setVisible(on);
        node.setManaged(on);
    }

    private <T> T loadView(StackPane host, String fxmlPath, Consumer<T> controllerConfigurer) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        try {
            Node root = loader.load();
            host.getChildren().setAll(root);
            host.setVisible(true);
            host.setManaged(true);
            @SuppressWarnings("unchecked")
            T controller = (T) loader.getController();
            if (controllerConfigurer != null) {
                controllerConfigurer.accept(controller);
            }
            return controller;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    private void ensureArticleViewLoaded() {
        if (articleViewNode != null) {
            return;
        }

        articleViewController = loadView(articleViewHost, AppConstants.ARTICLE_VIEW_FXML, controller -> {
            controller.setOnBackToSearch(this::handleBackToSearch);
            controller.setOnSectionViewed(this::handleSectionViewed);
            controller.setOnSuggestedArticleSelected(this::openSuggestedArticleInReader);
        });
        articleViewNode = articleViewHost.getChildren().isEmpty() ? null : articleViewHost.getChildren().get(0);
        if (articleViewNode != null) {
            articleViewNode.setVisible(false);
            articleViewNode.setManaged(false);
        }
        articleViewHost.setVisible(false);
        articleViewHost.setManaged(false);
    }

    private void ensureCalendarViewLoaded() {
        if (calendarViewNode != null) {
            return;
        }

        calendarViewController = loadView(calendarHost, AppConstants.CALENDAR_VIEW_FXML, controller -> {
            controller.setOnGoToEventFeed(() -> {
                ensureEventFeedLoaded();
                eventFeedViewController.refresh();
                showView(eventFeedViewNode, calendarViewNode);
            });
            controller.setOnGoToNewEvent(() -> {
                ensureCreateEventLoaded();
                returnAfterCreateEvent = calendarViewNode;
                createEventViewController.startCreateNew();
                showView(createEventViewNode, calendarViewNode);
            });
            controller.setOnEventSelected((event, occurrenceDate) -> openEventDetail(event, occurrenceDate,
                    calendarViewNode));
        });
        calendarViewNode = calendarHost.getChildren().isEmpty() ? null : calendarHost.getChildren().get(0);
    }

    private void ensureEventFeedLoaded() {
        if (eventFeedViewNode != null) {
            return;
        }

        eventFeedViewController = loadView(eventFeedHost, AppConstants.EVENT_FEED_FXML, controller -> {
            controller.setOnBackToCalendar(() -> showView(calendarViewNode, eventFeedViewNode));
            controller.setOnEventSelected((event, occurrenceDate) -> openEventDetail(event, occurrenceDate,
                    eventFeedViewNode));
        });
        eventFeedViewNode = eventFeedHost.getChildren().isEmpty() ? null : eventFeedHost.getChildren().get(0);
        if (eventFeedViewNode != null) {
            eventFeedViewNode.setVisible(false);
            eventFeedViewNode.setManaged(false);
        }
    }

    private void ensureCreateEventLoaded() {
        if (createEventViewNode != null) {
            return;
        }

        createEventViewController = loadView(createEventHost, AppConstants.CREATE_EVENT_FXML, controller -> {
            controller.setOnBackToCalendar(() -> {
                ensureCalendarViewLoaded();
                calendarViewController.refresh();
                ensureEventFeedLoaded();
                eventFeedViewController.refresh();

                Node target = (returnAfterCreateEvent != null) ? returnAfterCreateEvent : calendarViewNode;
                showOnlyCalendarView(target);

                returnAfterCreateEvent = null;
            });
        });
        createEventViewNode = createEventHost.getChildren().isEmpty() ? null : createEventHost.getChildren().get(0);
        if (createEventViewNode != null) {
            createEventViewNode.setVisible(false);
            createEventViewNode.setManaged(false);
        }
    }

    private void ensureEventDetailLoaded() {
        if (eventDetailViewNode != null) {
            return;
        }

        eventDetailViewController = loadView(eventDetailHost, AppConstants.EVENT_DETAIL_FXML, controller -> {
            controller.setOnArticleSelected(this::openArticleFromEventDetail);
        });
        eventDetailViewNode = eventDetailHost.getChildren().isEmpty() ? null : eventDetailHost.getChildren().get(0);
        if (eventDetailViewNode != null) {
            eventDetailViewNode.setVisible(false);
            eventDetailViewNode.setManaged(false);
        }
    }

    private void ensureSettingsViewLoaded() {
        if (settingsViewNode != null) {
            return;
        }

        settingsViewController = loadView(settingsHost, AppConstants.SETTINGS_VIEW_FXML, controller -> {
            controller.setOnPreferencesChanged(this::refreshArticlesView);
        });
        settingsViewNode = settingsHost.getChildren().isEmpty() ? null : settingsHost.getChildren().get(0);
    }

    private void closeArticleOverlayIfOpen() {
        // Resets the overlay state so it doesnt stick when switches tabs
        if (articleViewNode != null) {
            articleViewNode.setVisible(false);
            articleViewNode.setManaged(false);
            articleViewNode.setTranslateX(0);
        }
        if (articleViewHost != null) {
            articleViewHost.setVisible(false);
            articleViewHost.setManaged(false);
        }

        searchView.setVisible(true);
        searchView.setManaged(true);

        isViewTransitioning = false;
        refreshBrowseFeedIfNeeded();
    }

    void renderBrowseFeed() {
        ContentPreferences preferences = getContentPreferencesService().getPreferences();
        List<Article> allArticles = ArticleServiceRegistry.getArticleCollection().getArticles();
        List<RecentlyReadEntry> recentEntries = recentlyReadService.getRecentEntries(5);
        List<Article> initialArticles = pickInitialBrowseOrder(allArticles);
        renderBrowseFeedContent(initialArticles, recentEntries, preferences, !hasUsableCachedBrowseOrder(allArticles));
        startBrowseRankingRefresh(allArticles, recentEntries, preferences);
        recentlyReadFeedDirty = false;
    }

    private void performSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            renderBrowseFeed();
            return;
        }

        // Show loading state
        articleListContainer.getChildren().clear();
        Label loading = new Label("Searching...");
        loading.getStyleClass().add("search-empty-label");
        articleListContainer.getChildren().add(loading);

        // Run search on background thread (ONNX model can be slow on first call)
        Thread searchThread = new Thread(() -> {
            ContentPreferences preferences = getContentPreferencesService().getPreferences();
            List<SearchResult> rawResults = getSearchService().search(query);
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

    private synchronized HybridSearchService getSearchService() {
        if (searchService == null) {
            searchService = ArticleServiceRegistry.getHybridSearchService();
        }
        return searchService;
    }

    private synchronized ArticleBrowseRankingService getBrowseRankingService() {
        if (browseRankingService == null) {
            browseRankingService = new ArticleBrowseRankingService();
        }
        return browseRankingService;
    }

    private synchronized ContentPreferencesService getContentPreferencesService() {
        if (contentPreferencesService == null) {
            contentPreferencesService = ContentPreferencesService.getInstance();
        }
        return contentPreferencesService;
    }

    private List<Article> pickInitialBrowseOrder(List<Article> allArticles) {
        if (hasUsableCachedBrowseOrder(allArticles)) {
            return cachedBrowseRankedArticles;
        }
        return allArticles;
    }

    private boolean hasUsableCachedBrowseOrder(List<Article> allArticles) {
        if (cachedBrowseRankedArticles == null || cachedBrowseRankedArticles.isEmpty()) {
            return false;
        }
        if (cachedBrowseRankedArticles.size() != allArticles.size()) {
            return false;
        }
        return cachedBrowseRankedArticles.containsAll(allArticles)
                && allArticles.containsAll(cachedBrowseRankedArticles);
    }

    private void renderBrowseFeedContent(List<Article> orderedArticles,
            List<RecentlyReadEntry> recentEntries,
            ContentPreferences preferences,
            boolean showLoadingHint) {
        articleListContainer.getChildren().clear();

        List<Article> articles = ArticlePersonalizationService.filterBlockedArticles(
                orderedArticles,
                preferences);
        List<Article> blockedArticles = orderedArticles.stream()
                .filter(article -> ArticlePersonalizationService.isBlocked(article, preferences))
                .toList();

        if (articles.isEmpty() && blockedArticles.isEmpty()) {
            showEmptyState("No articles found");
            return;
        }

        boolean hasVisibleRecent = false;
        if (!recentEntries.isEmpty()) {
            for (RecentlyReadEntry entry : recentEntries) {
                Article article = findArticleById(entry.articleId()).orElse(null);
                if (article != null && !ArticlePersonalizationService.isBlocked(article, preferences)) {
                    if (!hasVisibleRecent) {
                        addRecentlyReadHeader();
                        hasVisibleRecent = true;
                    }
                    articleListContainer.getChildren().add(
                            ArticleCardFactory.createRecentArticleCard(
                                    article,
                                    entry,
                                    this::openRecentArticle,
                                    this::removeRecentArticle));
                }
            }
        }

        addFeedSectionHeader("Articles For You");
        if (showLoadingHint) {
            addBrowseLoadingHint();
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

    private void addBrowseLoadingHint() {
        Label loadingHint = new Label("Personalising articles...");
        loadingHint.getStyleClass().add("article-card-subtitle");
        loadingHint.setWrapText(true);
        articleListContainer.getChildren().add(loadingHint);
    }

    private void addRecentlyReadHeader() {
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("article-feed-section-header");

        Label header = new Label("Recently Read");
        header.getStyleClass().add("article-feed-section-title");
        HBox.setHgrow(header, javafx.scene.layout.Priority.ALWAYS);
        header.setMaxWidth(Double.MAX_VALUE);

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("article-section-action-button");
        clearButton.setOnAction(event -> handleClearRecentlyRead());

        headerRow.getChildren().addAll(header, clearButton);
        articleListContainer.getChildren().add(headerRow);
    }

    private void startBrowseRankingRefresh(List<Article> allArticles,
            List<RecentlyReadEntry> recentEntries,
            ContentPreferences preferences) {
        long requestId = ++browseRankingRequestId;
        List<Article> articleSnapshot = List.copyOf(allArticles);
        List<RecentlyReadEntry> recentSnapshot = List.copyOf(recentEntries);

        Thread rankingThread = new Thread(() -> {
            List<Article> ranked = getBrowseRankingService().rankArticles(
                    articleSnapshot,
                    recentSnapshot,
                    preferences);

            Platform.runLater(() -> {
                if (requestId != browseRankingRequestId) {
                    return;
                }

                cachedBrowseRankedArticles = ranked.stream().collect(Collectors.toUnmodifiableList());
                if (!isBrowseFeedVisible()) {
                    return;
                }

                ContentPreferences latestPreferences = getContentPreferencesService().getPreferences();
                List<RecentlyReadEntry> latestRecentEntries = recentlyReadService.getRecentEntries(5);
                renderBrowseFeedContent(cachedBrowseRankedArticles, latestRecentEntries, latestPreferences, false);
                recentlyReadFeedDirty = false;
            });
        }, "browse-ranking-loader");
        rankingThread.setDaemon(true);
        rankingThread.start();
    }

    private void renderBrowseCards(List<BrowseCardData> visibleCards, List<BrowseCardData> blockedCards) {
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

    private void renderSearchResults(String query, List<SearchResult> visibleResults,
            List<SearchResult> blockedResults) {
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

        ContentPreferences preferences = getContentPreferencesService().getPreferences();
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
        ContentPreferences preferences = getContentPreferencesService().getPreferences();

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
        openArticle(article, null);
    }

    private void openArticle(Article article, Integer resumeSectionIndex) {
        if (isViewTransitioning)
            return;
        isViewTransitioning = true;

        ensureArticleViewLoaded();
        if (articleViewNode == null || articleViewController == null) {
            isViewTransitioning = false;
            return;
        }

        boolean resumeFromSavedSection = resumeSectionIndex != null;
        if (!resumeFromSavedSection) {
            articleViewController.openArticle(article);
        } else {
            articleViewController.openArticleAtSection(article, resumeSectionIndex, false);
        }

        // Position article view off-screen to the right, then slide in
        articleViewHost.setVisible(true);
        articleViewHost.setManaged(true);
        articleViewNode.setManaged(true);
        articleViewNode.setTranslateX(AppConstants.APP_WIDTH);
        articleViewNode.setVisible(true);

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(AppConstants.VIEW_SLIDE_MS), articleViewNode);
        slide.setFromX(AppConstants.APP_WIDTH);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.setOnFinished(e -> {
            searchView.setVisible(false);
            isViewTransitioning = false;
            if (resumeFromSavedSection && article.getFileName() != null) {
                recordRecentlyReadProgress(article.getFileName(), resumeSectionIndex, Instant.now());
            }
        });
        slide.play();
    }

    void renderSearchResults(String query, List<SearchResult> results) {
        articleListContainer.getChildren().clear();

        if (results == null || results.isEmpty()) {
            showEmptyState("No results for \"" + query + "\"");
            return;
        }

        for (SearchResult result : results) {
            articleListContainer.getChildren().add(
                    ArticleCardFactory.createArticleCard(
                            result.article(), result.score(), query, this::openArticle));
        }
    }

    void openRecentArticle(RecentlyReadEntry entry) {
        if (entry == null) {
            return;
        }

        Optional<Article> maybeArticle = findArticleById(entry.articleId());
        if (maybeArticle.isEmpty()) {
            return;
        }

        openArticle(maybeArticle.get(), entry.lastReadSectionIndex());
    }

    private void removeRecentArticle(RecentlyReadEntry entry) {
        if (entry == null || entry.articleId() == null || entry.articleId().isBlank()) {
            return;
        }

        recentlyReadService.removeInMemory(entry.articleId());
        recentlyReadFeedDirty = false;
        renderBrowseFeed();
        recentlyReadWriter.execute(recentlyReadService::flush);
    }

    private Optional<Article> findArticleById(String articleId) {
        return ArticleServiceRegistry.getArticleCollection().getArticles().stream()
                .filter(article -> articleId.equals(article.getFileName()))
                .findFirst();
    }

    private void addFeedSectionHeader(String title) {
        Label header = new Label(title);
        header.getStyleClass().add("article-feed-section-title");
        articleListContainer.getChildren().add(header);
    }

    private void handleClearRecentlyRead() {
        recentlyReadService.clearInMemory();
        recentlyReadFeedDirty = false;
        renderBrowseFeed();
        recentlyReadWriter.execute(recentlyReadService::flush);
    }

    private void handleSectionViewed(Article article, Integer sectionIndex) {
        if (article == null || article.getFileName() == null || sectionIndex == null) {
            return;
        }

        recordRecentlyReadProgress(article.getFileName(), sectionIndex, Instant.now());
    }

    private void handleBackToSearch() {
        if (isViewTransitioning)
            return;
        isViewTransitioning = true;

        // Make search view visible underneath before sliding article away
        searchView.setVisible(true);

        if (articleViewNode == null) {
            isViewTransitioning = false;
            return;
        }

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(AppConstants.VIEW_SLIDE_MS), articleViewNode);
        slide.setFromX(0);
        slide.setToX(AppConstants.APP_WIDTH);
        slide.setInterpolator(Interpolator.EASE_IN);
        slide.setOnFinished(e -> {
            articleViewNode.setVisible(false);
            articleViewNode.setManaged(false);
            articleViewNode.setTranslateX(0);
            if (articleViewHost != null) {
                articleViewHost.setVisible(false);
                articleViewHost.setManaged(false);
            }
            isViewTransitioning = false;
            refreshBrowseFeedIfNeeded();
        });
        slide.play();
    }

    private void recordRecentlyReadProgress(String articleId, int sectionIndex, Instant timestamp) {
        recentlyReadService.saveProgressInMemory(articleId, sectionIndex, timestamp);
        markRecentlyReadFeedDirty();
        recentlyReadWriter.execute(recentlyReadService::flush);
    }

    private void markRecentlyReadFeedDirty() {
        recentlyReadFeedDirty = true;
        refreshBrowseFeedIfNeeded();
    }

    private void refreshBrowseFeedIfNeeded() {
        if (!recentlyReadFeedDirty || !isBrowseFeedVisible()) {
            return;
        }
        renderBrowseFeed();
    }

    private boolean isBrowseFeedVisible() {
        boolean emptySearch = searchField == null || searchField.getText() == null || searchField.getText().isBlank();
        boolean articleOverlayHidden = articleViewNode == null || !articleViewNode.isVisible();
        boolean searchPaneVisible = searchView == null || searchView.isVisible();
        return emptySearch && articleOverlayHidden && searchPaneVisible && !isViewTransitioning;
    }

    /**
     * Switches between views in the StackPane by showing one and hiding another.
     */
    private void showView(Node show, Node hide) {
        setHostedNodeVisible(hide, false);
        setHostedNodeVisible(show, true);
    }

    private void setHostedNodeVisible(Node node, boolean on) {
        if (node == null) {
            return;
        }

        node.setVisible(on);
        node.setManaged(on);

        Node parent = node.getParent();
        if (parent instanceof StackPane host) {
            host.setVisible(on);
            host.setManaged(on);
        }
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
            renderBrowseFeed();
        } else {
            performSearch();
        }
    }

    private void showOnlyCalendarView(Node toShow) {
        setHostedNodeVisible(calendarViewNode, false);
        setHostedNodeVisible(eventFeedViewNode, false);
        setHostedNodeVisible(createEventViewNode, false);
        setHostedNodeVisible(eventDetailViewNode, false);

        setHostedNodeVisible(toShow, true);

    }

    private void openEventDetail(CalendarEvent event, LocalDate occurrenceDate, Node returnTo) {
        // set data
        ensureEventDetailLoaded();
        ensureCreateEventLoaded();
        ensureCalendarViewLoaded();
        ensureEventFeedLoaded();
        if (eventDetailViewController == null || createEventViewController == null || calendarViewController == null
                || eventFeedViewController == null || eventDetailViewNode == null) {
            return;
        }

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

            showOnlyCalendarView(createEventViewNode);
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
        showOnlyCalendarView(eventDetailViewNode);
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

    private void openSuggestedArticleInReader(Article article) {
        if (article == null) {
            return;
        }

        ensureArticleViewLoaded();
        if (articleViewNode == null || articleViewController == null) {
            return;
        }

        articleViewController.openArticle(article);
        articleViewHost.setVisible(true);
        articleViewHost.setManaged(true);
        articleViewNode.setManaged(true);
        articleViewNode.setTranslateX(0);
        articleViewNode.setVisible(true);
        searchView.setVisible(false);
        isViewTransitioning = false;
    }
}
