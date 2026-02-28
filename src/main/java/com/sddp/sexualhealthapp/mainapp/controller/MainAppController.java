package com.sddp.sexualhealthapp.mainapp.controller;

import java.time.LocalDate;
import java.util.List;

import com.sddp.sexualhealthapp.article.controller.ArticleCardFactory;
import com.sddp.sexualhealthapp.article.controller.ArticleViewController;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.article.service.HybridSearchService;
import com.sddp.sexualhealthapp.calendar.controller.CalendarController;
import com.sddp.sexualhealthapp.calendar.controller.CreateEventController;
import com.sddp.sexualhealthapp.calendar.controller.EventDetailController;
import com.sddp.sexualhealthapp.calendar.controller.EventFeedController;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.navigation.SceneManager;
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

    @FXML
    private StackPane contentStack;
    @FXML
    private StackPane articlesRoot;
    @FXML
    private VBox calendarRoot;
    @FXML
    private VBox settingsRoot;
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
    private PauseTransition searchDebounce;
    private boolean isViewTransitioning = false;
    private Node returnAfterCreateEvent = null;

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

        // Wire calendar navigation callbacks
        calendarViewController.setOnGoToEventFeed(() -> {
            eventFeedViewController.refresh();
            showView(eventFeedView, calendarView);
        });
        calendarViewController.setOnGoToNewEvent(() -> {
            returnAfterCreateEvent = calendarView;
            createEventViewController.startCreateNew();
            showView(createEventView,calendarView);
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

        // Show all articles on initial load
        showAllArticles();

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

    }

    private void switchToTab(String tab) {
        // Hides all
        setVisible_Managed(articlesRoot, false);
        setVisible_Managed(calendarRoot, false);
        setVisible_Managed(settingsRoot, false);

        // When tab selected
        switch (tab) {
            case "ARTICLES" -> setVisible_Managed(articlesRoot, true);
            case "CALENDAR" -> {
                setVisible_Managed(calendarRoot, true);
                closeArticleOverlayIfOpen();
            }
            case "SETTINGS" -> {
                setVisible_Managed(settingsRoot, true);
                closeArticleOverlayIfOpen();
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
        eventDetailViewController.setOnEdit(evnt -> {
            returnAfterCreateEvent = returnTo;
            createEventViewController.startEdit(evnt);
            showOnlyCalendarView(createEventView);
        });
        // show detail screen
        showOnlyCalendarView(eventDetailView);
    }

}
