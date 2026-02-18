package com.sddp.sexualhealthapp.calendar.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * Stub controller for the event feed view (story 48).
 * Provides navigation back to the calendar; the full event feed
 * will be implemented by the assigned teammate.
 *
 * TODO (Taran): Implement stories 48.1-48.3 — event feed list,
 * filtering, and detail navigation.
 */
public class EventFeedController {

    private Runnable onBackToCalendar;

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
}
