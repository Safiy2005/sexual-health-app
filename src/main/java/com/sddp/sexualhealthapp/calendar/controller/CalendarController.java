package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the calendar grid view (story 47).
 * Displays a monthly calendar with navigation and event indicators.
 * Days with events show coloured dots beneath the day number, one per
 * event type (Appointment=coral, Medication=teal, Test=lavender).
 * Clicking a day selects it and shows that day's events below the grid.
 */
public class CalendarController {

    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 6;
    private static final double ROW_HEIGHT = 48;
    private static final String[] DAY_ABBREVIATIONS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final int YEAR_RANGE_BEFORE = 2;
    private static final int YEAR_RANGE_AFTER = 5;

    @FXML private Label monthYearLabel;
    @FXML private GridPane dayOfWeekHeader;
    @FXML private GridPane calendarGrid;
    @FXML private ScrollPane dayEventsScrollPane;
    @FXML private VBox dayEventsContainer;
    @FXML private HBox monthYearPicker;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;

    private EventStorageService eventStorageService;
    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private boolean pickerVisible = false;
    private boolean suppressPickerEvent = false;

    private Runnable onGoToEventFeed;
    private Runnable onGoToNewEvent;

    @FXML
    private void initialize() {
        eventStorageService = new EventStorageService();
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        setupDayOfWeekHeaders();
        setupGridConstraints();
        setupMonthYearPicker();

        // Make month/year label clickable to toggle the picker
        monthYearLabel.setOnMouseClicked(e -> toggleMonthYearPicker());

        populateCalendar();
    }

