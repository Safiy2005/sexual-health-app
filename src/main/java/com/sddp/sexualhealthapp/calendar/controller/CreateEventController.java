package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;

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
    @FXML private ComboBox<Integer> hourComboBox;
    @FXML private ComboBox<Integer> minuteComboBox;
    @FXML private VBox dosageContainer;
    @FXML private TextField dosageField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> recurrenceComboBox;
    @FXML private HBox intervalContainer;
    @FXML private TextField intervalField;
    @FXML private Label intervalLabel;

    private EventStorageService storageService;     // for the json
    private Runnable onBackToCalendar;

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll(EventType.values()); // sets dropdown to have values from the model eventtyp
        storageService = new EventStorageService();

        for (int i = 0; i < 24; i++) {          // set hour box to have the hours in 24hr
            hourComboBox.getItems().add(i);
        }
        for (int i = 0; i < 60; i += 5) {
            minuteComboBox.getItems().add(i);       // minutes in 5 min intervals
        }

        recurrenceComboBox.getItems().addAll(
                "Does not repeat",
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly"
        );
        // Set the default to "Does not repeat" so it isn't blank
        recurrenceComboBox.getSelectionModel().selectFirst();

        // listener to show dosage when medication
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean isMedication = (newValue != null && newValue.name().equals("MEDICATION"));

            dosageContainer.setVisible(isMedication);
            dosageContainer.setManaged(isMedication);

            // clear dosage info if type changed, stop accidentally saving this
            if (!isMedication) {
                dosageField.clear();
            }
        });
        //listener for the repeat box
        recurrenceComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isRepeating = (newVal != null && !newVal.equals("Does not repeat"));

            intervalContainer.setVisible(isRepeating);
            intervalContainer.setManaged(isRepeating);

            if (isRepeating) {
                switch (newVal) {
                    case "Daily": intervalLabel.setText("days"); break;
                    case "Weekly": intervalLabel.setText("weeks"); break;
                    case "Monthly": intervalLabel.setText("months"); break;
                    case "Yearly": intervalLabel.setText("years"); break;
                }
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
        clearForm();
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
            System.out.println("Validation failed: fill required fields");
            return;
        }

        java.time.LocalTime time = null;
        if (hourComboBox.getValue() != null && minuteComboBox.getValue() != null) {
            time = java.time.LocalTime.of(hourComboBox.getValue(), minuteComboBox.getValue());
        }

        String description = descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText();
        String dosage = dosageField.getText().trim().isEmpty() ? null : dosageField.getText();

        CalendarEvent newEvent = new CalendarEvent(title, date, time, type, description, dosage);

        String recurrenceSelection = recurrenceComboBox.getValue();
        if (recurrenceSelection != null && !recurrenceSelection.equals("Does not repeat")) {

            int interval;
            try {
                interval = Integer.parseInt(intervalField.getText().trim());

                // block saving if neg or 0
                if (interval < 1) {
                    System.out.println("Validation failed: Interval must be 1 or greater.");
                    return;
                }

            } catch (NumberFormatException e) {
                // block invalid input types (decimals text etc)
                System.out.println("Validation failed: Interval must be a whole number.");
                return;
            }
            com.sddp.sexualhealthapp.calendar.model.RecurrenceRule rule = null;

            switch (recurrenceSelection) {
                case "Daily":
                    rule = com.sddp.sexualhealthapp.calendar.model.RecurrenceRule.daily(interval);
                    break;
                case "Weekly":
                    rule = com.sddp.sexualhealthapp.calendar.model.RecurrenceRule.weekly(interval);
                    break;
                case "Monthly":
                    rule = com.sddp.sexualhealthapp.calendar.model.RecurrenceRule.monthlyOnDay(interval);
                    break;
                case "Yearly":
                    rule = com.sddp.sexualhealthapp.calendar.model.RecurrenceRule.yearly(interval);
                    break;
            }

            // Attach the rule to the event we created in step 5
            newEvent.setRecurrenceRule(rule);


        storageService.addEvent(newEvent);
        System.out.println("Full event saved successfully!");

        handleBackToCalendar(event);
    } }
    // clears contents after leaving the create page
    private void clearForm() {
        titleField.clear();
        datePicker.setValue(null);
        typeComboBox.getSelectionModel().clearSelection();
        hourComboBox.getSelectionModel().clearSelection();
        minuteComboBox.getSelectionModel().clearSelection();
        descriptionArea.clear();
        dosageField.clear();
        recurrenceComboBox.getSelectionModel().selectFirst();
        intervalField.setText("1");
    }
}

