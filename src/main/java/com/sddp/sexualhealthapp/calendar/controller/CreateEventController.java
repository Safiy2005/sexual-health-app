package com.sddp.sexualhealthapp.calendar.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.model.RecurrenceRule;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Stub controller for the create-event view (story 22).
 * Provides navigation back to the calendar; the full event creation
 * form will be implemented by the assigned teammate.
 * validation, and persistence.
 */
public class CreateEventController {

    // must match feild names in the fxml file
    @FXML
    private TextField titleField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<EventType> typeComboBox;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;
    @FXML
    private VBox dosageContainer;
    @FXML
    private TextField dosageField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<String> recurrenceComboBox;
    @FXML
    private HBox intervalContainer;
    @FXML
    private Spinner<Integer> intervalSpinner;
    @FXML
    private Spinner<Integer> occurrenceCountSpinner;
    @FXML
    private Label intervalLabel;
    @FXML
    private VBox endConditionContainer;
    @FXML
    private ComboBox<String> endTypeComboBox;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private HBox occurrenceContainer;
    @FXML
    private CheckBox allDayCheckBox;
    @FXML
    private HBox timeSpinnerContainer;
    @FXML
    private Label errorLabel;
    @FXML
    private HBox weeklyDaysContainer;
    @FXML
    private ToggleButton btnMon, btnTue, btnWed, btnThu, btnFri, btnSat, btnSun;
    @FXML
    private VBox monthlyOptionsContainer;
    @FXML
    private RadioButton radioSameDay, radioNthWeekday, radioLastDay;
    @FXML
    private VBox exceptionsContainer;
    @FXML
    private DatePicker exceptionDatePicker;
    @FXML
    private ListView<LocalDate> exceptionListView;
    @FXML
    private Label headerLabel;
    @FXML 
    private Button saveButton;

    private final javafx.collections.ObservableList<LocalDate> exceptionDates = javafx.collections.FXCollections
            .observableArrayList();
    private EventStorageService storageService; // for the json
    private Runnable onBackToCalendar;
    private CalendarEvent editingEvent = null;

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll(EventType.values()); // sets dropdown to have values from the model eventtyp
        storageService = EventStorageService.getInstance();

        // makes the whole date bar open the calendar picker, wouldnt before
        datePicker.setOnMouseClicked(e -> datePicker.show());
        datePicker.getEditor().setOnMouseClicked(e -> datePicker.show()); // <-- Tells the text to open it

        centerDatePickerPopup(datePicker);
        centerDatePickerPopup(endDatePicker);
        endDatePicker.setOnMouseClicked(e -> endDatePicker.show());
        endDatePicker.getEditor().setOnMouseClicked(e -> endDatePicker.show()); // <-- Tells the text to open it
        // Hide week numbers to prevent the cells from squishing and truncating text
        datePicker.setShowWeekNumbers(false);
        endDatePicker.setShowWeekNumbers(false);

        exceptionDatePicker.setOnMouseClicked(e -> exceptionDatePicker.show());
        exceptionDatePicker.getEditor().setOnMouseClicked(e -> exceptionDatePicker.show());
        centerDatePickerPopup(exceptionDatePicker);
        exceptionDatePicker.setShowWeekNumbers(false);

