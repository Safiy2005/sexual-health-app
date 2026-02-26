package com.sddp.sexualhealthapp.calendar.model;

import java.time.LocalDate;

/**
 * Represents a single concrete occurrence of a {@link CalendarEvent} on a
 * specific date. For non-recurring events the occurrence date equals the
 * event's stored date; for recurring events it is the expanded date on
 * which the recurrence pattern matches.
 *
 * <p>
 * Used by the event feed (story 48) to display an infinitely-scrollable
 * chronological list of upcoming events, including recurring medication
 * reminders that may repeat indefinitely.
 * </p>
 *
 * @param event          the underlying calendar event
 * @param occurrenceDate the concrete date of this occurrence
 */
public record EventOccurrence(CalendarEvent event, LocalDate occurrenceDate) {
}
