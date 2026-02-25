package com.sddp.sexualhealthapp.calendar.controller;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class EventDetailController {
    
    @FXML private Label nameLabel;
    @FXML private Label typeBadge;
    @FXML private Label dateTimeLabel;

    @FXML private Label descriptionLabel;

    @FXML private VBox dosageBox;
    @FXML private Label dosageLabel;

    private Runnable onBack;
    private CalendarEvent currentEvent;

    public void setOnBack(Runnable onBack){
        this.onBack = onBack;
    }

    public void setEvent(CalendarEvent event){
        this.currentEvent = event;
        render();
    }

    public void render(){
        if (currentEvent == null) return;

        nameLabel.setText(nullToEmpty(currentEvent.getName()));

        EventType type = currentEvent.getType();
        String typeText = (type != null) ? type.getDisplayName() : "Event";
        typeBadge.setText(typeText);

        // coloured background and text
        if (type != null) {
            typeBadge.setStyle("-fx-background-color: " + type.getDotColor() + "22;" + " -fx-text-fill: " + type.getDotColor());
        } else{
            typeBadge.setStyle("");
        }

        // date/time line
        String dateStr = currentEvent.getDate() != null 
                ? currentEvent.getDate().getDayOfMonth() + " " +
                  currentEvent.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                  currentEvent.getDate().getYear()
                : "";

        String timeStr = currentEvent.getTime() != null
                ? currentEvent.getTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "All day";

        dateTimeLabel.setText(dateStr.isBlank() ? timeStr : (dateStr + " . " + timeStr));

        //description
        String descr = nullToEmpty(currentEvent.getDescription());
        descriptionLabel.setText(descr.isBlank() ? "No description" : descr);

        // dosage (only shows if its a medication and has content)
        boolean showDosage = (type == EventType.MEDICATION) && !nullToEmpty(currentEvent.getDosage()).isBlank();
        dosageBox.setVisible(showDosage);
        dosageBox.setManaged(showDosage);
        dosageLabel.setText(showDosage? currentEvent.getDosage(): "");

    }

    @FXML
    private void handleBackToCalendar(ActionEvent e) {
        if (onBack != null) onBack.run();
    }

    private static String nullToEmpty(String s){
        return s == null ? "" : s;
    }

}
