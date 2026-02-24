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
import javafx.scene.control.Spinner;
import java.time.LocalDate;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.CheckBox;
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
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private VBox dosageContainer;
    @FXML private TextField dosageField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> recurrenceComboBox;
    @FXML private HBox intervalContainer;
    @FXML private Spinner<Integer> intervalSpinner;
    @FXML private Spinner<Integer> occurrenceCountSpinner;
    @FXML private Label intervalLabel;
    @FXML private VBox endConditionContainer;
    @FXML private ComboBox<String> endTypeComboBox;
    @FXML private DatePicker endDatePicker;
    @FXML private HBox occurrenceContainer;
    @FXML private CheckBox allDayCheckBox;
    @FXML private HBox timeSpinnerContainer;

    private EventStorageService storageService;     // for the json
    private Runnable onBackToCalendar;

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll(EventType.values()); // sets dropdown to have values from the model eventtyp
        storageService = new EventStorageService();

        //makes the whole date bar open the calendar picker, wouldnt before
        datePicker.setOnMouseClicked(e -> datePicker.show());
        datePicker.getEditor().setOnMouseClicked(e -> datePicker.show()); // <-- Tells the text to open it

        endDatePicker.setOnMouseClicked(e -> endDatePicker.show());
        endDatePicker.getEditor().setOnMouseClicked(e -> endDatePicker.show()); // <-- Tells the text to open it

        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12));     // set up the valules in the time selection spinners
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));

        intervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        occurrenceCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 10));

        recurrenceComboBox.getItems().addAll(
                "Does not repeat",
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly"
        );
        // Set the default to "Does not repeat" so it isn't blank
        recurrenceComboBox.getSelectionModel().selectFirst();

        // listener to hide time selection if all day event
        allDayCheckBox.selectedProperty().addListener((obs, oldVal, isChecked) -> {
            timeSpinnerContainer.setVisible(!isChecked);
            timeSpinnerContainer.setManaged(!isChecked);
        });

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

            endConditionContainer.setVisible(isRepeating);
            endConditionContainer.setManaged(isRepeating);

            if (isRepeating) {
                switch (newVal) {
                    case "Daily": intervalLabel.setText("days"); break;
                    case "Weekly": intervalLabel.setText("weeks"); break;
                    case "Monthly": intervalLabel.setText("months"); break;
                    case "Yearly": intervalLabel.setText("years"); break;
                }
            }
        });
        endTypeComboBox.getItems().addAll("Never", "On date", "After occurrences");
        endTypeComboBox.getSelectionModel().selectFirst();

        endTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isOnDate = "On date".equals(newVal);
            boolean isAfter = "After occurrences".equals(newVal);

            endDatePicker.setVisible(isOnDate);
            endDatePicker.setManaged(isOnDate);

            occurrenceContainer.setVisible(isAfter);
            occurrenceContainer.setManaged(isAfter);
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

        // gets time if not set to all day
        java.time.LocalTime time = null;
        if (!allDayCheckBox.isSelected()) {
            time = java.time.LocalTime.of(
                    hourSpinner.getValueFactory().getValue(),
                    minuteSpinner.getValueFactory().getValue()
            );
        }

        String description = descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText();
        String dosage = dosageField.getText().trim().isEmpty() ? null : dosageField.getText();

        CalendarEvent newEvent = new CalendarEvent(title, date, time, type, description, dosage);

        String recurrenceSelection = recurrenceComboBox.getValue();
        if (recurrenceSelection != null && !recurrenceSelection.equals("Does not repeat")) {

            // Simply grab the guaranteed valid integer from the spinner!
            int interval = intervalSpinner.getValueFactory().getValue();
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

            String endType = endTypeComboBox.getValue();

            if ("On date".equals(endType)) {
                LocalDate endDate = endDatePicker.getValue();
                // We still need to check if the date is valid/empty
                if (endDate == null || endDate.isBefore(date)) {
                    System.out.println("Validation failed: Please select a valid end date that is after the start date.");
                    return;
                }
                rule.until(endDate);

            } else if ("After occurrences".equals(endType)) {
                // Simply grab the guaranteed valid integer!
                int count = occurrenceCountSpinner.getValueFactory().getValue();
                rule.times(count);
            }

            // attach the rule to the event
            newEvent.setRecurrenceRule(rule);
        }

        storageService.addEvent(newEvent);
        System.out.println("Full event saved successfully!");

        handleBackToCalendar(event);
    }
    // clears contents after leaving the create page
    private void clearForm() {
        titleField.clear();
        datePicker.setValue(null);
        typeComboBox.getSelectionModel().clearSelection();
        hourSpinner.getValueFactory().setValue(12);
        minuteSpinner.getValueFactory().setValue(0);
        descriptionArea.clear();
        dosageField.clear();
        recurrenceComboBox.getSelectionModel().selectFirst();
        endTypeComboBox.getSelectionModel().selectFirst();
        endDatePicker.setValue(null);
        intervalSpinner.getValueFactory().setValue(1);
        occurrenceCountSpinner.getValueFactory().setValue(10);
        allDayCheckBox.setSelected(false);
    }
}

