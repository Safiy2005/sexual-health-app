package com.sddp.sexualhealthapp.calendar.controller;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventOccurrence;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventFeedControllerTest {

    private Path tempFile;
    private EventStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("event-feed-test-", ".json");
        Files.delete(tempFile); // Start with no file for deterministic setup
        service = new EventStorageService(tempFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    void loadBatchSkippingEmptyWindows_FindsLaterEventAfterEmptyWeek() {
        CalendarEvent march8Event = new CalendarEvent(
                "Vaccination Appointment",
                LocalDate.of(2026, 3, 8),
                LocalTime.of(11, 0),
                EventType.APPOINTMENT,
                null,
                null);
        service.addEvent(march8Event);

        EventFeedController.BatchLoadResult result = EventFeedController.loadBatchSkippingEmptyWindows(
                service,
                LocalDate.of(2026, 3, 1), // First 7-day window (Mar 1-7) is empty
                7,
                4);

        List<EventOccurrence> occurrences = result.occurrences();
        assertEquals(1, occurrences.size());
        assertEquals(LocalDate.of(2026, 3, 8), occurrences.get(0).occurrenceDate());
        assertEquals("Vaccination Appointment", occurrences.get(0).event().getName());
        assertEquals(LocalDate.of(2026, 3, 15), result.nextBatchStart());
    }

    @Test
    void loadBatchSkippingEmptyWindows_ReturnsEmptyAfterLookaheadLimit() {
        EventFeedController.BatchLoadResult result = EventFeedController.loadBatchSkippingEmptyWindows(
                service,
                LocalDate.of(2026, 3, 1),
                7,
                2);

        assertTrue(result.occurrences().isEmpty());
        // With maxEmptyWindowsToSkip=2, windows checked are Mar 1-7, Mar 8-14, Mar 15-21
        assertEquals(LocalDate.of(2026, 3, 22), result.nextBatchStart());
    }
}
