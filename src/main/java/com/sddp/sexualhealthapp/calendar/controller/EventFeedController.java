package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.EventOccurrence;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for the upcoming events feed view (story 48).
 * Displays all events — including recurring medication reminders —
 * from today onwards in chronological order.
 *
 * <p>
 * Because recurring events can produce an infinite number of
 * occurrences, the feed loads lazily: an initial batch of days is
 * loaded on open, and additional batches are fetched automatically
 * as the user scrolls near the bottom.
 * </p>
 */
public class EventFeedController {

    /** Number of days to load per batch. */
    private static final int BATCH_DAYS = 7;

    /** Scroll position threshold (0.0–1.0) that triggers loading the next batch. */
    private static final double LOAD_MORE_THRESHOLD = 0.85;

    @FXML
    private ScrollPane feedScrollPane;
    @FXML
    private VBox feedContainer;

    private EventStorageService eventStorageService;
    private Runnable onBackToCalendar;

    /** The start date for the next batch to load. */
    private LocalDate nextBatchStart;

    /** Guard flag to prevent concurrent batch loads. */
    private boolean loading = false;

    @FXML
    private void initialize() {
        eventStorageService = new EventStorageService();
        nextBatchStart = LocalDate.now();

        loadNextBatch();

        // Lazy-load more events when the user scrolls near the bottom
        feedScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (!loading && newVal.doubleValue() >= LOAD_MORE_THRESHOLD) {
                loadNextBatch();
            }
        });
    }

    /**
     * Sets the callback to navigate back to the calendar view.
     *
     * @param callback the callback to run on back navigation
     */
    public void setOnBackToCalendar(Runnable callback) {
        this.onBackToCalendar = callback;
    }

    @FXML
    private void handleBackToCalendar(ActionEvent event) {
        if (onBackToCalendar != null) {
            onBackToCalendar.run();
        }
    }

    /**
     * Loads the next batch of upcoming event occurrences (covering
     * {@link #BATCH_DAYS} days) and appends them to the feed.
     */
    private void loadNextBatch() {
        loading = true;

        LocalDate batchEnd = nextBatchStart.plusDays(BATCH_DAYS - 1);
        List<EventOccurrence> batch = eventStorageService.getUpcomingOccurrences(nextBatchStart, batchEnd);

        // Show empty-state only when the very first batch has nothing
        if (batch.isEmpty() && feedContainer.getChildren().isEmpty()) {
            Label empty = new Label("No upcoming events");
            empty.getStyleClass().add("calendar-no-events-label");
            feedContainer.getChildren().add(empty);
        }

        for (EventOccurrence occurrence : batch) {
            VBox card = EventCardFactory.createEventCard(occurrence);
            feedContainer.getChildren().add(card);
        }

        nextBatchStart = batchEnd.plusDays(1);
        loading = false;
    }

    /**
     * Refreshes the event feed from scratch. Called externally when
     * events are added or modified by other views.
     */
    public void refresh() {
        eventStorageService = new EventStorageService();
        feedContainer.getChildren().clear();
        nextBatchStart = LocalDate.now();
        loadNextBatch();
    }
}
