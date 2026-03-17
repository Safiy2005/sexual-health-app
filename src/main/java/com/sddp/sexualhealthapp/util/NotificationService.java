package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences;
import com.sddp.sexualhealthapp.settings.model.ReminderPreferences.VisibilityMode;
import com.sddp.sexualhealthapp.settings.service.ReminderPreferencesService;
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
                    .showInformation();
        });
    }

    public static void scheduleEventReminder(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
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
            scheduler.schedule(() -> showEventToastAndSave(event, occurrenceDate, storageService), 2000, TimeUnit.MILLISECONDS);
        }
    }

    private static void showEventToastAndSave(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
        // Fetch the very latest preferences right before displaying
        ReminderPreferences prefs = ReminderPreferencesService.getInstance().getPreferences();
        VisibilityMode mode = prefs.visibilityMode();

        // 1. If OFF, just save the state to prevent future firing and exit early
        if (mode == VisibilityMode.OFF) {
            updateEventState(event, occurrenceDate, storageService);
            return;
        }

        // 2. Format based on visibility mode
        String title;
        String text;

        switch (mode) {
            case EXPLICIT:
                String description = event.getDescription() != null ? event.getDescription() : "You have an upcoming event.";
                title = "Reminder: " + event.getName();
                text = event.getTime().toString() + " - " + description;
                break;

            case DISCREET:
                title = "Upcoming Event";
                text = "You have an event scheduled for " + event.getTime().toString();
                break;

            case DISGUISED:
            default:
                // Because of the compact constructor, we know these are valid strings
                title = prefs.customDisguisedTitle();
                text = prefs.customDisguisedBody() + " " + event.getTime().toString();
                break;
        }
        // 3. Show the notification
        Platform.runLater(() -> {
            Notifications.create()
                    .title(title)
                    .text(text)
                    .position(Pos.BOTTOM_RIGHT)
                    .hideAfter(Duration.seconds(10))
                    .showWarning();
        });

        // 4. Update state and save
        updateEventState(event, occurrenceDate, storageService);
    }

    private static void updateEventState(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
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