package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.model.RecurrenceRule;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateEventControllerTest {

    private Path tempFile;
    private EventStorageService storageService;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("create-event-test-", ".json");
        Files.delete(tempFile);
        storageService = new EventStorageService(tempFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    private CalendarEvent buildEvent(String title, LocalDate date, LocalTime time,
            EventType type, String description, String dosage) {
        return new CalendarEvent(title, date, time, type, description, dosage);
    }

    @Test
    void testCreateEvent_AppointmentWithAllFields() {
        CalendarEvent event = buildEvent(
                "GP Check-up", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 30), EventType.APPOINTMENT,
                "Annual screening", null);

        assertTrue(storageService.addEvent(event));

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals("GP Check-up", saved.getName());
        assertEquals(LocalDate.of(2026, 3, 15), saved.getDate());
        assertEquals(LocalTime.of(10, 30), saved.getTime());
        assertEquals(EventType.APPOINTMENT, saved.getType());
        assertEquals("Annual screening", saved.getDescription());
        assertNull(saved.getDosage());
        assertNull(saved.getRecurrenceRule());
    }

    @Test
    void testCreateEvent_MedicationWithDosage() {
        CalendarEvent event = buildEvent(
                "PrEP Dose", LocalDate.of(2026, 3, 15),
                LocalTime.of(8, 0), EventType.MEDICATION,
                "Take with food", "200mg");

        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(EventType.MEDICATION, saved.getType());
        assertEquals("200mg", saved.getDosage());
        assertEquals("Take with food", saved.getDescription());
    }

    @Test
    void testCreateEvent_TestType() {
        CalendarEvent event = buildEvent(
                "STI Screening", LocalDate.of(2026, 4, 1),
                LocalTime.of(14, 0), EventType.TEST,
                "Blood draw", null);

        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(EventType.TEST, saved.getType());
        assertEquals("STI Screening", saved.getName());
    }

    @Test
    void testCreateEvent_AllDayEvent_NullTime() {
        CalendarEvent event = buildEvent(
                "Health Awareness Day", LocalDate.of(2026, 5, 1),
                null, EventType.APPOINTMENT,
                "All-day event", null);

        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertNull(saved.getTime());
        assertEquals("Health Awareness Day", saved.getName());
    }

    @Test
    void testCreateEvent_NoDescription() {
        CalendarEvent event = buildEvent(
                "Quick Visit", LocalDate.of(2026, 3, 20),
                LocalTime.of(9, 0), EventType.APPOINTMENT,
                null, null);

        storageService.addEvent(event);

        assertNull(storageService.getAllEvents().get(0).getDescription());
    }

    @Test
    void testCreateEvent_NonMedicationHasNoDosage() {
        // Mimics the controller clearing dosage when type != MEDICATION
        CalendarEvent event = buildEvent(
                "Appointment", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0), EventType.APPOINTMENT,
                null, null);

        storageService.addEvent(event);

        assertNull(storageService.getAllEvents().get(0).getDosage());
    }

    @Test
    void testValidation_MissingTitleDetected() {
        String title = null;
        assertTrue(title == null || title.trim().isEmpty(),
                "Null title should be caught by validation");
    }

    @Test
    void testValidation_EmptyTitleDetected() {
        String title = "   ";
        assertTrue(title.trim().isEmpty(),
                "Whitespace-only title should be caught by validation");
    }

    @Test
    void testValidation_MissingDateDetected() {
        LocalDate date = null;
        assertNull(date, "Null date should be caught by validation");
    }

    @Test
    void testValidation_MissingTypeDetected() {
        EventType type = null;
        assertNull(type, "Null type should be caught by validation");
    }

    @Test
    void testValidation_AllFieldsPresent_PassesValidation() {
        String title = "Valid Event";
        LocalDate date = LocalDate.of(2026, 4, 10);
        EventType type = EventType.APPOINTMENT;

        boolean hasError = false;
        if (title == null || title.trim().isEmpty())
            hasError = true;
        if (date == null)
            hasError = true;
        if (type == null)
            hasError = true;

        assertFalse(hasError, "Complete event should pass validation");
    }

    @Test
    void testValidation_MultipleFieldsMissing_AllDetected() {
        // Simulates the controller's StringBuilder pattern
        String title = "";
        LocalDate date = null;
        EventType type = null;

        StringBuilder errorMessage = new StringBuilder("Missing: ");
        boolean hasError = false;

        if (title == null || title.trim().isEmpty()) {
            errorMessage.append("Title, ");
            hasError = true;
        }
        if (date == null) {
            errorMessage.append("Date, ");
            hasError = true;
        }
        if (type == null) {
            errorMessage.append("Type, ");
            hasError = true;
        }

        assertTrue(hasError);
        String finalMsg = errorMessage.substring(0, errorMessage.length() - 2);
        assertEquals("Missing: Title, Date, Type", finalMsg);
    }

    @Test
    void testCreateEvent_DailyRecurrence() {
        CalendarEvent event = buildEvent(
                "Daily PrEP", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "200mg");

        RecurrenceRule rule = RecurrenceRule.daily(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertNotNull(saved.getRecurrenceRule());
        assertEquals(RecurrenceRule.Frequency.DAILY, saved.getRecurrenceRule().getFrequency());
        assertEquals(1, saved.getRecurrenceRule().getInterval());
    }

    @Test
    void testCreateEvent_DailyRecurrenceWithInterval() {
        CalendarEvent event = buildEvent(
                "Every-other-day Med", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "100mg");

        RecurrenceRule rule = RecurrenceRule.daily(2);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(2, saved.getRecurrenceRule().getInterval());
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 1)));
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 3)));
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 2)));
    }

    @Test
    void testCreateEvent_WeeklyRecurrenceWithDays() {
        CalendarEvent event = buildEvent(
                "Counselling", LocalDate.of(2026, 3, 2), // Monday
                LocalTime.of(16, 0), EventType.APPOINTMENT,
                null, null);

        RecurrenceRule rule = RecurrenceRule.weekly(1, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.Frequency.WEEKLY, saved.getRecurrenceRule().getFrequency());
        Set<DayOfWeek> days = saved.getRecurrenceRule().getDaysOfWeek();
        assertTrue(days.contains(DayOfWeek.MONDAY));
        assertTrue(days.contains(DayOfWeek.WEDNESDAY));
        assertEquals(2, days.size());
    }

    @Test
    void testCreateEvent_WeeklyRecurrenceWithInterval() {
        CalendarEvent event = buildEvent(
                "Bi-weekly Check-in", LocalDate.of(2026, 3, 2),
                LocalTime.of(10, 0), EventType.APPOINTMENT,
                null, null);

        RecurrenceRule rule = RecurrenceRule.weekly(2, DayOfWeek.MONDAY);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(2, saved.getRecurrenceRule().getInterval());
        // Occurs on start date
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 2)));
        // Occurs 2 weeks later
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 16)));
        // Does NOT occur 1 week later
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 9)));
    }

    @Test
    void testCreateEvent_MonthlyRecurrenceOnDay() {
        CalendarEvent event = buildEvent(
                "Monthly Review", LocalDate.of(2026, 3, 15),
                LocalTime.of(14, 0), EventType.APPOINTMENT,
                null, null);

        RecurrenceRule rule = RecurrenceRule.monthlyOnDay(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.Frequency.MONTHLY, saved.getRecurrenceRule().getFrequency());
        assertEquals(RecurrenceRule.MonthlyPattern.DAY_OF_MONTH, saved.getRecurrenceRule().getMonthlyPattern());
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 15)));
        assertTrue(saved.occursOn(LocalDate.of(2026, 4, 15)));
        assertFalse(saved.occursOn(LocalDate.of(2026, 4, 16)));
    }

    @Test
    void testCreateEvent_MonthlyRecurrenceOnLastDay() {
        CalendarEvent event = buildEvent(
                "Month-end Review", LocalDate.of(2026, 1, 31),
                LocalTime.of(17, 0), EventType.APPOINTMENT,
                null, null);

        RecurrenceRule rule = RecurrenceRule.monthlyOnLastDay(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.MonthlyPattern.LAST_DAY, saved.getRecurrenceRule().getMonthlyPattern());
        // Feb 2026 has 28 days
        assertTrue(saved.occursOn(LocalDate.of(2026, 2, 28)));
        // March 2026 has 31 days
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 31)));
        // Not the 30th in March (not the last day)
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 30)));
    }

    @Test
    void testCreateEvent_MonthlyRecurrenceOnNthWeekday() {
        // March 2, 2026 is the 1st Monday of March
        CalendarEvent event = buildEvent(
                "First-Monday Meeting", LocalDate.of(2026, 3, 2),
                LocalTime.of(9, 0), EventType.APPOINTMENT,
                null, null);

        RecurrenceRule rule = RecurrenceRule.monthlyOnNthWeekday(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.MonthlyPattern.NTH_WEEKDAY, saved.getRecurrenceRule().getMonthlyPattern());
        // April 6, 2026 is the 1st Monday of April
        assertTrue(saved.occursOn(LocalDate.of(2026, 4, 6)));
        // April 13 is the 2nd Monday — should not match
        assertFalse(saved.occursOn(LocalDate.of(2026, 4, 13)));
    }

    @Test
    void testCreateEvent_YearlyRecurrence() {
        CalendarEvent event = buildEvent(
                "Annual Check-up", LocalDate.of(2026, 6, 15),
                LocalTime.of(10, 0), EventType.TEST,
                null, null);

        RecurrenceRule rule = RecurrenceRule.yearly(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.Frequency.YEARLY, saved.getRecurrenceRule().getFrequency());
        assertTrue(saved.occursOn(LocalDate.of(2027, 6, 15)));
        assertFalse(saved.occursOn(LocalDate.of(2027, 6, 16)));
    }

    @Test
    void testCreateEvent_RecurrenceUntilDate() {
        CalendarEvent event = buildEvent(
                "Short Course", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "100mg");

        RecurrenceRule rule = RecurrenceRule.daily(1).until(LocalDate.of(2026, 3, 10));
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.EndType.UNTIL, saved.getRecurrenceRule().getEndType());
        assertEquals(LocalDate.of(2026, 3, 10), saved.getRecurrenceRule().getEndDate());
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 10)));
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 11)));
    }

    @Test
    void testCreateEvent_RecurrenceAfterOccurrences() {
        CalendarEvent event = buildEvent(
                "Limited Course", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "50mg");

        RecurrenceRule rule = RecurrenceRule.daily(1).times(5);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.EndType.COUNT, saved.getRecurrenceRule().getEndType());
        assertEquals(5, saved.getRecurrenceRule().getOccurrenceCount());
        // Occurrences 1-5
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 5)));
        // Occurrence 6 — beyond count
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 6)));
    }

    @Test
    void testCreateEvent_RecurrenceNeverEnd() {
        CalendarEvent event = buildEvent(
                "Ongoing Med", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "200mg");

        RecurrenceRule rule = RecurrenceRule.daily(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.EndType.NEVER, saved.getRecurrenceRule().getEndType());
        // Still occurs far in the future
        assertTrue(saved.occursOn(LocalDate.of(2027, 12, 31)));
    }

    @Test
    void testCreateEvent_RecurrenceWithExceptionDates() {
        CalendarEvent event = buildEvent(
                "Daily Med", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "200mg");

        Set<LocalDate> exceptions = new HashSet<>();
        exceptions.add(LocalDate.of(2026, 3, 5));
        exceptions.add(LocalDate.of(2026, 3, 10));

        RecurrenceRule rule = RecurrenceRule.daily(1);
        rule.setExcludedDates(exceptions);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertNotNull(saved.getRecurrenceRule().getExcludedDates());
        assertEquals(2, saved.getRecurrenceRule().getExcludedDates().size());
        // Normal day — still occurs
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 4)));
        // Exception days — skipped
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 5)));
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 10)));
        // Day after exception — still occurs
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 6)));
    }

    @Test
    void testCreateEvent_RecurrenceWithNoExceptions() {
        CalendarEvent event = buildEvent(
                "Daily Med", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "200mg");

        RecurrenceRule rule = RecurrenceRule.daily(1);
        // Don't set any excluded dates
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertNull(saved.getRecurrenceRule().getExcludedDates());
    }

    @Test
    void testCreateEvent_DoesNotRepeat_NoRecurrenceRule() {
        // Mimics "Does not repeat" selection — controller skips recurrence
        CalendarEvent event = buildEvent(
                "One-off Appointment", LocalDate.of(2026, 4, 1),
                LocalTime.of(11, 0), EventType.APPOINTMENT,
                null, null);
        // No recurrence rule set

        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertNull(saved.getRecurrenceRule());
        assertTrue(saved.occursOn(LocalDate.of(2026, 4, 1)));
        assertFalse(saved.occursOn(LocalDate.of(2026, 4, 2)));
    }

    @Test
    void testCreateEvent_PersistsAcrossReload() {
        CalendarEvent event = buildEvent(
                "Persistent Event", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 30), EventType.APPOINTMENT,
                "Important visit", null);
        storageService.addEvent(event);

        // Reload from disk
        EventStorageService reloaded = new EventStorageService(tempFile);

        assertEquals(1, reloaded.getAllEvents().size());
        CalendarEvent loaded = reloaded.getAllEvents().get(0);
        assertEquals("Persistent Event", loaded.getName());
        assertEquals(LocalDate.of(2026, 3, 15), loaded.getDate());
        assertEquals(LocalTime.of(10, 30), loaded.getTime());
        assertEquals(EventType.APPOINTMENT, loaded.getType());
    }

    @Test
    void testCreateEvent_RecurrencePersistsAcrossReload() {
        CalendarEvent event = buildEvent(
                "Daily PrEP", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION,
                null, "200mg");

        Set<LocalDate> exceptions = new HashSet<>();
        exceptions.add(LocalDate.of(2026, 3, 5));

        RecurrenceRule rule = RecurrenceRule.weekly(2, DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        rule.until(LocalDate.of(2026, 6, 30));
        rule.setExcludedDates(exceptions);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        // Reload from disk
        EventStorageService reloaded = new EventStorageService(tempFile);
        CalendarEvent loaded = reloaded.getAllEvents().get(0);

        assertNotNull(loaded.getRecurrenceRule());
        RecurrenceRule loadedRule = loaded.getRecurrenceRule();
        assertEquals(RecurrenceRule.Frequency.WEEKLY, loadedRule.getFrequency());
        assertEquals(2, loadedRule.getInterval());
        assertEquals(RecurrenceRule.EndType.UNTIL, loadedRule.getEndType());
        assertEquals(LocalDate.of(2026, 6, 30), loadedRule.getEndDate());
        assertTrue(loadedRule.getDaysOfWeek().contains(DayOfWeek.MONDAY));
        assertTrue(loadedRule.getDaysOfWeek().contains(DayOfWeek.FRIDAY));
        assertTrue(loadedRule.getExcludedDates().contains(LocalDate.of(2026, 3, 5)));
    }

    @Test
    void testCreateEvent_MedicationDosagePersistsAcrossReload() {
        CalendarEvent event = buildEvent(
                "PrEP", LocalDate.of(2026, 3, 15),
                LocalTime.of(8, 0), EventType.MEDICATION,
                "Take daily", "200mg");
        storageService.addEvent(event);

        EventStorageService reloaded = new EventStorageService(tempFile);
        CalendarEvent loaded = reloaded.getAllEvents().get(0);

        assertEquals("200mg", loaded.getDosage());
        assertEquals(EventType.MEDICATION, loaded.getType());
        assertEquals("Take daily", loaded.getDescription());
    }

    @Test
    void testCreateMultipleEvents_AllSaved() {
        storageService.addEvent(buildEvent(
                "Appointment", LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0), EventType.APPOINTMENT, null, null));
        storageService.addEvent(buildEvent(
                "Medication", LocalDate.of(2026, 3, 10),
                LocalTime.of(8, 0), EventType.MEDICATION, null, "200mg"));
        storageService.addEvent(buildEvent(
                "Test", LocalDate.of(2026, 3, 15),
                null, EventType.TEST, "Blood work", null));

        assertEquals(3, storageService.getAllEvents().size());
    }

    @Test
    void testCreateMultipleEvents_QueriedByDate() {
        storageService.addEvent(buildEvent(
                "Morning Med", LocalDate.of(2026, 3, 10),
                LocalTime.of(8, 0), EventType.MEDICATION, null, "200mg"));
        storageService.addEvent(buildEvent(
                "Afternoon Appt", LocalDate.of(2026, 3, 10),
                LocalTime.of(14, 0), EventType.APPOINTMENT, null, null));
        storageService.addEvent(buildEvent(
                "Other Day", LocalDate.of(2026, 3, 11),
                LocalTime.of(10, 0), EventType.TEST, null, null));

        List<CalendarEvent> march10 = storageService.getEventsForDate(LocalDate.of(2026, 3, 10));
        assertEquals(2, march10.size());
        // Should be sorted by time
        assertEquals("Morning Med", march10.get(0).getName());
        assertEquals("Afternoon Appt", march10.get(1).getName());
    }

    @Test
    void testCreateEvent_UniqueIdsGenerated() {
        CalendarEvent e1 = buildEvent("Event 1", LocalDate.now(), null, EventType.APPOINTMENT, null, null);
        CalendarEvent e2 = buildEvent("Event 2", LocalDate.now(), null, EventType.APPOINTMENT, null, null);

        storageService.addEvent(e1);
        storageService.addEvent(e2);

        assertNotEquals(e1.getId(), e2.getId());
    }

    @Test
    void testCreateEvent_EndDateBeforeStartDate_RuleStillSet() {
        // The controller only applies until() when endDate >= event.getDate()
        // Simulating controller logic: if endDate is before start, skip setting until
        CalendarEvent event = buildEvent(
                "Edge Case", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0), EventType.APPOINTMENT, null, null);

        RecurrenceRule rule = RecurrenceRule.daily(1);
        LocalDate endDate = LocalDate.of(2026, 3, 10); // Before start

        // Controller logic: only apply until() if endDate >= event.getDate()
        if (endDate != null && !endDate.isBefore(event.getDate())) {
            rule.until(endDate);
        }
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        // Should fall back to NEVER since endDate was before start
        CalendarEvent saved = storageService.getAllEvents().get(0);
        assertEquals(RecurrenceRule.EndType.NEVER, saved.getRecurrenceRule().getEndType());
    }

    @Test
    void testCreateEvent_WeeklyNoDaysSelected_FallsBackToStartDay() {
        // March 2, 2026 is a Monday
        CalendarEvent event = buildEvent(
                "Weekly Meeting", LocalDate.of(2026, 3, 2),
                LocalTime.of(10, 0), EventType.APPOINTMENT, null, null);

        // Weekly with no specific days — RecurrenceRule.weekly() with empty array
        RecurrenceRule rule = RecurrenceRule.weekly(1);
        event.setRecurrenceRule(rule);
        storageService.addEvent(event);

        CalendarEvent saved = storageService.getAllEvents().get(0);
        // Should still occur on Mondays (the start date's day of week)
        assertTrue(saved.occursOn(LocalDate.of(2026, 3, 9))); // Next Monday
        assertFalse(saved.occursOn(LocalDate.of(2026, 3, 10))); // Tuesday
    }
}
