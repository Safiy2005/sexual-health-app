package com.sddp.sexualhealthapp.calendar.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * Stub controller for the create-event view (story 22).
 * Provides navigation back to the calendar; the full event creation
 * form will be implemented by the assigned teammate.
 *
 * TODO (Oli): Implement stories 22.1-22.3 — event creation form,
 * validation, and persistence.
 */
public class CreateEventController {

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
