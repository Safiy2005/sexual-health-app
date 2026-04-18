package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationServiceTest {

    @BeforeEach
    void setUp() {
        // Clear the scheduler before every test so they don't interfere with each other
        NotificationService.clearAllTasks();
    }

    @Test
    void testScheduleEventReminder_addsTaskToMap() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setId("test-id-1");
        event.setName("Test Event");
        event.setDate(LocalDate.now());
        event.setTime(LocalTime.now().plusHours(1));
        event.setReminderMinutes(10);

        // Act
        NotificationService.scheduleEventReminder(event, LocalDate.now(), null);

        // Assert: Verify the map is tracking exactly 1 task
        assertEquals(1, NotificationService.getActiveTaskCount(), "Task should be added to the scheduler map.");
    }

    @Test
    void testDeleteEvent_cancelsGhostNotifications() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setId("test-id-2");
        event.setName("Delete Event");
        event.setDate(LocalDate.now());
        event.setTime(LocalTime.now().plusHours(1));
        event.setReminderMinutes(15);

        // Act 1: Schedule it
        NotificationService.scheduleEventReminder(event, LocalDate.now(), null);
        assertEquals(1, NotificationService.getActiveTaskCount());

        // Act 2: Simulate deletion by calling the cancel method
        NotificationService.cancelScheduledReminder(event.getId());

        // Assert: Verify the task is gone (Fixes the ghost notification bug)
        assertEquals(0, NotificationService.getActiveTaskCount(), "Task should be completely removed when cancelled.");
    }

    @Test
    void testEditEvent_preventsDuplicateNotifications() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setId("test-id-3");
        event.setName("Edit Event");
        event.setDate(LocalDate.now());
        event.setTime(LocalTime.now().plusHours(1));
        event.setReminderMinutes(5);

        // Act 1: Schedule the original reminder
        NotificationService.scheduleEventReminder(event, LocalDate.now(), null);
        assertEquals(1, NotificationService.getActiveTaskCount());

        // Act 2: Simulate the user editing the event and changing the reminder time
        event.setReminderMinutes(30);

        // Act 3: Reschedule using the SAME event ID
        NotificationService.scheduleEventReminder(event, LocalDate.now(), null);

        // Assert: Verify we still only have 1 task, not 2 (Fixes the duplicate bug)
        assertEquals(1, NotificationService.getActiveTaskCount(),
                "Editing an event should replace the old task, not duplicate it.");
    }

    @Test
    void testScheduleEventReminder_ignoresEventsWithoutReminders() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setId("edge-case-1");
        event.setDate(LocalDate.now());
        event.setTime(LocalTime.now().plusHours(1));
        event.setReminderMinutes(null); // NO REMINDER SET

        // Act
        NotificationService.scheduleEventReminder(event, LocalDate.now(), null);

        // Assert
        assertEquals(0, NotificationService.getActiveTaskCount(),
                "Should not schedule a task if reminderMinutes is null.");
    }

    @Test
    void testScheduleEventReminder_ignoresAlreadySentReminders() {
        // Arrange
        CalendarEvent event = new CalendarEvent();
        event.setId("edge-case-2");
        event.setDate(LocalDate.now());
        event.setTime(LocalTime.now().plusHours(1));
        event.setReminderMinutes(10);

        // Simulate that the reminder already fired today
        event.setLastReminderSentDate(LocalDate.now());

        // Act
        NotificationService.scheduleEventReminder(event, LocalDate.now(), null);

        // Assert
        assertEquals(0, NotificationService.getActiveTaskCount(),
                "Should not schedule if the reminder was already sent today to prevent spam.");
    }
}