package com.sddp.sexualhealthapp.calendar.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CalendarEvent model class.
 * Verifies constructors, field access, equality, and nullable fields.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
class CalendarEventTest {

    @Test
    void testFullConstructor_SetsAllFields() {
        CalendarEvent event = new CalendarEvent(
                "GP Check-up", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 30), EventType.APPOINTMENT,
                "Annual screening", null);

        assertEquals("GP Check-up", event.getName());
        assertEquals(LocalDate.of(2026, 3, 15), event.getDate());
        assertEquals(LocalTime.of(10, 30), event.getTime());
        assertEquals(EventType.APPOINTMENT, event.getType());
        assertEquals("Annual screening", event.getDescription());
        assertNull(event.getDosage());
        assertNotNull(event.getId());
    }

    @Test
    void testFullConstructor_GeneratesUniqueIds() {
        CalendarEvent event1 = new CalendarEvent(
                "Event 1", LocalDate.now(), null, EventType.TEST, null, null);
        CalendarEvent event2 = new CalendarEvent(
                "Event 2", LocalDate.now(), null, EventType.TEST, null, null);

        assertNotEquals(event1.getId(), event2.getId());
    }

    @Test
    void testNoArgConstructor_GeneratesId() {
        CalendarEvent event = new CalendarEvent();

        assertNotNull(event.getId());
        assertFalse(event.getId().isEmpty());
    }

    @Test
    void testNoArgConstructor_FieldsAreNull() {
        CalendarEvent event = new CalendarEvent();

        assertNull(event.getName());
        assertNull(event.getDate());
        assertNull(event.getTime());
        assertNull(event.getType());
        assertNull(event.getDescription());
        assertNull(event.getDosage());
    }

    @Test
    void testSetters_ModifyFields() {
        CalendarEvent event = new CalendarEvent();
        event.setName("PrEP Dose");
        event.setDate(LocalDate.of(2026, 4, 1));
        event.setTime(LocalTime.of(8, 0));
        event.setType(EventType.MEDICATION);
        event.setDescription("Take with food");
        event.setDosage("200mg");

        assertEquals("PrEP Dose", event.getName());
        assertEquals(LocalDate.of(2026, 4, 1), event.getDate());
        assertEquals(LocalTime.of(8, 0), event.getTime());
        assertEquals(EventType.MEDICATION, event.getType());
        assertEquals("Take with food", event.getDescription());
        assertEquals("200mg", event.getDosage());
    }

    @Test
    void testSetId_OverridesGeneratedId() {
        CalendarEvent event = new CalendarEvent();
        String originalId = event.getId();
        event.setId("custom-id-123");

        assertEquals("custom-id-123", event.getId());
        assertNotEquals(originalId, event.getId());
    }

    @Test
    void testNullableFields_TimeCanBeNull() {
        CalendarEvent event = new CalendarEvent(
                "All-day event", LocalDate.now(), null,
                EventType.APPOINTMENT, null, null);

        assertNull(event.getTime());
    }

    @Test
    void testNullableFields_DescriptionCanBeNull() {
        CalendarEvent event = new CalendarEvent(
                "Quick event", LocalDate.now(), LocalTime.NOON,
                EventType.TEST, null, null);

        assertNull(event.getDescription());
    }

    @Test
    void testNullableFields_DosageCanBeNull() {
        CalendarEvent event = new CalendarEvent(
                "Appointment", LocalDate.now(), LocalTime.NOON,
                EventType.APPOINTMENT, "Clinic visit", null);

        assertNull(event.getDosage());
    }

    @Test
    void testDosageField_ForMedication() {
        CalendarEvent event = new CalendarEvent(
                "Morning PrEP", LocalDate.now(), LocalTime.of(8, 0),
                EventType.MEDICATION, "Daily dose", "200mg");

        assertEquals("200mg", event.getDosage());
    }

    @Test
    void testEquals_SameId() {
        CalendarEvent event1 = new CalendarEvent();
        CalendarEvent event2 = new CalendarEvent();
        event2.setId(event1.getId());

        assertEquals(event1, event2);
    }

    @Test
    void testEquals_DifferentId() {
        CalendarEvent event1 = new CalendarEvent(
                "Event", LocalDate.now(), null, EventType.TEST, null, null);
        CalendarEvent event2 = new CalendarEvent(
                "Event", LocalDate.now(), null, EventType.TEST, null, null);

        assertNotEquals(event1, event2);
    }

    @Test
    void testEquals_SameInstance() {
        CalendarEvent event = new CalendarEvent();

        assertEquals(event, event);
    }

    @Test
    void testEquals_Null() {
        CalendarEvent event = new CalendarEvent();

        assertNotEquals(event, null);
    }

    @Test
    void testEquals_DifferentType() {
        CalendarEvent event = new CalendarEvent();

        assertNotEquals(event, "not an event");
    }

    @Test
    void testHashCode_ConsistentWithEquals() {
        CalendarEvent event1 = new CalendarEvent();
        CalendarEvent event2 = new CalendarEvent();
        event2.setId(event1.getId());

        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testHashCode_DifferentForDifferentIds() {
        CalendarEvent event1 = new CalendarEvent();
        CalendarEvent event2 = new CalendarEvent();

        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testToString_ContainsKeyFields() {
        CalendarEvent event = new CalendarEvent(
                "Check-up", LocalDate.of(2026, 3, 10), null,
                EventType.APPOINTMENT, null, null);

        String str = event.toString();
        assertTrue(str.contains("Check-up"));
        assertTrue(str.contains("2026-03-10"));
        assertTrue(str.contains("APPOINTMENT"));
        assertTrue(str.contains(event.getId()));
    }

    // --- occursOn (AC: day cell renders / events listed / recurrence dots) ---

    @Test
    void testOccursOn_NonRecurring_MatchesExactDate() {
        CalendarEvent event = new CalendarEvent(
                "Check-up", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0), EventType.APPOINTMENT, null, null);

        assertTrue(event.occursOn(LocalDate.of(2026, 3, 15)));
    }

    @Test
    void testOccursOn_NonRecurring_DoesNotMatchDifferentDate() {
        CalendarEvent event = new CalendarEvent(
                "Check-up", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0), EventType.APPOINTMENT, null, null);

        assertFalse(event.occursOn(LocalDate.of(2026, 3, 16)));
    }

    @Test
    void testOccursOn_DailyRecurrence_MatchesSubsequentDays() {
        CalendarEvent event = new CalendarEvent(
                "PrEP Dose", LocalDate.of(2026, 2, 1),
                LocalTime.of(8, 0), EventType.MEDICATION, null, null);
        event.setRecurrenceRule(RecurrenceRule.daily());

        assertTrue(event.occursOn(LocalDate.of(2026, 2, 1)));
        assertTrue(event.occursOn(LocalDate.of(2026, 2, 15)));
        assertTrue(event.occursOn(LocalDate.of(2026, 3, 10)));
    }

    @Test
    void testOccursOn_DailyRecurrence_DoesNotMatchBeforeStart() {
        CalendarEvent event = new CalendarEvent(
                "PrEP Dose", LocalDate.of(2026, 2, 10),
                LocalTime.of(8, 0), EventType.MEDICATION, null, null);
        event.setRecurrenceRule(RecurrenceRule.daily());

        assertFalse(event.occursOn(LocalDate.of(2026, 2, 9)));
    }

    @Test
    void testOccursOn_WeeklyRecurrence_MatchesCorrectDays() {
        CalendarEvent event = new CalendarEvent(
                "Counselling", LocalDate.of(2026, 2, 3),
                LocalTime.of(16, 0), EventType.APPOINTMENT, null, null);
        event.setRecurrenceRule(RecurrenceRule.weekly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));

        // Feb 3 is a Tuesday (start date)
        assertTrue(event.occursOn(LocalDate.of(2026, 2, 3)));
        // Feb 5 is a Thursday
        assertTrue(event.occursOn(LocalDate.of(2026, 2, 5)));
        // Feb 4 is a Wednesday — should not match
        assertFalse(event.occursOn(LocalDate.of(2026, 2, 4)));
    }

    @Test
    void testOccursOn_RecurrenceWithUntil_StopsAfterEndDate() {
        CalendarEvent event = new CalendarEvent(
                "Counselling", LocalDate.of(2026, 2, 1),
                LocalTime.of(16, 0), EventType.APPOINTMENT, null, null);
        event.setRecurrenceRule(RecurrenceRule.daily().until(LocalDate.of(2026, 2, 10)));

        // On end date — should still occur
        assertTrue(event.occursOn(LocalDate.of(2026, 2, 10)));
        // After end date — should NOT occur
        assertFalse(event.occursOn(LocalDate.of(2026, 2, 11)));
    }

    @Test
    void testOccursOn_RecurrenceWithCount_StopsAfterOccurrences() {
        CalendarEvent event = new CalendarEvent(
                "Follow-up", LocalDate.of(2026, 3, 1),
                LocalTime.of(14, 0), EventType.APPOINTMENT, null, null);
        event.setRecurrenceRule(RecurrenceRule.daily().times(3));

        assertTrue(event.occursOn(LocalDate.of(2026, 3, 1)));   // occurrence 1
        assertTrue(event.occursOn(LocalDate.of(2026, 3, 2)));   // occurrence 2
        assertTrue(event.occursOn(LocalDate.of(2026, 3, 3)));   // occurrence 3
        assertFalse(event.occursOn(LocalDate.of(2026, 3, 4)));  // beyond count
    }
}
