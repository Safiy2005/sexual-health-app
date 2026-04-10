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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Service for handling application notifications.
 * Uses ControlsFX for safe, cross-platform JavaFX notifications.
 */
public class NotificationService {
    // map to track each reminder by an id
    private static final Map<String, java.util.concurrent.ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // cancels scheduled reminders, prevents persistence after event deleted or modded
    public static void cancelScheduledReminder(String eventId) {
        java.util.concurrent.ScheduledFuture<?> task = scheduledTasks.remove(eventId);
        if (task != null) task.cancel(false);
    }
    public static void scheduleEventReminder(CalendarEvent event, LocalDate occurrenceDate, EventStorageService storageService) {
        if (event.getTime() == null || event.getReminderMinutes() == null ||
                occurrenceDate.equals(event.getLastReminderSentDate())) {
            return;
        }

        // cancel notifs with same id, prevent dupes when editing
        cancelScheduledReminder(event.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventDateTime = LocalDateTime.of(occurrenceDate, event.getTime());
        LocalDateTime reminderTime = eventDateTime.minusMinutes(event.getReminderMinutes());

        long delay;
        if (reminderTime.isAfter(now)) {
            delay = java.time.temporal.ChronoUnit.MILLIS.between(now, reminderTime);
        } else if (eventDateTime.isAfter(now)) {
            delay = 2000; // trigger if in reminder window but not yet fired (delay to prevent loading issues)
        } else {return;}

        java.util.concurrent.ScheduledFuture<?> task = scheduler.schedule(() -> {
            try {
                showEventToastAndSave(event, occurrenceDate, storageService);
            } finally {scheduledTasks.remove(event.getId());}}, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        scheduledTasks.put(event.getId(), task);
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

    // --- For Testing ---

    public static int getActiveTaskCount() {
        return scheduledTasks.size();
    }

    public static void clearAllTasks() {
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
    }

}


