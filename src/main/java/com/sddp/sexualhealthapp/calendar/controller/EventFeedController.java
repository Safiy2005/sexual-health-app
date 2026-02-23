package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
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
 * Displays all events from today onwards in chronological order,
 * including both appointments and medication reminders, so the user
 * can plan around upcoming events at a glance.
 */
public class EventFeedController {

    @FXML
    private ScrollPane feedScrollPane;
    @FXML
    private VBox feedContainer;

    private EventStorageService eventStorageService;
    private Runnable onBackToCalendar;

    @FXML
    private void initialize() {
        eventStorageService = new EventStorageService();
        populateFeed();
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
     * Populates the feed with upcoming events sorted chronologically.
     * Each card shows the event name, date, type badge, and time.
     */
    private void populateFeed() {
        feedContainer.getChildren().clear();

        List<CalendarEvent> upcoming = eventStorageService.getUpcomingEvents(LocalDate.now());

        if (upcoming.isEmpty()) {
            Label empty = new Label("No upcoming events");
            empty.getStyleClass().add("calendar-no-events-label");
            feedContainer.getChildren().add(empty);
            return;
        }

        for (CalendarEvent event : upcoming) {
            VBox card = EventCardFactory.createEventCard(event, true);
            feedContainer.getChildren().add(card);
        }
    }

    /**
     * Refreshes the event feed. Called externally when events are
     * added or modified by other views.
     */
    public void refresh() {
        eventStorageService = new EventStorageService();
        populateFeed();
    }
}
