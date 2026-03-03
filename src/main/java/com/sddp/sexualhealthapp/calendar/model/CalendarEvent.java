package com.sddp.sexualhealthapp.calendar.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a calendar event such as an appointment, medication reminder,
 * or test/check-up. This is the core data model shared across the calendar
 * view (story 47), event feed (story 48), event detail pages (story 49),
 * event creation forms (story 22), and medication reminders (story 40).
 */
public class CalendarEvent {

    private String id;
    private String name;
    private LocalDate date;
    private LocalTime time;
    private EventType type;
    private String description;
    private String dosage;
    private RecurrenceRule recurrenceRule;
    private Integer reminderMinutes;
    private LocalDate lastReminderSentDate;

    /**
     * No-arg constructor required by Gson deserialization.
     */
    public CalendarEvent() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Full constructor for creating a new event.
     *
     * @param name        the event title
     * @param date        the event date
     * @param time        the event time (nullable for all-day events)
     * @param type        the event type
     * @param description optional notes (nullable)
     * @param dosage      dosage info, only for MEDICATION type (nullable)
     */
    public CalendarEvent(String name, LocalDate date, LocalTime time,
                         EventType type, String description, String dosage) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.date = date;
        this.time = time;
        this.type = type;
        this.description = description;
        this.dosage = dosage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public Integer getReminderMinutes() {return reminderMinutes;}

    public void setReminderMinutes(Integer reminderMinutes) {this.reminderMinutes = reminderMinutes;}

    public LocalDate getLastReminderSentDate() {return lastReminderSentDate;}
    public void setLastReminderSentDate(LocalDate lastReminderSentDate) {this.lastReminderSentDate = lastReminderSentDate;}

    /**
     * Returns true if this event occurs on the given date, taking into
     * account any recurrence rule. For non-recurring events, this simply
     * checks date equality.
     */
    public boolean occursOn(LocalDate queryDate) {
        if (recurrenceRule != null) {
            return recurrenceRule.occursOn(date, queryDate);
        }
        return date.equals(queryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarEvent that = (CalendarEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CalendarEvent{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", type=" + type +
                (recurrenceRule != null ? ", recurrence=" + recurrenceRule : "") +
                '}';
    }
}