        // set up the valules in the time selection spinners
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));

        intervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        occurrenceCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 10));

        recurrenceComboBox.getItems().addAll(
                "Does not repeat",
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly");
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
        // Group monthly radio buttons
        ToggleGroup monthlyGroup = new ToggleGroup();
        radioSameDay.setToggleGroup(monthlyGroup);
        radioNthWeekday.setToggleGroup(monthlyGroup);
        radioLastDay.setToggleGroup(monthlyGroup);

        // Bind exception list view
        exceptionListView.setItems(exceptionDates);

        // 1. Add click-to-remove listener
        exceptionListView.setOnMouseClicked(event -> {
            LocalDate selectedDate = exceptionListView.getSelectionModel().getSelectedItem();
            if (selectedDate != null) {
                exceptionDates.remove(selectedDate);
                exceptionListView.getSelectionModel().clearSelection(); // Clear highlight
            }
        });

        // 2. Custom formatter so it looks clean (e.g., "✖ Feb 26, 2026")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        exceptionListView.setCellFactory(lv -> new ListCell<LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("exception-list-cell");
                } else {
                    setText("✖  " + date.format(formatter));
                    if (!getStyleClass().contains("exception-list-cell")) {
                        getStyleClass().add("exception-list-cell");
                    }
                }
            }
        });

        // UPDATE this listener to dynamically show/hide the advanced options
        recurrenceComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newVal) -> {
            boolean isRepeating = (newVal != null && !newVal.equals("Does not repeat"));
            boolean isWeekly = "Weekly".equals(newVal);
            boolean isMonthly = "Monthly".equals(newVal);

            intervalContainer.setVisible(isRepeating);
            intervalContainer.setManaged(isRepeating);
            endConditionContainer.setVisible(isRepeating);
            endConditionContainer.setManaged(isRepeating);
            exceptionsContainer.setVisible(isRepeating);
            exceptionsContainer.setManaged(isRepeating);

            weeklyDaysContainer.setVisible(isWeekly);
            weeklyDaysContainer.setManaged(isWeekly);
            monthlyOptionsContainer.setVisible(isMonthly);
            monthlyOptionsContainer.setManaged(isMonthly);

            if (isRepeating) {
                intervalLabel.setText(switch (newVal) {
                    case "Daily" -> "days";
                    case "Weekly" -> "weeks";
                    case "Monthly" -> "months";
                    case "Yearly" -> "years";
                    default -> "";
                });
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

    @FXML
    private void handleSaveEvent(ActionEvent event) {
        // 1. Reset visual states
        titleField.getStyleClass().remove("input-error");
        datePicker.getStyleClass().remove("input-error");
        typeComboBox.getStyleClass().remove("input-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // 2. Comprehensive Validation
        boolean hasError = false;
        StringBuilder errorMessage = new StringBuilder("Missing: ");

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            titleField.getStyleClass().add("input-error");
            errorMessage.append("Title, ");
            hasError = true;
        }

        if (datePicker.getValue() == null) {
            datePicker.getStyleClass().add("input-error");
            errorMessage.append("Date, ");
            hasError = true;
        }

        if (typeComboBox.getValue() == null) {
            typeComboBox.getStyleClass().add("input-error");
            errorMessage.append("Type, ");
            hasError = true;
        }

        if (hasError) {
            String finalMsg = errorMessage.substring(0, errorMessage.length() - 2);
            errorLabel.setText(finalMsg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        // 3. Get inputs
        String title = titleField.getText();
        LocalDate date = datePicker.getValue();
        EventType type = typeComboBox.getValue();

        java.time.LocalTime time = null;
        if (!allDayCheckBox.isSelected()) {
            time = java.time.LocalTime.of(
                    hourSpinner.getValueFactory().getValue(),
                    minuteSpinner.getValueFactory().getValue());
        }

        String description = descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText();
        String dosage = dosageField.getText().trim().isEmpty() ? null : dosageField.getText();

        CalendarEvent newEvent = new CalendarEvent(title, date, time, type, description, dosage);

        // 4. Delegate to our new advanced recurrence helper
        applyRecurrence(newEvent);

        // 5. Save
        if (editingEvent != null) {
            // Preserves the original ID
            newEvent.setId(editingEvent.getId());
            storageService.updateEvent(newEvent);
        } else {
            storageService.addEvent(newEvent);
        }
        System.out.println("Full event saved successfully!");

        editingEvent = null;

        handleBackToCalendar(event);
    }

    private void applyRecurrence(CalendarEvent event) {
        String selection = recurrenceComboBox.getValue();
        if (selection == null || "Does not repeat".equals(selection))
            return;

        int interval = intervalSpinner.getValueFactory().getValue();
        RecurrenceRule rule = null;

        switch (selection) {
            case "Daily" -> rule = RecurrenceRule.daily(interval);
            case "Weekly" -> {
                java.util.List<java.time.DayOfWeek> days = new java.util.ArrayList<>();
                if (btnMon.isSelected())
                    days.add(java.time.DayOfWeek.MONDAY);
                if (btnTue.isSelected())
                    days.add(java.time.DayOfWeek.TUESDAY);
                if (btnWed.isSelected())
                    days.add(java.time.DayOfWeek.WEDNESDAY);
                if (btnThu.isSelected())
                    days.add(java.time.DayOfWeek.THURSDAY);
                if (btnFri.isSelected())
                    days.add(java.time.DayOfWeek.FRIDAY);
                if (btnSat.isSelected())
                    days.add(java.time.DayOfWeek.SATURDAY);
                if (btnSun.isSelected())
                    days.add(java.time.DayOfWeek.SUNDAY);

                java.time.DayOfWeek[] daysArray = days.toArray(new java.time.DayOfWeek[0]);
                rule = RecurrenceRule.weekly(interval, daysArray);
            }
            case "Monthly" -> {
                if (radioLastDay.isSelected()) {
                    rule = RecurrenceRule.monthlyOnLastDay(interval);
                } else if (radioNthWeekday.isSelected()) {
                    rule = RecurrenceRule.monthlyOnNthWeekday(interval);
                } else {
                    rule = RecurrenceRule.monthlyOnDay(interval);
                }
            }
            case "Yearly" -> rule = RecurrenceRule.yearly(interval);
        }

        if (rule != null) {
            String endType = endTypeComboBox.getValue();
            if ("On date".equals(endType)) {
                LocalDate endDate = endDatePicker.getValue();
                if (endDate != null && !endDate.isBefore(event.getDate())) {
                    rule.until(endDate);
                }
            } else if ("After occurrences".equals(endType)) {
                rule.times(occurrenceCountSpinner.getValueFactory().getValue());
            }

            // Apply Exceptions using the Set setter from RecurrenceRule.java
            if (!exceptionDates.isEmpty()) {
                rule.setExcludedDates(new java.util.HashSet<>(exceptionDates));
            }

            event.setRecurrenceRule(rule);
        }
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

        // Clear advanced UI elements
        btnMon.setSelected(false);
        btnTue.setSelected(false);
        btnWed.setSelected(false);
        btnThu.setSelected(false);
        btnFri.setSelected(false);
        btnSat.setSelected(false);
        btnSun.setSelected(false);
        radioSameDay.setSelected(true);
        exceptionDates.clear();
        exceptionDatePicker.setValue(null);
    }

    @FXML
    private void handleAddException() {
        LocalDate skipDate = exceptionDatePicker.getValue();
        if (skipDate != null && !exceptionDates.contains(skipDate)) {
            exceptionDates.add(skipDate);
            exceptionDatePicker.setValue(null); // Reset picker
        }
    }

    // has calendar pickers centred popups instead of dynamic drops downs that can
    // fall off the window
    private void centerDatePickerPopup(DatePicker picker) {
        picker.setOnShowing(ev -> {
            // We use Platform.runLater to ensure the popup has been
            // initialized and its width/height are available
            javafx.application.Platform.runLater(() -> {
                // Find the skin and the popup content
                if (picker.getSkin() instanceof javafx.scene.control.skin.DatePickerSkin) {
                    javafx.scene.Node popupContent = ((javafx.scene.control.skin.DatePickerSkin) picker.getSkin())
                            .getPopupContent();

                    if (popupContent != null && popupContent.getScene() != null) {
                        javafx.stage.Window popupWindow = popupContent.getScene().getWindow();
                        javafx.stage.Window mainWindow = picker.getScene().getWindow();

                        // Calculate center coordinates
                        double centerX = mainWindow.getX() + (mainWindow.getWidth() / 2) - (popupWindow.getWidth() / 2);
                        double centerY = mainWindow.getY() + (mainWindow.getHeight() / 2)
                                - (popupWindow.getHeight() / 2);

                        // Position the window
                        popupWindow.setX(centerX);
                        popupWindow.setY(centerY);
                    }
                }
            });
        });
    }


    private void populateFormFromEvent(CalendarEvent e) {
        titleField.setText(e.getName());
        datePicker.setValue(e.getDate());
        typeComboBox.setValue(e.getType());

        // Description and Dosage
        descriptionArea.setText(e.getDescription() == null ? "" : e.getDescription());
        dosageField.setText(e.getDosage() == null ? "" : e.getDosage());

        // Time 
        boolean allDay = (e.getTime() == null);
        allDayCheckBox.setSelected(allDay);

        // Listener will hide or show timeSpinnerContainer
        if (!allDay) {
            hourSpinner.getValueFactory().setValue(e.getTime().getHour());
            minuteSpinner.getValueFactory().setValue(e.getTime().getMinute());
        } else {
            hourSpinner.getValueFactory().setValue(12);
            minuteSpinner.getValueFactory().setValue(0);
        }

        // Recurrence
        RecurrenceRule rule = e.getRecurrenceRule();
        exceptionDates.clear(); // clears the old UI state

        if (rule == null) {
            recurrenceComboBox.getSelectionModel().select("Does not repeat");
            endTypeComboBox.getSelectionModel().select("Never");
            endDatePicker.setValue(null);
            occurrenceCountSpinner.getValueFactory().setValue(10);
            intervalSpinner.getValueFactory().setValue(1);

            // Clears the weekly day toggles and monthly radios
            btnMon.setSelected(false);
            btnTue.setSelected(false);
            btnWed.setSelected(false);
            btnThu.setSelected(false);
            btnFri.setSelected(false);
            btnSat.setSelected(false);
            btnSun.setSelected(false);

            radioSameDay.setSelected(true);
            return ;
        }

        // Interval
        intervalSpinner.getValueFactory().setValue(rule.getInterval());

        // Frequency drop down selection
        switch (rule.getFrequency()) {
            case DAILY -> recurrenceComboBox.getSelectionModel().select("Daily");
            case WEEKLY -> recurrenceComboBox.getSelectionModel().select("Weekly");
            case MONTHLY -> recurrenceComboBox.getSelectionModel().select("Monthly");
            case YEARLY -> recurrenceComboBox.getSelectionModel().select("Yearly");
        }

        // Weekly day toggles
        if (rule.getFrequency() == RecurrenceRule.Frequency.WEEKLY && rule.getDaysOfWeek() != null) {
            Set<java.time.DayOfWeek> days = rule.getDaysOfWeek();
            btnMon.setSelected(days.contains(java.time.DayOfWeek.MONDAY));
            btnTue.setSelected(days.contains(java.time.DayOfWeek.TUESDAY));
            btnWed.setSelected(days.contains(java.time.DayOfWeek.WEDNESDAY));
            btnThu.setSelected(days.contains(java.time.DayOfWeek.THURSDAY));
            btnFri.setSelected(days.contains(java.time.DayOfWeek.FRIDAY));
            btnSat.setSelected(days.contains(java.time.DayOfWeek.SATURDAY));
            btnSun.setSelected(days.contains(java.time.DayOfWeek.SUNDAY));

        }

        // Monthly radio buttons
        if (rule.getFrequency() == RecurrenceRule.Frequency.MONTHLY) {
            if (rule.getMonthlyPattern() == RecurrenceRule.MonthlyPattern.LAST_DAY) {
                radioLastDay.setSelected(true);
            } else if (rule.getMonthlyPattern() == RecurrenceRule.MonthlyPattern.NTH_WEEKDAY) {
                radioNthWeekday.setSelected(true);
            } else {
                radioSameDay.setSelected(true);
            }
        }

        // End conditions
        switch (rule.getEndType()) {
            case NEVER -> {
                endTypeComboBox.getSelectionModel().select("Never");
                endDatePicker.setValue(null);
            }
            case UNTIL -> {
                endTypeComboBox.getSelectionModel().select("On date");
                endDatePicker.setValue(rule.getEndDate());
            }
            case COUNT -> {
                endTypeComboBox.getSelectionModel().select("After occurrences");
                occurrenceCountSpinner.getValueFactory().setValue(rule.getOccurrenceCount());
            }
        }

        // Exceptions
        if (rule.getExcludedDates() != null) {
            exceptionDates.setAll(rule.getExcludedDates());
        }
    }


    public void startEdit(CalendarEvent event) {
        headerLabel.setText("Edit Event");
        saveButton.setText("Save Changes");
        this.editingEvent = event;
        populateFormFromEvent(event);
    }


    public void startCreateNew() {
        headerLabel.setText("New Event");
        saveButton.setText("Save Event");
        editingEvent = null;
        clearForm();
    }
}
