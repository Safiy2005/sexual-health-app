package com.sddp.sexualhealthapp.util;

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
    public static void scheduleEventReminder(CalendarEvent event, int minutesInAdvance) {
        // Skip if there's no specific time set for the event, eg all day ones
        if (event.getTime() == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDateTime = LocalDateTime.of(event.getDate(), event.getTime());
        LocalDateTime reminderTime = eventDateTime.minusMinutes(minutesInAdvance);

        if (reminderTime.isAfter(now)) {
            // FUTURE: The reminder time hasn't happened yet. Schedule it!
            long delayInMillis = ChronoUnit.MILLIS.between(now, reminderTime);
            scheduler.schedule(() -> showEventToast(event), delayInMillis, TimeUnit.MILLISECONDS);
            System.out.println("Scheduled reminder for: " + event.getName());

        } else if (eventDateTime.isAfter(now)) {
            // MISSED: The reminder time passed while the app was closed,
            // but the event hasn't happened yet! Show it immediately.
            showEventToast(event);
        }
    }

    private static void showEventToast(CalendarEvent event) {
        String description = event.getDescription() != null ? event.getDescription() : "You have an upcoming event.";
        Toast.toast(ToastType.INFO, "Reminder: " + event.getName(), event.getTime().toString() + " - " + description);
    }

    // Good practice to shut down the thread when the app closes
    public static void shutdown() {
        scheduler.shutdownNow();
    }
}