package com.sddp.sexualhealthapp.calendar.util;

import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.model.RecurrenceRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDetailFormatterTest {

    @Test
    void formatEventName_NullOrBlank_UsesFallback() {
        assertEquals("Untitled event", EventDetailFormatter.formatEventName(null));
        assertEquals("Untitled event", EventDetailFormatter.formatEventName("   "));
    }

    @Test
    void formatEventName_TrimmedValue_ReturnsTrimmedName() {
        assertEquals("PrEP dose", EventDetailFormatter.formatEventName("  PrEP dose  "));
    }

    @Test
    void formatDateTime_WithOccurrenceDate_PrefersOccurrenceDate() {
        String formatted = EventDetailFormatter.formatDateTime(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3),
                LocalTime.of(8, 0),
                Locale.UK);

        assertEquals("3 February 2026 · 08:00", formatted);
    }

    @Test
    void formatDateTime_WithNoTime_UsesAllDay() {
        String formatted = EventDetailFormatter.formatDateTime(
                LocalDate.of(2026, 2, 1),
                null,
                null,
                Locale.UK);

        assertEquals("1 February 2026 · All day", formatted);
    }

    @Test
    void formatDescription_NullOrBlank_UsesPlaceholder() {
        assertEquals("No description provided", EventDetailFormatter.formatDescription(null));
        assertEquals("No description provided", EventDetailFormatter.formatDescription(" "));
    }

    @Test
    void formatDescription_TrimmedValue_ReturnsTrimmedDescription() {
        assertEquals("Take with food", EventDetailFormatter.formatDescription("  Take with food  "));
    }

    @Test
    void shouldShowDosage_OnlyForMedicationWithValue() {
        assertTrue(EventDetailFormatter.shouldShowDosage(EventType.MEDICATION, "200mg"));
        assertFalse(EventDetailFormatter.shouldShowDosage(EventType.MEDICATION, "   "));
        assertFalse(EventDetailFormatter.shouldShowDosage(EventType.APPOINTMENT, "200mg"));
        assertFalse(EventDetailFormatter.shouldShowDosage(EventType.TEST, null));
    }

    @Test
    void formatDosage_TrimsValue() {
        assertEquals("200mg", EventDetailFormatter.formatDosage("  200mg  "));
    }

    @Test
    void formatRecurrence_Daily() {
        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                RecurrenceRule.daily(),
                LocalDate.of(2026, 2, 1),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Daily", recurrence.get());
    }

    @Test
    void formatRecurrence_DailyWithCount() {
        RecurrenceRule rule = RecurrenceRule.daily(2).times(6);

        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                rule,
                LocalDate.of(2026, 2, 1),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Every 2 days for 6 occurrences", recurrence.get());
    }

    @Test
    void formatRecurrence_Weekly() {
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                rule,
                LocalDate.of(2026, 2, 1),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Every Tuesday and Thursday", recurrence.get());
    }

    @Test
    void formatRecurrence_WeeklyWithUntil() {
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
                .until(LocalDate.of(2026, 4, 30));

        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                rule,
                LocalDate.of(2026, 2, 1),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Every Tuesday and Thursday until 30 April 2026", recurrence.get());
    }

    @Test
    void formatRecurrence_MonthlyLastDay() {
        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                RecurrenceRule.monthlyOnLastDay(),
                LocalDate.of(2026, 1, 31),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Monthly (last day)", recurrence.get());
    }

    @Test
    void formatRecurrence_MonthlyNthWeekday() {
        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                RecurrenceRule.monthlyOnNthWeekday(),
                LocalDate.of(2026, 2, 18),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Monthly (3rd Wednesday)", recurrence.get());
    }

    @Test
    void formatRecurrence_Yearly() {
        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                RecurrenceRule.yearly(),
                LocalDate.of(2026, 2, 1),
                Locale.UK);

        assertTrue(recurrence.isPresent());
        assertEquals("Yearly", recurrence.get());
    }

    @Test
    void formatRecurrence_NoRule_ReturnsEmpty() {
        Optional<String> recurrence = EventDetailFormatter.formatRecurrence(
                null,
                LocalDate.of(2026, 2, 1),
                Locale.UK);

        assertTrue(recurrence.isEmpty());
    }
}
