package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    // Creates a background thread that stays alive to trigger reminders, stops stutters
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // test notif for when uinlocked
    public static void showWelcomeNotification() {
        Toast.toast(ToastType.INFO, "App Unlocked", "Welcome to a totally normal calculator");
    }

    public static void scheduleEventReminder(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
        // 1. Guard clause: Stop if no time, no reminder, or if WE ALREADY SENT IT FOR THIS SPECIFIC DATE
        if (event.getTime() == null || event.getReminderMinutes() == null ||
                occurrenceDate.equals(event.getLastReminderSentDate())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        // HUGE FIX: Use the occurrenceDate (today), NOT the event's original start date!
        LocalDateTime eventDateTime = LocalDateTime.of(occurrenceDate, event.getTime());
        LocalDateTime reminderTime = eventDateTime.minusMinutes(event.getReminderMinutes());

        if (reminderTime.isAfter(now)) {
            // SCENARIO A: FUTURE
            long delayInMillis = ChronoUnit.MILLIS.between(now, reminderTime);
            scheduler.schedule(() -> showEventToastAndSave(event, occurrenceDate, storageService), delayInMillis, TimeUnit.MILLISECONDS);

        } else if (eventDateTime.isAfter(now)) {
            // SCENARIO B: MISSED IT
            scheduler.schedule(() -> showEventToastAndSave(event, occurrenceDate, storageService), 2000, TimeUnit.MILLISECONDS);
        }
    }

    private static void showEventToastAndSave(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
        String description = event.getDescription() != null ? event.getDescription() : "You have an upcoming event.";
        Toast.toast(ToastType.INFO, "Reminder: " + event.getName(), event.getTime().toString() + " - " + description);

        // 2. Mark this specific date as sent!
        event.setLastReminderSentDate(occurrenceDate);

        // 3. Save to JSON
        if (storageService != null) {
            storageService.updateEvent(event);
        }
    }
    // Good practice to shut down the thread when the app closes
    public static void shutdown() {
        scheduler.shutdownNow();
    }
}