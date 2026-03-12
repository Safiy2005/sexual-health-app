package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling application notifications.
 * Uses ControlsFX for safe, cross-platform JavaFX notifications.
 */
public class NotificationService {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void showWelcomeNotification() {
        Platform.runLater(() -> {
            Notifications.create()
                    .title("App Unlocked")
                    .text("Welcome to a totally normal calculator")
                    .position(Pos.BOTTOM_RIGHT)
                    .hideAfter(Duration.seconds(5))
                    .show();
        });
    }

    public static void scheduleEventReminder(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
        // Prevents scheduling if missing data, or if we already sent a reminder for this specific occurrence
        if (event.getTime() == null || event.getReminderMinutes() == null ||
                occurrenceDate.equals(event.getLastReminderSentDate())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDateTime = LocalDateTime.of(occurrenceDate, event.getTime());
        LocalDateTime reminderTime = eventDateTime.minusMinutes(event.getReminderMinutes());

        if (reminderTime.isAfter(now)) {
            long delayInMillis = ChronoUnit.MILLIS.between(now, reminderTime);
            scheduler.schedule(() -> showEventToastAndSave(event, occurrenceDate, storageService), delayInMillis, TimeUnit.MILLISECONDS);
        } else if (eventDateTime.isAfter(now)) {
            // Catch-up notification for missed reminders (shows after 2 seconds)
            scheduler.schedule(() -> showEventToastAndSave(event, occurrenceDate, storageService), 2000, TimeUnit.MILLISECONDS);
        }
    }

    private static void showEventToastAndSave(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
        String description = event.getDescription() != null ? event.getDescription() : "You have an upcoming event.";

        Platform.runLater(() -> {
            Notifications.create()
                    .title("Reminder: " + event.getName())
                    .text(event.getTime().toString() + " - " + description)
                    .position(Pos.BOTTOM_RIGHT)
                    .hideAfter(Duration.seconds(10))
                    .showInformation();
        });

        // Update the event state so we don't notify them again today, then save to storage
        event.setLastReminderSentDate(occurrenceDate);
        if (storageService != null) {
            storageService.updateEvent(event);
        }
    }

    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}