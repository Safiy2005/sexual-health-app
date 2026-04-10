package com.sddp.sexualhealthapp.calendar.service;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.model.RecurrenceRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EventStorageService class.
 * Uses temporary files for isolated testing of JSON persistence.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
class EventStorageServiceTest {

    private Path tempFile;
    private EventStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        com.sddp.sexualhealthapp.util.NotificationService.clearAllTasks(); // clears global notifs so they dont carry across tests
        tempFile = Files.createTempFile("events-test-", ".json");
        Files.delete(tempFile); // Start with no file (test empty state)
        service = new EventStorageService(tempFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    private CalendarEvent createTestEvent(String name, LocalDate date, LocalTime time, EventType type) {
        return new CalendarEvent(name, date, time, type, null, null);
    }

    // --- getAllEvents ---

    @Test
    void testGetAllEvents_EmptyInitially() {
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void testGetAllEvents_ReturnsUnmodifiableList() {
        service.addEvent(createTestEvent("Test", LocalDate.now(), null, EventType.APPOINTMENT));

        assertThrows(UnsupportedOperationException.class, () ->
                service.getAllEvents().add(new CalendarEvent()));
    }

    @Test
    void testGetAllEvents_ReturnsAllAdded() {
        service.addEvent(createTestEvent("Event 1", LocalDate.now(), null, EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Event 2", LocalDate.now(), null, EventType.TEST));

        assertEquals(2, service.getAllEvents().size());
    }

    // --- addEvent ---

    @Test
    void testAddEvent_Success() {
        CalendarEvent event = createTestEvent(
                "GP Visit", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0), EventType.APPOINTMENT);

        assertTrue(service.addEvent(event));
        assertEquals(1, service.getAllEvents().size());
        assertEquals("GP Visit", service.getAllEvents().get(0).getName());
    }

    @Test
    void testAddEvent_Null() {
        assertFalse(service.addEvent(null));
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void testAddEvent_WithAllFields() {
        CalendarEvent event = new CalendarEvent(
                "PrEP Dose", LocalDate.of(2026, 3, 15),
                LocalTime.of(8, 0), EventType.MEDICATION,
                "Take with food", "200mg");

        assertTrue(service.addEvent(event));

        CalendarEvent retrieved = service.getAllEvents().get(0);
        assertEquals("PrEP Dose", retrieved.getName());
        assertEquals("Take with food", retrieved.getDescription());
        assertEquals("200mg", retrieved.getDosage());
    }

    // --- getEventsForDate ---

    @Test
    void testGetEventsForDate_FiltersCorrectly() {
        LocalDate targetDate = LocalDate.of(2026, 3, 15);
        LocalDate otherDate = LocalDate.of(2026, 3, 16);

        service.addEvent(createTestEvent("On target", targetDate, LocalTime.of(10, 0), EventType.APPOINTMENT));
        service.addEvent(createTestEvent("On other", otherDate, LocalTime.of(11, 0), EventType.TEST));
        service.addEvent(createTestEvent("Also target", targetDate, LocalTime.of(14, 0), EventType.MEDICATION));

        List<CalendarEvent> result = service.getEventsForDate(targetDate);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getDate().equals(targetDate)));
    }

    @Test
    void testGetEventsForDate_SortedByTime() {
        LocalDate date = LocalDate.of(2026, 3, 15);

        service.addEvent(createTestEvent("Afternoon", date, LocalTime.of(14, 0), EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Morning", date, LocalTime.of(9, 0), EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Evening", date, LocalTime.of(18, 0), EventType.APPOINTMENT));

        List<CalendarEvent> result = service.getEventsForDate(date);

        assertEquals("Morning", result.get(0).getName());
        assertEquals("Afternoon", result.get(1).getName());
        assertEquals("Evening", result.get(2).getName());
    }

    @Test
    void testGetEventsForDate_NullTimesLast() {
        LocalDate date = LocalDate.of(2026, 3, 15);

        service.addEvent(createTestEvent("All-day", date, null, EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Timed", date, LocalTime.of(9, 0), EventType.APPOINTMENT));

        List<CalendarEvent> result = service.getEventsForDate(date);

        assertEquals("Timed", result.get(0).getName());
        assertEquals("All-day", result.get(1).getName());
    }

    @Test
    void testGetEventsForDate_NoResults() {
        service.addEvent(createTestEvent("Event", LocalDate.of(2026, 3, 15), null, EventType.TEST));

        List<CalendarEvent> result = service.getEventsForDate(LocalDate.of(2026, 3, 16));

        assertTrue(result.isEmpty());
    }

    // --- getEventsForMonth ---

    @Test
    void testGetEventsForMonth_FiltersCorrectly() {
        YearMonth march2026 = YearMonth.of(2026, 3);

        service.addEvent(createTestEvent("March event", LocalDate.of(2026, 3, 10), null, EventType.APPOINTMENT));
        service.addEvent(createTestEvent("April event", LocalDate.of(2026, 4, 10), null, EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Also March", LocalDate.of(2026, 3, 25), null, EventType.TEST));

        List<CalendarEvent> result = service.getEventsForMonth(march2026);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> YearMonth.from(e.getDate()).equals(march2026)));
    }

    @Test
    void testGetEventsForMonth_EmptyMonth() {
        service.addEvent(createTestEvent("March", LocalDate.of(2026, 3, 10), null, EventType.TEST));

        List<CalendarEvent> result = service.getEventsForMonth(YearMonth.of(2026, 4));

        assertTrue(result.isEmpty());
    }

    // --- getDaysWithEvents ---

    @Test
    void testGetDaysWithEvents_ReturnsCorrectDays() {
        YearMonth march2026 = YearMonth.of(2026, 3);

        service.addEvent(createTestEvent("Event 1", LocalDate.of(2026, 3, 5), null, EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Event 2", LocalDate.of(2026, 3, 5), null, EventType.TEST));
        service.addEvent(createTestEvent("Event 3", LocalDate.of(2026, 3, 20), null, EventType.MEDICATION));

        Set<Integer> days = service.getDaysWithEvents(march2026);

        assertEquals(2, days.size());
        assertTrue(days.contains(5));
        assertTrue(days.contains(20));
    }

    @Test
    void testGetDaysWithEvents_EmptyMonth() {
        Set<Integer> days = service.getDaysWithEvents(YearMonth.of(2026, 6));

        assertTrue(days.isEmpty());
    }

    @Test
    void testGetDaysWithEvents_DoesNotIncludeOtherMonths() {
        service.addEvent(createTestEvent("March", LocalDate.of(2026, 3, 15), null, EventType.TEST));
        service.addEvent(createTestEvent("April", LocalDate.of(2026, 4, 15), null, EventType.TEST));

        Set<Integer> marchDays = service.getDaysWithEvents(YearMonth.of(2026, 3));

        assertEquals(1, marchDays.size());
        assertTrue(marchDays.contains(15));
    }

    // --- getEventById ---

    @Test
    void testGetEventById_Found() {
        CalendarEvent event = createTestEvent("Findable", LocalDate.now(), null, EventType.TEST);
        service.addEvent(event);

        Optional<CalendarEvent> result = service.getEventById(event.getId());

        assertTrue(result.isPresent());
        assertEquals("Findable", result.get().getName());
    }

    @Test
    void testGetEventById_NotFound() {
        Optional<CalendarEvent> result = service.getEventById("nonexistent-id");

        assertTrue(result.isEmpty());
    }

    // --- updateEvent ---

    @Test
    void testUpdateEvent_Success() {
        CalendarEvent event = createTestEvent("Original", LocalDate.now(), null, EventType.TEST);
        service.addEvent(event);

        event.setName("Updated");
        assertTrue(service.updateEvent(event));

        assertEquals("Updated", service.getAllEvents().get(0).getName());
    }

    @Test
    void testUpdateEvent_NotFound() {
        CalendarEvent event = createTestEvent("Ghost", LocalDate.now(), null, EventType.TEST);

        assertFalse(service.updateEvent(event));
    }

    @Test
    void testUpdateEvent_Null() {
        assertFalse(service.updateEvent(null));
    }

    // --- deleteEvent ---

    @Test
    void testDeleteEvent_Success() {
        CalendarEvent event = createTestEvent("Deletable", LocalDate.now(), null, EventType.TEST);
        service.addEvent(event);

        assertTrue(service.deleteEvent(event.getId()));
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void testDeleteEvent_NotFound() {
        assertFalse(service.deleteEvent("nonexistent-id"));
    }

    @Test
    void testDeleteEvent_OnlyDeletesTarget() {
        CalendarEvent event1 = createTestEvent("Keep", LocalDate.now(), null, EventType.APPOINTMENT);
        CalendarEvent event2 = createTestEvent("Delete", LocalDate.now(), null, EventType.TEST);
        service.addEvent(event1);
        service.addEvent(event2);

        service.deleteEvent(event2.getId());

        assertEquals(1, service.getAllEvents().size());
        assertEquals("Keep", service.getAllEvents().get(0).getName());
    }

    // --- Persistence ---

    @Test
    void testPersistence_SurvivesReload() {
        CalendarEvent event = new CalendarEvent(
                "Persistent", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 30), EventType.APPOINTMENT,
                "Important visit", null);
        service.addEvent(event);

        EventStorageService reloaded = new EventStorageService(tempFile);

        assertEquals(1, reloaded.getAllEvents().size());
        CalendarEvent loaded = reloaded.getAllEvents().get(0);
        assertEquals("Persistent", loaded.getName());
        assertEquals(LocalDate.of(2026, 3, 15), loaded.getDate());
        assertEquals(LocalTime.of(10, 30), loaded.getTime());
        assertEquals(EventType.APPOINTMENT, loaded.getType());
        assertEquals("Important visit", loaded.getDescription());
    }

    @Test
    void testPersistence_MedicationWithDosage() {
        CalendarEvent event = new CalendarEvent(
                "PrEP", LocalDate.of(2026, 3, 15),
                LocalTime.of(8, 0), EventType.MEDICATION,
                "Daily dose", "200mg");
        service.addEvent(event);

        EventStorageService reloaded = new EventStorageService(tempFile);
        CalendarEvent loaded = reloaded.getAllEvents().get(0);

        assertEquals("200mg", loaded.getDosage());
        assertEquals(EventType.MEDICATION, loaded.getType());
    }

    @Test
    void testPersistence_MissingFile() {
        // tempFile was already deleted in setUp, so service starts empty
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void testPersistence_CorruptedFile() throws IOException {
        Files.writeString(tempFile, "this is not valid json!");

        EventStorageService corruptService = new EventStorageService(tempFile);

        assertTrue(corruptService.getAllEvents().isEmpty());
    }

    @Test
    void testPersistence_EmptyJsonArray() throws IOException {
        Files.writeString(tempFile, "[]");

        EventStorageService emptyService = new EventStorageService(tempFile);

        assertTrue(emptyService.getAllEvents().isEmpty());
    }

    @Test
    void testPersistence_NullJsonContent() throws IOException {
        Files.writeString(tempFile, "null");

        EventStorageService nullService = new EventStorageService(tempFile);

        assertTrue(nullService.getAllEvents().isEmpty());
    }

    // --- Full CRUD flow ---

    @Test
    void testFullCrudFlow() {
        // Create
        CalendarEvent event = new CalendarEvent(
                "Initial", LocalDate.of(2026, 3, 15),
                LocalTime.of(10, 0), EventType.APPOINTMENT,
                "Notes here", null);
        assertTrue(service.addEvent(event));
        assertEquals(1, service.getAllEvents().size());

        // Read
        Optional<CalendarEvent> found = service.getEventById(event.getId());
        assertTrue(found.isPresent());
        assertEquals("Initial", found.get().getName());

        // Update
        event.setName("Updated");
        event.setDescription("New notes");
        assertTrue(service.updateEvent(event));
        assertEquals("Updated", service.getEventById(event.getId()).get().getName());

        // Delete
        assertTrue(service.deleteEvent(event.getId()));
        assertTrue(service.getAllEvents().isEmpty());
        assertTrue(service.getEventById(event.getId()).isEmpty());
    }

    @Test
    void testMultipleEventsAcrossMonths() {
        service.addEvent(createTestEvent("Feb", LocalDate.of(2026, 2, 10), null, EventType.TEST));
        service.addEvent(createTestEvent("Mar 1", LocalDate.of(2026, 3, 5), null, EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Mar 2", LocalDate.of(2026, 3, 20), null, EventType.MEDICATION));
        service.addEvent(createTestEvent("Apr", LocalDate.of(2026, 4, 1), null, EventType.TEST));

        assertEquals(4, service.getAllEvents().size());
        assertEquals(2, service.getEventsForMonth(YearMonth.of(2026, 3)).size());
        assertEquals(2, service.getDaysWithEvents(YearMonth.of(2026, 3)).size());
        assertEquals(1, service.getEventsForDate(LocalDate.of(2026, 3, 5)).size());
    }

    // --- getEventTypesPerDay (AC: coloured dots per event type) ---

    @Test
    void testGetEventTypesPerDay_ReturnsUniqueTypesPerDay() {
        YearMonth march = YearMonth.of(2026, 3);

        service.addEvent(createTestEvent("Appointment", LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0), EventType.APPOINTMENT));
        service.addEvent(createTestEvent("Test", LocalDate.of(2026, 3, 10),
                null, EventType.TEST));

        Map<Integer, Set<EventType>> result = service.getEventTypesPerDay(march);

        assertTrue(result.containsKey(10));
        Set<EventType> types = result.get(10);
        assertEquals(2, types.size());
        assertTrue(types.contains(EventType.APPOINTMENT));
        assertTrue(types.contains(EventType.TEST));
    }

    @Test
    void testGetEventTypesPerDay_SameTypeSameDayProducesSingleEntry() {
        YearMonth march = YearMonth.of(2026, 3);

        // Two MEDICATION events on the same day — should produce only one dot
        service.addEvent(createTestEvent("PrEP Morning", LocalDate.of(2026, 3, 5),
                LocalTime.of(8, 0), EventType.MEDICATION));
        service.addEvent(createTestEvent("PrEP Evening", LocalDate.of(2026, 3, 5),
                LocalTime.of(20, 0), EventType.MEDICATION));

        Map<Integer, Set<EventType>> result = service.getEventTypesPerDay(march);

        Set<EventType> types = result.get(5);
        assertEquals(1, types.size());
        assertTrue(types.contains(EventType.MEDICATION));
    }

    @Test
    void testGetEventTypesPerDay_DayWithNoEventsNotInMap() {
        YearMonth march = YearMonth.of(2026, 3);

        service.addEvent(createTestEvent("Event", LocalDate.of(2026, 3, 10),
                null, EventType.TEST));

        Map<Integer, Set<EventType>> result = service.getEventTypesPerDay(march);

        // Day 10 has an event; day 11 does not
        assertTrue(result.containsKey(10));
        assertFalse(result.containsKey(11));
    }

    @Test
    void testGetEventTypesPerDay_EmptyMonth() {
        Map<Integer, Set<EventType>> result =
                service.getEventTypesPerDay(YearMonth.of(2026, 6));

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEventTypesPerDay_OnlyContainsValidDayNumbers() {
        YearMonth feb = YearMonth.of(2026, 2); // 28 days

        service.addEvent(createTestEvent("Event", LocalDate.of(2026, 2, 15),
                null, EventType.TEST));

        Map<Integer, Set<EventType>> result = service.getEventTypesPerDay(feb);

        // All keys must be valid day numbers for February
        for (int day : result.keySet()) {
            assertTrue(day >= 1 && day <= 28,
                    "Day " + day + " is outside valid range for February 2026");
        }
    }

    @Test
    void testGetEventTypesPerDay_RecurringEventProducesDotsOnAllMatchingDates() {
        YearMonth march = YearMonth.of(2026, 3);

        CalendarEvent dailyMed = new CalendarEvent(
                "PrEP", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION, null, null);
        dailyMed.setRecurrenceRule(RecurrenceRule.daily());
        service.addEvent(dailyMed);

        Map<Integer, Set<EventType>> result = service.getEventTypesPerDay(march);

        // A daily event starting March 1 should produce a dot on every day in March
        assertEquals(31, result.size());
        for (int day = 1; day <= 31; day++) {
            assertTrue(result.containsKey(day), "Missing dot on day " + day);
            assertTrue(result.get(day).contains(EventType.MEDICATION));
        }
    }

    @Test
    void testGetEventTypesPerDay_RecurringEventWithUntil_NoDotsAfterEndDate() {
        YearMonth march = YearMonth.of(2026, 3);

        CalendarEvent limited = new CalendarEvent(
                "Short course", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION, null, null);
        limited.setRecurrenceRule(RecurrenceRule.daily().until(LocalDate.of(2026, 3, 10)));
        service.addEvent(limited);

        Map<Integer, Set<EventType>> result = service.getEventTypesPerDay(march);

        // Dots on days 1-10 (inclusive)
        for (int day = 1; day <= 10; day++) {
            assertTrue(result.containsKey(day), "Missing dot on day " + day);
        }
        // No dots on days 11-31
        for (int day = 11; day <= 31; day++) {
            assertFalse(result.containsKey(day),
                    "Unexpected dot on day " + day + " after UNTIL end date");
        }
    }

    @Test
    void testGetEventsForDate_IncludesRecurringEvents() {
        CalendarEvent dailyMed = new CalendarEvent(
                "PrEP", LocalDate.of(2026, 2, 1),
                LocalTime.of(8, 0), EventType.MEDICATION, null, null);
        dailyMed.setRecurrenceRule(RecurrenceRule.daily());
        service.addEvent(dailyMed);

        // Query a date 2 weeks after the start
        List<CalendarEvent> result = service.getEventsForDate(LocalDate.of(2026, 2, 15));

        assertEquals(1, result.size());
        assertEquals("PrEP", result.get(0).getName());
    }

    @Test
    void testGetEventsForDate_ExcludesRecurringEventAfterUntil() {
        CalendarEvent limited = new CalendarEvent(
                "Course", LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), EventType.MEDICATION, null, null);
        limited.setRecurrenceRule(RecurrenceRule.daily().until(LocalDate.of(2026, 3, 5)));
        service.addEvent(limited);

        // On the end date — included
        assertEquals(1, service.getEventsForDate(LocalDate.of(2026, 3, 5)).size());
        // After the end date — excluded
        assertTrue(service.getEventsForDate(LocalDate.of(2026, 3, 6)).isEmpty());
    }
    @Test
    void testCreateEvent_WithReminderAndMemoryPersistsAcrossReload() {
        // 1. Create a standard medication event using the normal constructor
        CalendarEvent event = new CalendarEvent(
                "PrEP Reminder", LocalDate.of(2026, 3, 15),
                LocalTime.of(9, 0), EventType.MEDICATION,
                "Take daily", "200mg");

        // ... (the rest of the test remains exactly the same)
        // 2. Add our new notification features
        event.setReminderMinutes(15); // 15 minutes before
        event.setLastReminderSentDate(LocalDate.of(2026, 3, 15)); // Simulate a sent notification

        // 3. Save it to the temporary test file
        service.addEvent(event);

        // 4. Reload from disk to simulate closing and reopening the app
        EventStorageService reloaded = new EventStorageService(tempFile);
        CalendarEvent loaded = reloaded.getAllEvents().get(0);

        // 5. Assert that the memory survived the JSON serialization
        assertNotNull(loaded.getReminderMinutes(), "Reminder minutes should not be lost");
        assertEquals(15, loaded.getReminderMinutes(), "Reminder should remain 15 minutes");

        assertNotNull(loaded.getLastReminderSentDate(), "The sent date memory should not be lost");
        assertEquals(LocalDate.of(2026, 3, 15), loaded.getLastReminderSentDate(),
                "The app must remember the exact date the notification was sent");
    }

    @Test
    void testCreateEvent_NoReminderDefaultsToNull() {
        // 1. Create an event where the user left the reminder checkbox UNCHECKED
        CalendarEvent event = new CalendarEvent(
                "PrEP Reminder", LocalDate.of(2026, 3, 15),
                LocalTime.of(9, 0), EventType.MEDICATION,
                "Take daily", "200mg");

        // We purposely do NOT set reminder minutes or the sent date here

        service.addEvent(event);

        EventStorageService reloaded = new EventStorageService(tempFile);
        CalendarEvent loaded = reloaded.getAllEvents().get(0);

        // 2. Assert that the JSON gracefully handles events with no reminders
        assertNull(loaded.getReminderMinutes(), "Events without reminders should stay null");
        assertNull(loaded.getLastReminderSentDate(), "Events without reminders should have a null sent date");
    }
    @Test
    void testUpdateEvent_preventsDuplicateNotifications() {
        // Arrange: Create and add an event
        CalendarEvent event = new CalendarEvent();
        event.setId("integ-update-1");
        event.setName("Original Event");
        event.setDate(LocalDate.now());
        event.setTime(LocalTime.now().plusHours(1));
        event.setReminderMinutes(10);

        service.addEvent(event); // <-- CHANGED to 'service'
        assertEquals(1, com.sddp.sexualhealthapp.util.NotificationService.getActiveTaskCount());

        // Act: Update the event time and save it
        event.setTime(LocalTime.now().plusHours(2));
        service.updateEvent(event); // <-- CHANGED to 'service'

        // Assert: Ensure we didn't duplicate the background task
        assertEquals(1, com.sddp.sexualhealthapp.util.NotificationService.getActiveTaskCount(),
                "Updating an event via storage service should replace, not duplicate, the notification task.");
    }
}
