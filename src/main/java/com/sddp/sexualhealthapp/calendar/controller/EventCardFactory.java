package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventOccurrence;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Shared factory for building event card UI nodes, used by both
 * {@link CalendarController} (day events list) and
 * {@link EventFeedController} (upcoming events feed).
 */
public final class EventCardFactory {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private EventCardFactory() {
        // utility class — no instantiation
    }

    /**
     * Creates an event card showing the event name, type badge, and time.
     * This is the default card style used by the calendar day events list.
     *
     * @param event the calendar event to render
     * @return a styled VBox card node
     */
    public static VBox createEventCard(CalendarEvent event) {
        return createEventCard(event, false);
    }

    /**
     * Creates an event card with an optional date subtitle line.
     *
     * @param event    the calendar event to render
     * @param showDate whether to display the event date beneath the title
     * @return a styled VBox card node
     */
    public static VBox createEventCard(CalendarEvent event, boolean showDate) {
        return createEventCard(event, showDate ? event.getDate() : null);
    }

    /**
     * Creates an event card for a concrete {@link EventOccurrence},
     * displaying the occurrence date (which may differ from the event's
     * stored start date for recurring events).
     *
     * @param occurrence the event occurrence to render
     * @return a styled VBox card node
     */
    public static VBox createEventCard(EventOccurrence occurrence) {
        return createEventCard(occurrence.event(), occurrence.occurrenceDate());
    }

    /**
     * Creates an event card with an explicit display date.
     *
     * @param event       the calendar event to render
     * @param displayDate the date to show on the card (null to hide the date line)
     * @return a styled VBox card node
     */
    public static VBox createEventCard(CalendarEvent event, LocalDate displayDate) {
        VBox card = new VBox(4);
        card.getStyleClass().add("calendar-event-card");

        // Event title
        Label nameLabel = new Label(event.getName());
        nameLabel.getStyleClass().add("calendar-event-card-title");
        nameLabel.setWrapText(true);
        card.getChildren().add(nameLabel);

        // Optional date line (used in the event feed)
        if (displayDate != null) {
            String dateText = displayDate.getDayOfMonth() + " "
                    + displayDate.getMonth()
                            .getDisplayName(TextStyle.FULL, Locale.getDefault())
                    + " " + displayDate.getYear();
            Label dateLabel = new Label(dateText);
            dateLabel.getStyleClass().add("calendar-event-card-date");
            card.getChildren().add(dateLabel);
        }

        // Meta row: type badge + time
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(event.getType().getDisplayName());
        typeLabel.getStyleClass().add("calendar-event-card-type");
        typeLabel.setStyle("-fx-background-color: " + event.getType().getDotColor() + "22;"
                + " -fx-text-fill: " + event.getType().getDotColor());
        metaRow.getChildren().add(typeLabel);

        if (event.getTime() != null) {
            Label timeLabel = new Label(event.getTime().format(TIME_FORMATTER));
            timeLabel.getStyleClass().add("calendar-event-card-time");
            metaRow.getChildren().add(timeLabel);
        }

        card.getChildren().add(metaRow);
        return card;
    }
}
