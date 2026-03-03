package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;

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

    // event reminder logic
    public static void scheduleEventReminder(CalendarEvent event, EventStorageService storageService) {
        // 1. Guard clause: Stop immediately if no time, no reminder, or it's ALREADY SENT!
        if (event.getTime() == null || event.getReminderMinutes() == null || event.isReminderSent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDateTime = LocalDateTime.of(event.getDate(), event.getTime());
        LocalDateTime reminderTime = eventDateTime.minusMinutes(event.getReminderMinutes());

        if (reminderTime.isAfter(now)) {
            // SCENARIO A: FUTURE. Schedule it normally.
            long delayInMillis = ChronoUnit.MILLIS.between(now, reminderTime);
            scheduler.schedule(() -> showEventToastAndSave(event, storageService), delayInMillis, TimeUnit.MILLISECONDS);
            System.out.println("Scheduled reminder for: " + event.getName());

        } else if (eventDateTime.isAfter(now)) {
            // SCENARIO B: MISSED IT! (Catch-up logic)
            // 2-second delay so the app UI loads before the OS popup fires
            scheduler.schedule(() -> showEventToastAndSave(event, storageService), 2000, TimeUnit.MILLISECONDS);
            System.out.println("Catch-up reminder triggered for: " + event.getName());
        }
    }

    private static void showEventToastAndSave(CalendarEvent event, EventStorageService storageService) {
        String description = event.getDescription() != null ? event.getDescription() : "You have an upcoming event.";
        Toast.toast(ToastType.INFO, "Reminder: " + event.getName(), event.getTime().toString() + " - " + description);

        // 2. Mark the event as sent!
        event.setReminderSent(true);

        // 3. Save the updated event back to events.json so it remembers forever
        if (storageService != null) {
            storageService.updateEvent(event);
        }
    }
    // Good practice to shut down the thread when the app closes
    public static void shutdown() {
        scheduler.shutdownNow();
    }
}