package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.EventOccurrence;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the upcoming events feed view (story 48).
 * Displays all events - including recurring medication reminders -
 * from today onwards in chronological order.
 *
 * <p>
 * Because recurring events can produce an infinite number of
 * occurrences, the feed loads lazily as the user scrolls.
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

    /** Number of days to query per load window. */
    private static final int BATCH_DAYS = 7;
    /** How many consecutive empty windows to skip while looking ahead/behind. */
    private static final int MAX_EMPTY_WINDOWS_TO_SKIP = 52;
    /** Keep rendered list bounded for smooth scrolling on long sessions. */
    private static final int MAX_RENDERED_EVENT_CARDS = 220;
    /** Target size after trimming old cards from one side. */
    private static final int TRIM_TO_EVENT_CARDS = 170;
    /** Scroll thresholds to load next/previous windows. */
    private static final double LOAD_MORE_THRESHOLD_BOTTOM = 0.85;
    private static final double LOAD_MORE_THRESHOLD_TOP = 0.15;
    /** Delay used to detect end of wheel/trackpad scrolling. */
    private static final Duration SCROLL_IDLE_DELAY = Duration.millis(130);

    private static final DateTimeFormatter DAY_HEADER_FORMATTER = DateTimeFormatter.ofPattern("EEE d MMM",
            Locale.getDefault());

    @FXML
    private ScrollPane feedScrollPane;
    @FXML
    private VBox feedContainer;

    private Runnable onBackToCalendar;

    private LocalDate anchorDate;
    private LocalDate endOfAnchorWeek;
    private LocalDate endOfNextWeek;
    private YearMonth anchorMonth;

    /** Next date to use for forward loading. */
    private LocalDate forwardCursor;
    /** Exclusive upper-bound cursor for backward loading. */
    private LocalDate backwardCursorExclusive;

    private final List<EventOccurrence> loadedOccurrences = new ArrayList<>();
    private boolean loading = false;
    private boolean edgeCheckPending = false;
    private boolean mouseInteractionActive = false;
    private final PauseTransition scrollIdleDebounce = new PauseTransition(SCROLL_IDLE_DELAY);

    static record BatchLoadResult(
            List<EventOccurrence> occurrences,
            LocalDate windowStart,
            LocalDate windowEnd,
            LocalDate nextBatchStart) {
    }

    static record ReverseBatchLoadResult(
            List<EventOccurrence> occurrences,
            LocalDate windowStart,
            LocalDate windowEnd,
            LocalDate nextEndExclusive) {
    }

    @FXML
    private void initialize() {
        refresh();

        scrollIdleDebounce.setOnFinished(e -> requestEdgeLoadCheck());

        feedScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (isNearLoadEdge(newVal.doubleValue())) {
                if (!mouseInteractionActive) {
                    scrollIdleDebounce.playFromStart();
                }
            } else {
                scrollIdleDebounce.stop();
            }
        });

        feedScrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> mouseInteractionActive = true);
        feedScrollPane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            mouseInteractionActive = false;
            if (isNearLoadEdge(feedScrollPane.getVvalue())) {
                requestEdgeLoadCheck();
            }
        });

        feedScrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (isNearLoadEdge(feedScrollPane.getVvalue())) {
                scrollIdleDebounce.playFromStart();
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

    @FXML
    private void handleJumpToToday(ActionEvent event) {
        refresh();
        Platform.runLater(() -> feedScrollPane.setVvalue(0.0));
    }

    /**
     * Refreshes the event feed from scratch. Called externally when
     * events are added or modified by other views.
     */
    public void refresh() {
        EventStorageService.getInstance().reloadFromDisk();
        resetFeedState();

        loadNextBatchFromCursor();
        renderFeedFromModel();
        Platform.runLater(() -> feedScrollPane.setVvalue(0.0));
    }

    private void resetFeedState() {
        anchorDate = LocalDate.now();
        endOfAnchorWeek = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        endOfNextWeek = endOfAnchorWeek.plusWeeks(1);
        anchorMonth = YearMonth.from(anchorDate);

        forwardCursor = anchorDate;
        backwardCursorExclusive = anchorDate;

        loadedOccurrences.clear();
        loading = false;
        edgeCheckPending = false;
    }

    private void requestEdgeLoadCheck() {
        if (loading) {
            edgeCheckPending = true;
            return;
        }
        maybeLoadForCurrentScrollPosition();
    }

    private void maybeLoadForCurrentScrollPosition() {
        if (loading || loadedOccurrences.isEmpty()) {
            return;
        }

        double scrollPos = feedScrollPane.getVvalue();
        boolean changed = false;

        if (scrollPos >= LOAD_MORE_THRESHOLD_BOTTOM) {
            changed = loadNextBatchFromCursor();
            if (changed) {
                trimFromStartIfNeeded();
            }
        } else if (scrollPos <= LOAD_MORE_THRESHOLD_TOP) {
            changed = loadPreviousBatchFromCursor();
            if (changed) {
                trimFromEndIfNeeded();
            }
        }

        if (changed) {
            renderFeedFromModel();
            // Keep the interaction fluid after prepend loads.
            if (scrollPos <= LOAD_MORE_THRESHOLD_TOP) {
                Platform.runLater(() -> feedScrollPane.setVvalue(0.18));
            } else if (scrollPos >= LOAD_MORE_THRESHOLD_BOTTOM) {
                Platform.runLater(() -> feedScrollPane.setVvalue(0.82));
            }
        }

        if (edgeCheckPending) {
            edgeCheckPending = false;
            Platform.runLater(this::requestEdgeLoadCheck);
        }
    }

    private boolean loadNextBatchFromCursor() {
        loading = true;
        try {
            BatchLoadResult result = loadBatchSkippingEmptyWindows(
                    EventStorageService.getInstance(),
                    forwardCursor,
                    BATCH_DAYS,
                    MAX_EMPTY_WINDOWS_TO_SKIP);

            forwardCursor = result.nextBatchStart();
            if (result.occurrences().isEmpty()) {
                return false;
            }

            if (loadedOccurrences.isEmpty()) {
                backwardCursorExclusive = result.windowStart();
            }
            loadedOccurrences.addAll(result.occurrences());
            recomputeCursorsFromLoadedData();
            return true;
        } finally {
            loading = false;
        }
    }

    private boolean loadPreviousBatchFromCursor() {
        if (!backwardCursorExclusive.isAfter(anchorDate)) {
            return false;
        }

        loading = true;
        try {
            ReverseBatchLoadResult result = loadPreviousBatchSkippingEmptyWindows(
                    EventStorageService.getInstance(),
                    backwardCursorExclusive,
                    anchorDate,
                    BATCH_DAYS,
                    MAX_EMPTY_WINDOWS_TO_SKIP);

            backwardCursorExclusive = result.nextEndExclusive();
            if (result.occurrences().isEmpty()) {
                return false;
            }

            loadedOccurrences.addAll(0, result.occurrences());
            recomputeCursorsFromLoadedData();
            return true;
        } finally {
            loading = false;
        }
    }

    static BatchLoadResult loadBatchSkippingEmptyWindows(
            EventStorageService storageService,
            LocalDate startDate,
            int batchDays,
            int maxEmptyWindowsToSkip) {

        LocalDate cursor = startDate;
        int emptyWindowsSkipped = 0;

        while (emptyWindowsSkipped <= maxEmptyWindowsToSkip) {
            LocalDate batchStart = cursor;
            LocalDate batchEnd = batchStart.plusDays(batchDays - 1L);
            List<EventOccurrence> batch = storageService.getUpcomingOccurrences(batchStart, batchEnd);
            cursor = batchEnd.plusDays(1);

            if (!batch.isEmpty()) {
                return new BatchLoadResult(batch, batchStart, batchEnd, cursor);
            }

            emptyWindowsSkipped++;
        }

        LocalDate scannedUntil = cursor.minusDays(1);
        return new BatchLoadResult(List.of(), startDate, scannedUntil, cursor);
    }

    static ReverseBatchLoadResult loadPreviousBatchSkippingEmptyWindows(
            EventStorageService storageService,
            LocalDate endExclusive,
            LocalDate minDate,
            int batchDays,
            int maxEmptyWindowsToSkip) {

        LocalDate cursorEndExclusive = endExclusive;
        int emptyWindowsSkipped = 0;

        while (emptyWindowsSkipped <= maxEmptyWindowsToSkip && cursorEndExclusive.isAfter(minDate)) {
            LocalDate batchStart = cursorEndExclusive.minusDays(batchDays);
            if (batchStart.isBefore(minDate)) {
                batchStart = minDate;
            }

            LocalDate batchEnd = cursorEndExclusive.minusDays(1);
            if (batchEnd.isBefore(batchStart)) {
                break;
            }

            List<EventOccurrence> batch = storageService.getUpcomingOccurrences(batchStart, batchEnd);
            cursorEndExclusive = batchStart;

            if (!batch.isEmpty()) {
                return new ReverseBatchLoadResult(batch, batchStart, batchEnd, cursorEndExclusive);
            }

            emptyWindowsSkipped++;
        }

        LocalDate scannedUntil = endExclusive.minusDays(1);
        return new ReverseBatchLoadResult(List.of(), minDate, scannedUntil, cursorEndExclusive);
    }

    private void recomputeCursorsFromLoadedData() {
        if (loadedOccurrences.isEmpty()) {
            forwardCursor = anchorDate;
            backwardCursorExclusive = anchorDate;
            return;
        }

        backwardCursorExclusive = loadedOccurrences.get(0).occurrenceDate();
        forwardCursor = loadedOccurrences.get(loadedOccurrences.size() - 1).occurrenceDate().plusDays(1);
    }

    private void trimFromStartIfNeeded() {
        if (loadedOccurrences.size() <= MAX_RENDERED_EVENT_CARDS) {
            return;
        }

        while (loadedOccurrences.size() > TRIM_TO_EVENT_CARDS && !loadedOccurrences.isEmpty()) {
            LocalDate firstDate = loadedOccurrences.get(0).occurrenceDate();
            while (!loadedOccurrences.isEmpty() && loadedOccurrences.get(0).occurrenceDate().equals(firstDate)) {
                loadedOccurrences.remove(0);
            }
        }

        recomputeCursorsFromLoadedData();
    }

    private void trimFromEndIfNeeded() {
        if (loadedOccurrences.size() <= MAX_RENDERED_EVENT_CARDS) {
            return;
        }

        while (loadedOccurrences.size() > TRIM_TO_EVENT_CARDS && !loadedOccurrences.isEmpty()) {
            LocalDate lastDate = loadedOccurrences.get(loadedOccurrences.size() - 1).occurrenceDate();
            while (!loadedOccurrences.isEmpty()
                    && loadedOccurrences.get(loadedOccurrences.size() - 1).occurrenceDate().equals(lastDate)) {
                loadedOccurrences.remove(loadedOccurrences.size() - 1);
            }
        }

        recomputeCursorsFromLoadedData();
    }

    private void renderFeedFromModel() {
        feedContainer.getChildren().clear();

        if (loadedOccurrences.isEmpty()) {
            Label empty = new Label("No upcoming events");
            empty.getStyleClass().add("calendar-no-events-label");
            feedContainer.getChildren().add(empty);
            return;
        }

        FeedSection lastRenderedSection = null;
        LocalDate lastRenderedDate = null;

        for (EventOccurrence occurrence : loadedOccurrences) {
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

    private boolean isNearLoadEdge(double scrollPosition) {
        return scrollPosition >= LOAD_MORE_THRESHOLD_BOTTOM || scrollPosition <= LOAD_MORE_THRESHOLD_TOP;
    }
}
