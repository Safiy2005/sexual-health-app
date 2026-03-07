package com.sddp.sexualhealthapp.util;

import com.sddp.sexualhealthapp.calendar.service.EventStorageService;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static void showWelcomeNotification() {
        sendNativeNotification("App Unlocked", "Welcome to a totally normal calculator");
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
        String description = event.getDescription() != null ? event.getDescription() : "You have an upcoming event.";

        sendNativeNotification("Reminder: " + event.getName(), event.getTime().toString() + " - " + description);

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

    // sends plain notification, no icons. mac should have java icon as its tricky like that
    private static void sendNativeNotification(String title, String message) {
        try {
            if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
                // Linux: Simplest form of notify-send
                new ProcessBuilder("notify-send", title, message).start();

            } else if (OS.contains("win")) {
                // windows notif uses windows.ui.notifications

                // Commented out because it blue screens windows machines

                // String script = String.format(
                //         "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null; " +
                //                 "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02); " +
                //                 "$nodes = $template.GetElementsByTagName('text'); " +
                //                 "$nodes.Item(0).AppendChild($template.CreateTextNode('%s')) | Out-Null; " +
                //                 "$nodes.Item(1).AppendChild($template.CreateTextNode('%s')) | Out-Null; " +
                //                 "$toast = [Windows.UI.Notifications.ToastNotification]::new($template); " +
                //                 "$appId = '{1AC14E77-02E7-4E5D-B744-2EB1AE5198B7}\\WindowsPowerShell\\v1.0\\powershell.exe'; " +
                //                 "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($appId).Show($toast);",
                //         title.replace("'", "''"),
                //         message.replace("'", "''")
                // );
                // new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", script).start();

            } else if (OS.contains("mac")) {
                // applescript notif
                String script = String.format(
                        "display notification \"%s\" with title \"%s\"",
                        message.replace("\"", "\\\""),
                        title.replace("\"", "\\\"")
                );
                new ProcessBuilder("osascript", "-e", script).start();
            }
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }
}