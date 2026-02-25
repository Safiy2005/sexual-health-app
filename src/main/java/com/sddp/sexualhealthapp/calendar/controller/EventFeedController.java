package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.EventOccurrence;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

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

    private enum FeedSection {
        TODAY("Today"),
        TOMORROW("Tomorrow"),
        THIS_WEEK("This week"),
        NEXT_WEEK("Next week"),
        THIS_MONTH("This month"),
        LATER("Later");

        private final String label;

        FeedSection(String label) {
            this.label = label;
        }
    }

    /** Number of days to load per batch. */
    private static final int BATCH_DAYS = 7;
    /** How many consecutive empty 7-day windows to skip while looking ahead. */
    private static final int MAX_EMPTY_WINDOWS_TO_SKIP = 52;
    private static final DateTimeFormatter DAY_HEADER_FORMATTER = DateTimeFormatter.ofPattern("EEE d MMM",
            Locale.getDefault());

    /** Scroll position threshold (0.0–1.0) that triggers loading the next batch. */
    private static final double LOAD_MORE_THRESHOLD = 0.85;

    @FXML
    private ScrollPane feedScrollPane;
    @FXML
    private VBox feedContainer;

    private Runnable onBackToCalendar;

    /** The start date for the next batch to load. */
    private LocalDate nextBatchStart;
    private LocalDate anchorDate;
    private LocalDate endOfAnchorWeek;
    private LocalDate endOfNextWeek;
    private YearMonth anchorMonth;
    private FeedSection lastRenderedSection;
    private LocalDate lastRenderedDate;

    /** Guard flag to prevent concurrent batch loads. */
    private boolean loading = false;

    static record BatchLoadResult(List<EventOccurrence> occurrences, LocalDate nextBatchStart) {
    }

    @FXML
    private void initialize() {
        resetFeedState();

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

        BatchLoadResult result = loadBatchSkippingEmptyWindows(
                nextBatchStart,
                BATCH_DAYS,
                MAX_EMPTY_WINDOWS_TO_SKIP);
        List<EventOccurrence> batch = result.occurrences();
        nextBatchStart = result.nextBatchStart();

        // Show empty-state only when no events were found within the look-ahead range
        if (batch.isEmpty() && feedContainer.getChildren().isEmpty()) {
            Label empty = new Label("No upcoming events");
            empty.getStyleClass().add("calendar-no-events-label");
            feedContainer.getChildren().add(empty);
        }

        for (EventOccurrence occurrence : batch) {
            appendOccurrence(occurrence);
        }
        loading = false;
    }

    static BatchLoadResult loadBatchSkippingEmptyWindows(
            LocalDate startDate,
            int batchDays,
            int maxEmptyWindowsToSkip) {

        LocalDate cursor = startDate;
        int emptyWindowsSkipped = 0;

        while (emptyWindowsSkipped <= maxEmptyWindowsToSkip) {
            LocalDate batchEnd = cursor.plusDays(batchDays - 1L);
            List<EventOccurrence> batch = EventStorageService.getInstance().getUpcomingOccurrences(cursor, batchEnd);
            cursor = batchEnd.plusDays(1);

            if (!batch.isEmpty()) {
                return new BatchLoadResult(batch, cursor);
            }

            emptyWindowsSkipped++;
        }

        return new BatchLoadResult(List.of(), cursor);
    }

    /**
     * Refreshes the event feed from scratch. Called externally when
     * events are added or modified by other views.
     */
    public void refresh() {
        EventStorageService.getInstance().reloadFromDisk();
        feedContainer.getChildren().clear();
        resetFeedState();
        loadNextBatch();
    }

    private void resetFeedState() {
        anchorDate = LocalDate.now();
        endOfAnchorWeek = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        endOfNextWeek = endOfAnchorWeek.plusWeeks(1);
        anchorMonth = YearMonth.from(anchorDate);
        nextBatchStart = anchorDate;
        lastRenderedSection = null;
        lastRenderedDate = null;
    }

    private void appendOccurrence(EventOccurrence occurrence) {
        LocalDate occurrenceDate = occurrence.occurrenceDate();
        FeedSection section = classifySection(occurrenceDate);

        if (section != lastRenderedSection) {
            feedContainer.getChildren().add(createSectionHeader(section));
            lastRenderedSection = section;
            lastRenderedDate = null;
        }

        if (!occurrenceDate.equals(lastRenderedDate)) {
            feedContainer.getChildren().add(createDayHeader(occurrenceDate));
            lastRenderedDate = occurrenceDate;
        }

        VBox card = EventCardFactory.createEventCard(occurrence.event(), (LocalDate) null);
        card.getStyleClass().add("event-feed-card");
        feedContainer.getChildren().add(card);
    }

    private FeedSection classifySection(LocalDate date) {
        if (date.equals(anchorDate)) {
            return FeedSection.TODAY;
        }
        if (date.equals(anchorDate.plusDays(1))) {
            return FeedSection.TOMORROW;
        }
        if (!date.isAfter(endOfAnchorWeek)) {
            return FeedSection.THIS_WEEK;
        }
        if (!date.isAfter(endOfNextWeek)) {
            return FeedSection.NEXT_WEEK;
        }
        if (YearMonth.from(date).equals(anchorMonth)) {
            return FeedSection.THIS_MONTH;
        }
        return FeedSection.LATER;
    }

    private HBox createSectionHeader(FeedSection section) {
        Label sectionLabel = new Label(section.label);
        sectionLabel.getStyleClass().add("event-feed-section-header");

        Region divider = new Region();
        divider.getStyleClass().add("event-feed-section-divider");
        HBox.setHgrow(divider, Priority.ALWAYS);

        HBox header = new HBox(8, sectionLabel, divider);
        header.getStyleClass().add("event-feed-section-row");
        return header;
    }

    private Label createDayHeader(LocalDate date) {
        Label dayHeader = new Label(formatDayHeader(date));
        dayHeader.getStyleClass().add("event-feed-day-header");
        return dayHeader;
    }

    private String formatDayHeader(LocalDate date) {
        String absoluteDate = date.format(DAY_HEADER_FORMATTER);
        if (date.equals(anchorDate)) {
            return "Today - " + absoluteDate;
        }
        if (date.equals(anchorDate.plusDays(1))) {
            return "Tomorrow - " + absoluteDate;
        }
        return absoluteDate;
    }
}
