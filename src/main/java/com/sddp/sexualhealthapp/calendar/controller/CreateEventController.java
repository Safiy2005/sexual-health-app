package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Stub controller for the create-event view (story 22).
 * Provides navigation back to the calendar; the full event creation
 * form will be implemented by the assigned teammate.
 *
 * TODO (Oli): Implement stories 22.1-22.3 — event creation form,
 * validation, and persistence.
 */
public class CreateEventController {

    // must match feild names in the fxml file
    @FXML private TextField titleField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<EventType> typeComboBox;

    private EventStorageService storageService;     // for the json
    private Runnable onBackToCalendar;

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll(EventType.values()); // sets dropdown to have values from the model eventtyp
        storageService = new EventStorageService();
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

    @FXML private void handleSaveEvent(ActionEvent event){
        // get inputs
        String title = titleField.getText();
        LocalDate date = datePicker.getValue();
        EventType type = typeComboBox.getValue();

        // validation to prevent a half populated event being saved
        if (title == null || title.trim().isEmpty() || date == null || type == null) {
            System.out.println("Validation failed: Not all fields entered");
            return;
        }

        String uniqueId = UUID.randomUUID().toString();

        CalendarEvent newEvent = new CalendarEvent(title, date, null, type, null, null);

        if (storageService != null) {
            storageService.addEvent(newEvent);
            System.out.println("Event saved successfully!");
        } else {
            System.err.println("Error: EventStorageService was never passed to the controller.");
        }
        handleBackToCalendar(event);
    }
}
