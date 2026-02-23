package com.sddp.sexualhealthapp.calendar.model;

/**
 * Types of events supported by the calendar system.
 * Each type has a display name and a dot colour used by the
 * calendar grid to show per-type event indicators.
 */
public enum EventType {
    APPOINTMENT("Appointment", "#E8836B"),
    MEDICATION("Medication",   "#7DBBB5"),
    TEST("Test",               "#9B8EC4");

    private final String displayName;
    private final String dotColor;

    EventType(String displayName, String dotColor) {
        this.displayName = displayName;
        this.dotColor = dotColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the hex colour used for this type's indicator dot
     * in the calendar grid (e.g. "#E8836B").
     */
    public String getDotColor() {
        return dotColor;
    }
}
