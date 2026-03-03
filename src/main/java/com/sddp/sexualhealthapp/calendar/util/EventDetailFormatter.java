package com.sddp.sexualhealthapp.calendar.util;

import com.sddp.sexualhealthapp.calendar.model.EventType;
import com.sddp.sexualhealthapp.calendar.model.RecurrenceRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Presentation formatter for event detail views.
 */
public final class EventDetailFormatter {

    private static final String UNTITLED_EVENT = "Untitled event";
    private static final String NO_DESCRIPTION = "No description provided";
    private static final String ALL_DAY = "All day";

    private EventDetailFormatter() {
    }

    public static String formatEventName(String name) {
        String trimmed = trimToNull(name);
        return trimmed == null ? UNTITLED_EVENT : trimmed;
    }

    public static String formatDateTime(LocalDate eventDate, LocalDate occurrenceDate, LocalTime time, Locale locale) {
        Locale activeLocale = locale == null ? Locale.getDefault() : locale;
        LocalDate displayDate = occurrenceDate != null ? occurrenceDate : eventDate;
        String timeText = time == null ? ALL_DAY : time.format(DateTimeFormatter.ofPattern("HH:mm", activeLocale));

        if (displayDate == null) {
            return timeText;
        }

        String dateText = displayDate.format(DateTimeFormatter.ofPattern("d MMMM uuuu", activeLocale));
        return dateText + " \u00b7 " + timeText;
    }

    public static String formatDescription(String description) {
        String trimmed = trimToNull(description);
        return trimmed == null ? NO_DESCRIPTION : trimmed;
    }

    public static boolean shouldShowDosage(EventType type, String dosage) {
        return type == EventType.MEDICATION && trimToNull(dosage) != null;
    }

    public static String formatDosage(String dosage) {
        String trimmed = trimToNull(dosage);
        return trimmed == null ? "" : trimmed;
    }

    public static Optional<String> formatRecurrence(RecurrenceRule rule, LocalDate startDate, Locale locale) {
        if (rule == null || rule.getFrequency() == null) {
            return Optional.empty();
        }

        Locale activeLocale = locale == null ? Locale.getDefault() : locale;
        String baseText = switch (rule.getFrequency()) {
            case DAILY -> formatDaily(rule);
            case WEEKLY -> formatWeekly(rule, startDate, activeLocale);
            case MONTHLY -> formatMonthly(rule, startDate, activeLocale);
            case YEARLY -> formatYearly(rule);
        };

        return Optional.of(appendEndCondition(baseText, rule, activeLocale));
    }

    private static String formatDaily(RecurrenceRule rule) {
        int interval = normalizeInterval(rule.getInterval());
        return interval == 1 ? "Daily" : "Every " + interval + " days";
    }

    private static String formatWeekly(RecurrenceRule rule, LocalDate startDate, Locale locale) {
        int interval = normalizeInterval(rule.getInterval());
        String dayPhrase = joinDayNames(resolveWeeklyDays(rule, startDate), locale);

        if (interval == 1) {
            return dayPhrase.isBlank() ? "Weekly" : "Every " + dayPhrase;
        }

        return dayPhrase.isBlank()
                ? "Every " + interval + " weeks"
                : "Every " + interval + " weeks on " + dayPhrase;
    }

    private static String formatMonthly(RecurrenceRule rule, LocalDate startDate, Locale locale) {
        int interval = normalizeInterval(rule.getInterval());
        RecurrenceRule.MonthlyPattern pattern = rule.getMonthlyPattern() == null
                ? RecurrenceRule.MonthlyPattern.DAY_OF_MONTH
                : rule.getMonthlyPattern();

        String suffix = switch (pattern) {
            case DAY_OF_MONTH -> "";
            case LAST_DAY -> " (last day)";
            case NTH_WEEKDAY -> nthWeekdaySuffix(startDate, locale);
        };

        if (interval == 1) {
            return pattern == RecurrenceRule.MonthlyPattern.DAY_OF_MONTH
                    ? "Monthly"
                    : "Monthly" + suffix;
        }

        return pattern == RecurrenceRule.MonthlyPattern.DAY_OF_MONTH
                ? "Every " + interval + " months"
                : "Every " + interval + " months" + suffix;
    }

    private static String formatYearly(RecurrenceRule rule) {
        int interval = normalizeInterval(rule.getInterval());
        return interval == 1 ? "Yearly" : "Every " + interval + " years";
    }

    private static String appendEndCondition(String baseText, RecurrenceRule rule, Locale locale) {
        RecurrenceRule.EndType endType = rule.getEndType();
        if (endType == null || endType == RecurrenceRule.EndType.NEVER) {
            return baseText;
        }

        if (endType == RecurrenceRule.EndType.UNTIL && rule.getEndDate() != null) {
            String endDateText = rule.getEndDate().format(DateTimeFormatter.ofPattern("d MMMM uuuu", locale));
            return baseText + " until " + endDateText;
        }

        if (endType == RecurrenceRule.EndType.COUNT && rule.getOccurrenceCount() > 0) {
            int count = rule.getOccurrenceCount();
            return baseText + " for " + count + " occurrence" + (count == 1 ? "" : "s");
        }

        return baseText;
    }

    private static List<DayOfWeek> resolveWeeklyDays(RecurrenceRule rule, LocalDate startDate) {
        Set<DayOfWeek> daysOfWeek = rule.getDaysOfWeek();
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            List<DayOfWeek> sorted = new ArrayList<>(daysOfWeek);
            sorted.sort(Comparator.comparingInt(DayOfWeek::getValue));
            return sorted;
        }

        if (startDate != null) {
            return List.of(startDate.getDayOfWeek());
        }

        return List.of();
    }

    private static String joinDayNames(List<DayOfWeek> days, Locale locale) {
        if (days.isEmpty()) {
            return "";
        }

        List<String> names = days.stream()
                .map(day -> titleCase(day.getDisplayName(TextStyle.FULL, locale)))
                .toList();

        if (names.size() == 1) {
            return names.get(0);
        }
        if (names.size() == 2) {
            return names.get(0) + " and " + names.get(1);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i == names.size() - 1) {
                builder.append("and ").append(names.get(i));
            } else {
                builder.append(names.get(i)).append(", ");
            }
        }
        return builder.toString();
    }

    private static String nthWeekdaySuffix(LocalDate startDate, Locale locale) {
        if (startDate == null) {
            return "";
        }

        int nth = ((startDate.getDayOfMonth() - 1) / 7) + 1;
        String dayName = titleCase(startDate.getDayOfWeek().getDisplayName(TextStyle.FULL, locale));
        return " (" + ordinal(nth) + " " + dayName + ")";
    }

    private static String ordinal(int value) {
        int mod100 = value % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return value + "th";
        }

        return switch (value % 10) {
            case 1 -> value + "st";
            case 2 -> value + "nd";
            case 3 -> value + "rd";
            default -> value + "th";
        };
    }

    private static int normalizeInterval(int interval) {
        return interval > 0 ? interval : 1;
    }

    private static String trimToNull(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String titleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
