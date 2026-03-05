package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    // Creates a background thread that stays alive to trigger reminders
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Detect the operating system once
    private static final String OS = System.getProperty("os.name").toLowerCase();

    // test notif for when unlocked
    public static void showWelcomeNotification() {
        sendNativeNotification("App Unlocked", "Welcome to a totally normal calculator");
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

        // Replaced two-slices call with our custom native implementation
        sendNativeNotification("Reminder: " + event.getName(), event.getTime().toString() + " - " + description);

        // 2. Mark this specific date as sent!
        event.setLastReminderSentDate(occurrenceDate);

        // 3. Save to JSON
        if (storageService != null) {
            storageService.updateEvent(event);
        }
    }

    // Good practice to shut down the thread when the app closes
    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    // --- INTERNAL CROSS-PLATFORM NOTIFICATION ENGINE --- //

    private static void sendNativeNotification(String title, String message) {
        try {
            if (OS.contains("mac")) {
                sendMacNotification(title, message);
            } else if (OS.contains("win")) {
                sendWindowsNotification(title, message);
            } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
                sendLinuxNotification(title, message);
            } else {
                System.out.println("Unsupported OS for notifications. Title: " + title + " | Msg: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to send native notification: " + e.getMessage());
        }
    }

    private static void sendMacNotification(String title, String message) throws IOException {
        String script = String.format(
                "display notification \"%s\" with title \"%s\" sound name \"Default\"",
                message.replace("\"", "\\\""),
                title.replace("\"", "\\\"")
        );
        new ProcessBuilder("osascript", "-e", script).start();
    }

    private static void sendWindowsNotification(String title, String message) throws IOException {
        String powershellCommand = String.format(
                "$title = '%s'; $msg = '%s'; " +
                        "[Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms'); " +
                        "$notify = New-Object System.Windows.Forms.NotifyIcon; " +
                        "$notify.Icon = [System.Drawing.SystemIcons]::Information; " +
                        "$notify.Visible = $true; " +
                        "$notify.ShowBalloonTip(5000, $title, $msg, [System.Windows.Forms.ToolTipIcon]::Info);",
                title.replace("'", "''"),
                message.replace("'", "''")
        );
        new ProcessBuilder("powershell", "-Command", powershellCommand).start();
    }

    private static void sendLinuxNotification(String title, String message) throws IOException {
        // ProcessBuilder array format handles spaces safely without manual escaping
        new ProcessBuilder("notify-send", title, message).start();
    }
}