    /**
     * Sets up the month and year ComboBoxes for the picker.
     */
    private void setupMonthYearPicker() {
        // Populate months
        for (Month month : Month.values()) {
            monthComboBox.getItems().add(
                    month.getDisplayName(TextStyle.FULL, Locale.getDefault()));
        }

        // Populate years (2 years back, 5 years forward)
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - YEAR_RANGE_BEFORE; y <= currentYear + YEAR_RANGE_AFTER; y++) {
            yearComboBox.getItems().add(y);
        }
    }

    /**
     * Sets up the 7-column day-of-week header row (Mon-Sun).
     */
    private void setupDayOfWeekHeaders() {
        for (int col = 0; col < GRID_COLUMNS; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / GRID_COLUMNS);
            cc.setHalignment(HPos.CENTER);
            dayOfWeekHeader.getColumnConstraints().add(cc);

            Label dayLabel = new Label(DAY_ABBREVIATIONS[col]);
            dayLabel.getStyleClass().add("calendar-dow-label");
            dayOfWeekHeader.add(dayLabel, col, 0);
        }
    }

    /**
     * Sets up column and row constraints for the calendar grid.
     */
    private void setupGridConstraints() {
        for (int col = 0; col < GRID_COLUMNS; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / GRID_COLUMNS);
            cc.setHalignment(HPos.CENTER);
            calendarGrid.getColumnConstraints().add(cc);
        }
        for (int row = 0; row < GRID_ROWS; row++) {
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(ROW_HEIGHT);
            rc.setVgrow(Priority.SOMETIMES);
            calendarGrid.getRowConstraints().add(rc);
        }
    }

    /**
     * Populates the calendar grid for the current month.
     * Rebuilds the grid each time the month changes or selection updates.
     */
    private void populateCalendar() {
        calendarGrid.getChildren().clear();

        // Update month/year header label
        String monthName = currentYearMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
        monthYearLabel.setText(monthName + " " + currentYearMonth.getYear());

        // Sync picker ComboBoxes without triggering their change handlers
        syncPickerToCurrentMonth();

        // Get event type indicators for this month
        Map<Integer, Set<EventType>> eventTypesPerDay =
                eventStorageService.getEventTypesPerDay(currentYearMonth);

        // Calculate the column offset for the 1st of the month (Monday=0 ... Sunday=6)
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentYearMonth.lengthOfMonth();

        LocalDate today = LocalDate.now();

        int dayNumber = 1;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int cellIndex = row * GRID_COLUMNS + col;

                if (cellIndex < startDayOfWeek || dayNumber > daysInMonth) {
                    // Empty cell outside the current month
                    Label empty = new Label("");
                    empty.getStyleClass().add("calendar-day-empty");
                    calendarGrid.add(empty, col, row);
                } else {
                    Set<EventType> types = eventTypesPerDay.getOrDefault(
                            dayNumber, Collections.emptySet());
                    VBox dayCell = createDayCell(dayNumber, today, types);
                    calendarGrid.add(dayCell, col, row);
                    dayNumber++;
                }
            }
        }

        updateDayEvents();
    }

    /**
     * Syncs the month/year picker ComboBoxes to the current month
     * without triggering their onChange handlers.
     */
    private void syncPickerToCurrentMonth() {
        suppressPickerEvent = true;
        monthComboBox.getSelectionModel().select(currentYearMonth.getMonthValue() - 1);
        yearComboBox.getSelectionModel().select(Integer.valueOf(currentYearMonth.getYear()));
        suppressPickerEvent = false;
    }

    /**
     * Creates a single day cell in the calendar grid.
     *
     * @param day        the day-of-month number
     * @param today      today's date for "today" highlighting
     * @param eventTypes the set of event types on this day (empty if none)
     * @return a VBox containing the day number and coloured indicator dots
     */
    private VBox createDayCell(int day, LocalDate today, Set<EventType> eventTypes) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.CENTER);
        cell.getStyleClass().add("calendar-day-cell");

        LocalDate cellDate = currentYearMonth.atDay(day);

        // Day number label
        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.getStyleClass().add("calendar-day-number");

        // Highlight today with teal pill background
        if (cellDate.equals(today)) {
            dayLabel.getStyleClass().add("calendar-day-today");
        }

        // Highlight selected date
        if (cellDate.equals(selectedDate)) {
            cell.getStyleClass().add("calendar-day-selected");
        }

        cell.getChildren().add(dayLabel);

        // Event indicator dots — one per type, coloured by EventType.getDotColor()
        if (!eventTypes.isEmpty()) {
            HBox dotRow = new HBox(3);
            dotRow.setAlignment(Pos.CENTER);
            for (EventType type : EventType.values()) {
                if (eventTypes.contains(type)) {
                    Circle dot = new Circle(3);
                    dot.setStyle("-fx-fill: " + type.getDotColor());
                    dotRow.getChildren().add(dot);
                }
            }
            cell.getChildren().add(dotRow);
        }

        // Click handler to select this day
        cell.setOnMouseClicked(e -> {
            selectedDate = cellDate;
            populateCalendar();
        });

        return cell;
    }

    /**
     * Updates the bottom section to show events for the selected date.
     * Provides a basic event list; story 48 will enhance this into a full event feed.
     */
    private void updateDayEvents() {
        dayEventsContainer.getChildren().clear();

        if (selectedDate == null) return;

        // Selected date header
        Label dateHeader = new Label(
                selectedDate.getDayOfMonth() + " "
                + selectedDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + selectedDate.getYear());
        dateHeader.getStyleClass().add("calendar-selected-date-header");
        dayEventsContainer.getChildren().add(dateHeader);

        List<CalendarEvent> dayEvents = eventStorageService.getEventsForDate(selectedDate);

        if (dayEvents.isEmpty()) {
            Label noEvents = new Label("No events");
            noEvents.getStyleClass().add("calendar-no-events-label");
            dayEventsContainer.getChildren().add(noEvents);
        } else {
            for (CalendarEvent event : dayEvents) {
                VBox eventCard = createEventCard(event);
                dayEventsContainer.getChildren().add(eventCard);
            }
        }
    }

    /**
     * Creates a simple event card for the day events list.
     * Shows the event name, type badge, and time.
     */
    private VBox createEventCard(CalendarEvent event) {
        VBox card = new VBox(4);
        card.getStyleClass().add("calendar-event-card");

        Label nameLabel = new Label(event.getName());
        nameLabel.getStyleClass().add("calendar-event-card-title");
        nameLabel.setWrapText(true);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(event.getType().getDisplayName());
        typeLabel.getStyleClass().add("calendar-event-card-type");
        typeLabel.setStyle("-fx-background-color: " + event.getType().getDotColor() + "22;"
                + " -fx-text-fill: " + event.getType().getDotColor());
        metaRow.getChildren().add(typeLabel);

        if (event.getTime() != null) {
            Label timeLabel = new Label(event.getTime().toString());
            timeLabel.getStyleClass().add("calendar-event-card-time");
            metaRow.getChildren().add(timeLabel);
        }

        card.getChildren().addAll(nameLabel, metaRow);
        return card;
    }

    // --- Navigation handlers ---

    @FXML
    private void handlePreviousMonth(ActionEvent event) {
        currentYearMonth = currentYearMonth.minusMonths(1);
        if (!YearMonth.from(selectedDate).equals(currentYearMonth)) {
            selectedDate = currentYearMonth.atDay(1);
        }
        populateCalendar();
    }

    @FXML
    private void handleNextMonth(ActionEvent event) {
        currentYearMonth = currentYearMonth.plusMonths(1);
        if (!YearMonth.from(selectedDate).equals(currentYearMonth)) {
            selectedDate = currentYearMonth.atDay(1);
        }
        populateCalendar();
    }

    /**
     * Navigates the calendar back to today's date and selects it.
     */
    @FXML
    private void handleGoToToday(ActionEvent event) {
        LocalDate today = LocalDate.now();
        currentYearMonth = YearMonth.from(today);
        selectedDate = today;
        populateCalendar();
    }

    /**
     * Toggles the month/year picker row visibility.
     */
    private void toggleMonthYearPicker() {
        pickerVisible = !pickerVisible;
        monthYearPicker.setVisible(pickerVisible);
        monthYearPicker.setManaged(pickerVisible);
    }

    /**
     * Handles selection changes in the month or year ComboBoxes.
     * Navigates the calendar to the selected month/year.
     */
    @FXML
    private void handleMonthYearChanged(ActionEvent event) {
        if (suppressPickerEvent) return;

        int monthIndex = monthComboBox.getSelectionModel().getSelectedIndex();
        Integer year = yearComboBox.getSelectionModel().getSelectedItem();

        if (monthIndex < 0 || year == null) return;

        currentYearMonth = YearMonth.of(year, monthIndex + 1);

        // Clamp selected day to valid range for the new month
        int clampedDay = Math.min(selectedDate.getDayOfMonth(), currentYearMonth.lengthOfMonth());
        selectedDate = currentYearMonth.atDay(clampedDay);

        populateCalendar();
    }

    // --- Callback setters for navigation (wired by MainAppController) ---

    /**
     * Sets the callback to navigate to the event feed view (story 48).
     *
     * @param callback the callback to run on navigation
     */
    public void setOnGoToEventFeed(Runnable callback) {
        this.onGoToEventFeed = callback;
    }

    /**
     * Sets the callback to navigate to the create-event view (story 22).
     *
     * @param callback the callback to run on navigation
     */
    public void setOnGoToNewEvent(Runnable callback) {
        this.onGoToNewEvent = callback;
    }

    @FXML
    private void handleGoToEventFeed(ActionEvent event) {
        if (onGoToEventFeed != null) {
            onGoToEventFeed.run();
        }
    }

    @FXML
    private void handleGoToNewEvent(ActionEvent event) {
        if (onGoToNewEvent != null) {
            onGoToNewEvent.run();
        }
    }

    /**
     * Refreshes the calendar view. Called externally when events are
     * added or modified by other views (e.g. story 22's event creation form).
     */
    public void refresh() {
        eventStorageService = new EventStorageService();
        populateCalendar();
    }
}
