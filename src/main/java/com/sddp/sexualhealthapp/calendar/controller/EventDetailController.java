package com.sddp.sexualhealthapp.calendar.controller;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.util.EventDetailFormatter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class EventDetailController {

    @FXML private Label nameLabel;
    @FXML private Label typeBadge;
    @FXML private Label dateTimeLabel;

    @FXML private VBox contentRoot;
    @FXML private VBox missingStateRoot;
    @FXML private Label missingStateBodyLabel;
    @FXML private Label missingEventIdLabel;

    @FXML private Label descriptionLabel;

    @FXML private VBox dosageBox;
    @FXML private Label dosageLabel;

    @FXML private VBox recurrenceBox;
    @FXML private Label recurrenceLabel;
    @FXML private VBox confirmDeleteOverlay;
    @FXML private Label confirmDeleteBody;
    @FXML private Button deleteSingleBtn;
    @FXML private Button deleteAllBtn;
    @FXML private javafx.scene.layout.HBox confirmDefaultRow;
    @FXML private javafx.scene.layout.VBox confirmRecurringRow;
    @FXML private VBox confirmEditOverlay;
    @FXML private Label confirmEditBody;
    @FXML private javafx.scene.layout.HBox confirmEdDefaultRow;
    @FXML private javafx.scene.layout.VBox confirmEdRecurringRow;

    private Runnable onBack;
    private CalendarEvent currentEvent;
    private LocalDate occurrenceDate;
    private String missingEventId;
    private java.util.function.BiConsumer<CalendarEvent, LocalDate> onEdit;
    private java.util.function.BiConsumer<CalendarEvent, LocalDate> onDelete;


    public void setOnEdit(java.util.function.BiConsumer<CalendarEvent, LocalDate> onEdit){
        this.onEdit = onEdit;
    }

    public void setOnDelete(java.util.function.BiConsumer<CalendarEvent, LocalDate> onDelete){
        this.onDelete = onDelete;
    }

    public void setOnBack(Runnable onBack){
        this.onBack = onBack;
    }

    public void setEvent(CalendarEvent event){
        setEvent(event, null);
    }

    public void setEvent(CalendarEvent event, LocalDate occurrenceDate) {
        this.currentEvent = event;
        this.occurrenceDate = occurrenceDate;
        this.missingEventId = null;
        render();
    }

    public void showMissingEventState(String eventId) {
        this.currentEvent = null;
        this.occurrenceDate = null;
        this.missingEventId = trimToNull(eventId);
        render();
    }

    private void hideDeleteOverlay() {
        confirmDeleteOverlay.setVisible(false);
        confirmDeleteOverlay.setManaged(false);
    }

    private void hideEditOverlay() {
        if (confirmEditOverlay != null) {
            confirmEditOverlay.setVisible(false);
            confirmEditOverlay.setManaged(false);
        }
    }

    public void render(){
        hideDeleteOverlay();
        hideEditOverlay();
        if (currentEvent == null) {
            showMissingState();
            return;
        }

        showContentState();

        nameLabel.setText(EventDetailFormatter.formatEventName(currentEvent.getName()));

        EventType type = currentEvent.getType();
        String typeText = (type != null) ? type.getDisplayName() : "Event";
        typeBadge.setText(typeText);

        if (type != null) {
            typeBadge.setStyle("-fx-background-color: " + type.getDotColor() + "22;" + " -fx-text-fill: " + type.getDotColor());
        } else{
            typeBadge.setStyle("-fx-background-color: #D4E8E5; -fx-text-fill: #3D7A75;");
        }

        dateTimeLabel.setText(
                EventDetailFormatter.formatDateTime(
                        currentEvent.getDate(),
                        occurrenceDate,
                        currentEvent.getTime(),
                        Locale.getDefault()));

        descriptionLabel.setText(EventDetailFormatter.formatDescription(currentEvent.getDescription()));

        boolean showDosage = EventDetailFormatter.shouldShowDosage(type, currentEvent.getDosage());
        dosageBox.setVisible(showDosage);
        dosageBox.setManaged(showDosage);
        dosageLabel.setText(showDosage ? EventDetailFormatter.formatDosage(currentEvent.getDosage()) : "");

        Optional<String> recurrenceText = EventDetailFormatter.formatRecurrence(
                currentEvent.getRecurrenceRule(),
                currentEvent.getDate(),
                Locale.getDefault());
        boolean showRecurrence = recurrenceText.isPresent();
        recurrenceBox.setVisible(showRecurrence);
        recurrenceBox.setManaged(showRecurrence);
        recurrenceLabel.setText(recurrenceText.orElse(""));
    }

    private void showContentState() {
        contentRoot.setVisible(true);
        contentRoot.setManaged(true);
        missingStateRoot.setVisible(false);
        missingStateRoot.setManaged(false);
    }

    private void showMissingState() {
        contentRoot.setVisible(false);
        contentRoot.setManaged(false);
        missingStateRoot.setVisible(true);
        missingStateRoot.setManaged(true);

        missingStateBodyLabel.setText("This event could not be loaded. It may have been deleted or is no longer available.");
        String idText = trimToNull(missingEventId);
        boolean showId = idText != null;
        missingEventIdLabel.setVisible(showId);
        missingEventIdLabel.setManaged(showId);
        missingEventIdLabel.setText(showId ? "Event ID: " + idText : "");
    }

    @FXML
    private void handleBackToCalendar(ActionEvent e) {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void handleEditEvent(ActionEvent e) {
        if (currentEvent == null) return;

        // If recurring and we have an occurence date, show a different message
        boolean recurring = currentEvent.getRecurrenceRule() != null;
        boolean hasOcc = occurrenceDate != null;

        boolean showRecurringChoice = recurring && hasOcc;

        // message

        confirmEditBody.setText(showRecurringChoice ? "Edit only this occurrence, or the whole series?"
                                                      : "Edit this event?.");  
        
        
        // toggle rows
        confirmEdDefaultRow.setVisible(!showRecurringChoice);
        confirmEdDefaultRow.setManaged(!showRecurringChoice);

        confirmEdRecurringRow.setVisible(showRecurringChoice);
        confirmEdRecurringRow.setManaged(showRecurringChoice);

        // show overlay
        confirmEditOverlay.setVisible(true);
        confirmEditOverlay.setManaged(true);
    }

    @FXML
    private void handleCancelEdit(ActionEvent e) {
        confirmEditOverlay.setVisible(false);
        confirmEditOverlay.setManaged(false);
    }

    @FXML
    private void handleConfirmEdit(ActionEvent e) {
        // non-recurring -> just edit
        handleCancelEdit(e);
        if (onEdit != null && currentEvent != null) {
            onEdit.accept(currentEvent, null); // treating it as edit series
        }
        
    }

    @FXML
    private void handleConfirmEditSingle(ActionEvent e){
        handleCancelEdit(e);
        if (onEdit != null && currentEvent != null) {
            onEdit.accept(currentEvent, occurrenceDate); 
        }
    }

    @FXML
    private void handleConfirmEditAll(ActionEvent e){
        handleCancelEdit(e);
        if (onEdit != null && currentEvent != null) {
            onEdit.accept(currentEvent, null); 
        }
    }
    

    @FXML
    private void handleDeleteEvent(ActionEvent e) {
        if (currentEvent == null) return;

        // If recurring and we have an occurence date, show a different message
        boolean recurring = currentEvent.getRecurrenceRule() != null;
        boolean hasOcc = occurrenceDate != null;

        boolean showRecurringChoice = recurring && hasOcc;

        // message

        confirmDeleteBody.setText(showRecurringChoice ? "Delete only this occurrence, or the whole series?"
                                                      : "This cannot be undone.");  
        
        
        // toggle rows
        confirmDefaultRow.setVisible(!showRecurringChoice);
        confirmDefaultRow.setManaged(!showRecurringChoice);

        confirmRecurringRow.setVisible(showRecurringChoice);
        confirmRecurringRow.setManaged(showRecurringChoice);

        // show overlay
        confirmDeleteOverlay.setVisible(true);
        confirmDeleteOverlay.setManaged(true);
    }

    @FXML
    private void handleCancelDelete(ActionEvent e) {
        confirmDeleteOverlay.setVisible(false);
        confirmDeleteOverlay.setManaged(false);
    }

    @FXML
    private void handleConfirmDelete(ActionEvent e) {
        
        confirmDeleteOverlay.setVisible(false);
        confirmDeleteOverlay.setManaged(false);

        if (onDelete != null && currentEvent != null) {
            onDelete.accept(currentEvent, occurrenceDate);
        }
    }

    @FXML
    private void handleConfirmDeleteSingle(ActionEvent e){
        handleCancelDelete(e);

        if(onDelete != null) {
            onDelete.accept(currentEvent, occurrenceDate);
        }
    }

    @FXML
    private void handleConfirmDeleteAll(ActionEvent e){
        handleCancelDelete(e);

        if (onDelete != null) {
            onDelete.accept(currentEvent, null);
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
