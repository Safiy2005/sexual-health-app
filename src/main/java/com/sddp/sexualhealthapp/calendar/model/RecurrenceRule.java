package com.sddp.sexualhealthapp.calendar.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines how a {@link CalendarEvent} repeats over time. Supports daily, weekly,
 * monthly, and yearly recurrence with configurable intervals, day-of-week
 * constraints, monthly patterns, end conditions, and exception dates.
 *
 * <p>Inspired by the iCalendar RRULE specification (RFC 5545), simplified
 * for the needs of this application. Used across stories 47 (calendar view),
 * 48 (event feed), 22 (event creation), and 40 (medication reminders).</p>
 *
 * <p>Use the static factory methods for common patterns:</p>
 * <pre>
 *   RecurrenceRule.daily()                          // every day
 *   RecurrenceRule.weekly(DayOfWeek.TUESDAY,        // every Tue and Thu
 *                         DayOfWeek.THURSDAY)
 *   RecurrenceRule.monthlyOnLastDay()               // last day of each month
 *   RecurrenceRule.yearly()                         // every year on same date
 * </pre>
 */
public class RecurrenceRule {

    /**
     * The base frequency of recurrence.
     */
    public enum Frequency {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }

    /**
     * How the day is determined for monthly recurrence.
     * <ul>
     *   <li>{@code DAY_OF_MONTH} — same day number each month (e.g., the 15th)</li>
     *   <li>{@code LAST_DAY} — last day of each month</li>
     *   <li>{@code NTH_WEEKDAY} — same Nth weekday (e.g., 3rd Wednesday),
     *       derived from the event's start date position in its month</li>
     * </ul>
     */
    public enum MonthlyPattern {
        DAY_OF_MONTH, LAST_DAY, NTH_WEEKDAY
    }

    /**
     * How the recurrence series ends.
     * <ul>
     *   <li>{@code NEVER} — repeats indefinitely</li>
     *   <li>{@code UNTIL} — repeats up to and including a specific date</li>
     *   <li>{@code COUNT} — repeats for a fixed number of occurrences</li>
     * </ul>
     */
    public enum EndType {
        NEVER, UNTIL, COUNT
    }

    private Frequency frequency;
    private int interval;
    private Set<DayOfWeek> daysOfWeek;
    private MonthlyPattern monthlyPattern;
    private EndType endType;
    private LocalDate endDate;
    private int occurrenceCount;
    private Set<LocalDate> excludedDates;

    /**
     * No-arg constructor for Gson deserialization.
     */
    public RecurrenceRule() {
        this.interval = 1;
        this.endType = EndType.NEVER;
    }

    private RecurrenceRule(Frequency frequency) {
        this.frequency = frequency;
        this.interval = 1;
        this.endType = EndType.NEVER;
    }

    // ── Static factory methods ─────────────────────────────────────

    /** Every {@code interval} days. */
    public static RecurrenceRule daily() {
        return new RecurrenceRule(Frequency.DAILY);
    }

    /** Every {@code interval} days. */
    public static RecurrenceRule daily(int interval) {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY);
        rule.interval = interval;
        return rule;
    }

    /** Every week on the given days (or the event's own day if none specified). */
    public static RecurrenceRule weekly(DayOfWeek... days) {
        RecurrenceRule rule = new RecurrenceRule(Frequency.WEEKLY);
        if (days.length > 0) {
            rule.daysOfWeek = EnumSet.copyOf(Set.of(days));
        }
        return rule;
    }

    /** Every {@code interval} weeks on the given days. */
    public static RecurrenceRule weekly(int interval, DayOfWeek... days) {
        RecurrenceRule rule = weekly(days);
        rule.interval = interval;
        return rule;
    }

    /** Every month on the same day of month as the event's start date. */
    public static RecurrenceRule monthlyOnDay() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY);
        rule.monthlyPattern = MonthlyPattern.DAY_OF_MONTH;
        return rule;
    }

    /** Every {@code interval} months on the same day of month. */
    public static RecurrenceRule monthlyOnDay(int interval) {
        RecurrenceRule rule = monthlyOnDay();
        rule.interval = interval;
        return rule;
    }

    /** Every month on the last day of the month. */
    public static RecurrenceRule monthlyOnLastDay() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY);
        rule.monthlyPattern = MonthlyPattern.LAST_DAY;
        return rule;
    }

    /** Every {@code interval} months on the last day. */
    public static RecurrenceRule monthlyOnLastDay(int interval) {
        RecurrenceRule rule = monthlyOnLastDay();
        rule.interval = interval;
        return rule;
    }

    /**
     * Every month on the same Nth weekday as the event's start date.
     * For example, if the event starts on the 2nd Tuesday of February,
     * it repeats on the 2nd Tuesday of every subsequent month.
     */
    public static RecurrenceRule monthlyOnNthWeekday() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY);
        rule.monthlyPattern = MonthlyPattern.NTH_WEEKDAY;
        return rule;
    }

    /** Every {@code interval} months on the same Nth weekday. */
    public static RecurrenceRule monthlyOnNthWeekday(int interval) {
        RecurrenceRule rule = monthlyOnNthWeekday();
        rule.interval = interval;
        return rule;
    }

    /** Every year on the same month and day as the event's start date. */
    public static RecurrenceRule yearly() {
        return new RecurrenceRule(Frequency.YEARLY);
    }

    /** Every {@code interval} years. */
    public static RecurrenceRule yearly(int interval) {
        RecurrenceRule rule = yearly();
        rule.interval = interval;
        return rule;
    }

    // ── Fluent end-condition setters ───────────────────────────────

    /** Sets the recurrence to end after the given date (inclusive). */
    public RecurrenceRule until(LocalDate date) {
        this.endType = EndType.UNTIL;
        this.endDate = date;
        return this;
    }

    /** Sets the recurrence to end after the given number of occurrences. */
    public RecurrenceRule times(int count) {
        this.endType = EndType.COUNT;
        this.occurrenceCount = count;
        return this;
    }

    /** Adds exception dates on which the event should not occur. */
    public RecurrenceRule excluding(LocalDate... dates) {
        if (this.excludedDates == null) {
            this.excludedDates = new HashSet<>();
        }
        for (LocalDate d : dates) {
            this.excludedDates.add(d);
        }
        return this;
    }

    // ── Core occurrence logic ──────────────────────────────────────

    /**
     * Determines whether a recurring event with the given start date
     * produces an occurrence on the query date.
     *
     * @param startDate the event's original start date (first occurrence)
     * @param queryDate the date to check
     * @return true if the recurrence pattern matches the query date
     */
    public boolean occursOn(LocalDate startDate, LocalDate queryDate) {
        if (queryDate.isBefore(startDate)) return false;

        // Check excluded dates
        if (excludedDates != null && excludedDates.contains(queryDate)) return false;

        // Check UNTIL end condition
        if (endType == EndType.UNTIL && endDate != null && queryDate.isAfter(endDate)) {
            return false;
        }

        // Check pattern match
        boolean patternMatch = switch (frequency) {
            case DAILY -> isDailyMatch(startDate, queryDate);
            case WEEKLY -> isWeeklyMatch(startDate, queryDate);
            case MONTHLY -> isMonthlyMatch(startDate, queryDate);
            case YEARLY -> isYearlyMatch(startDate, queryDate);
        };

        if (!patternMatch) return false;

        // Check COUNT end condition (requires counting occurrences)
        if (endType == EndType.COUNT) {
            return countOccurrencesUpTo(startDate, queryDate) <= occurrenceCount;
        }

        return true;
    }

    private boolean isDailyMatch(LocalDate startDate, LocalDate queryDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, queryDate);
        return daysBetween % interval == 0;
    }

    private boolean isWeeklyMatch(LocalDate startDate, LocalDate queryDate) {
        // Determine which days to match
        Set<DayOfWeek> targetDays = (daysOfWeek != null && !daysOfWeek.isEmpty())
                ? daysOfWeek
                : EnumSet.of(startDate.getDayOfWeek());

        if (!targetDays.contains(queryDate.getDayOfWeek())) return false;

        // Calculate week offset using ISO weeks (Monday = start of week)
        LocalDate startMonday = toMonday(startDate);
        LocalDate queryMonday = toMonday(queryDate);
        long weeksBetween = ChronoUnit.WEEKS.between(startMonday, queryMonday);

        return weeksBetween >= 0 && weeksBetween % interval == 0;
    }

    private boolean isMonthlyMatch(LocalDate startDate, LocalDate queryDate) {
        // Check month interval alignment
        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(startDate), YearMonth.from(queryDate));
        if (monthsBetween < 0 || monthsBetween % interval != 0) return false;

        MonthlyPattern pattern = (monthlyPattern != null) ? monthlyPattern : MonthlyPattern.DAY_OF_MONTH;

        return switch (pattern) {
            case DAY_OF_MONTH -> queryDate.getDayOfMonth() == startDate.getDayOfMonth();
            case LAST_DAY -> queryDate.getDayOfMonth() == queryDate.lengthOfMonth();
            case NTH_WEEKDAY -> isNthWeekdayMatch(startDate, queryDate);
        };
    }

    private boolean isNthWeekdayMatch(LocalDate startDate, LocalDate queryDate) {
        // Must be the same day of week
        if (queryDate.getDayOfWeek() != startDate.getDayOfWeek()) return false;

        // Calculate which "Nth" occurrence (1-based) in the month
        int startNth = (startDate.getDayOfMonth() - 1) / 7 + 1;
        int queryNth = (queryDate.getDayOfMonth() - 1) / 7 + 1;

        return queryNth == startNth;
    }

    private boolean isYearlyMatch(LocalDate startDate, LocalDate queryDate) {
        long yearsBetween = ChronoUnit.YEARS.between(startDate, queryDate);
        if (yearsBetween < 0 || yearsBetween % interval != 0) return false;

        // Same month and day (handles leap year: Feb 29 only matches on leap years)
        return queryDate.getMonthValue() == startDate.getMonthValue()
                && queryDate.getDayOfMonth() == startDate.getDayOfMonth();
    }

    /**
     * Counts how many occurrences of this pattern fall between startDate
     * and queryDate (inclusive). Used for COUNT end-condition checking.
     * Iterates using frequency-aware stepping for efficiency.
     */
    private int countOccurrencesUpTo(LocalDate startDate, LocalDate queryDate) {
        int count = 0;
        LocalDate current = startDate;

        while (!current.isAfter(queryDate)) {
            if (isOccurrenceCandidate(startDate, current)) {
                if (excludedDates == null || !excludedDates.contains(current)) {
                    count++;
                }
            }
            if (current.equals(queryDate)) break;
            current = advanceToNext(startDate, current);
            if (current == null) break;
        }

        return count;
    }

    /**
     * Checks if the date matches the frequency pattern (without end-condition
     * or exclusion checks). Used internally for COUNT iteration.
     */
    private boolean isOccurrenceCandidate(LocalDate startDate, LocalDate candidate) {
        return switch (frequency) {
            case DAILY -> isDailyMatch(startDate, candidate);
            case WEEKLY -> isWeeklyMatch(startDate, candidate);
            case MONTHLY -> isMonthlyMatch(startDate, candidate);
            case YEARLY -> isYearlyMatch(startDate, candidate);
        };
    }

    /**
     * Advances from the current date to the next candidate date for the
     * frequency type. Ensures COUNT iteration is efficient (no day-by-day scan).
     */
    private LocalDate advanceToNext(LocalDate startDate, LocalDate current) {
        return switch (frequency) {
            case DAILY -> current.plusDays(interval);
            case WEEKLY -> advanceWeekly(startDate, current);
            case MONTHLY -> advanceMonthly(current);
            case YEARLY -> current.plusYears(interval);
        };
    }

    private LocalDate advanceWeekly(LocalDate startDate, LocalDate current) {
        Set<DayOfWeek> targetDays = (daysOfWeek != null && !daysOfWeek.isEmpty())
                ? daysOfWeek
                : EnumSet.of(startDate.getDayOfWeek());

        // Try the next day in the current week
        LocalDate next = current.plusDays(1);
        LocalDate weekEnd = toMonday(current).plusDays(7);

        while (next.isBefore(weekEnd)) {
            if (targetDays.contains(next.getDayOfWeek())) {
                return next;
            }
            next = next.plusDays(1);
        }

        // Jump to the next valid week and find the first target day
        LocalDate nextWeekMonday = toMonday(current).plusWeeks(interval);
        for (int d = 0; d < 7; d++) {
            LocalDate candidate = nextWeekMonday.plusDays(d);
            if (targetDays.contains(candidate.getDayOfWeek())) {
                return candidate;
            }
        }

        return null; // should never happen if targetDays is non-empty
    }

    private LocalDate advanceMonthly(LocalDate current) {
        return current.plusMonths(interval);
    }

    /** Returns the Monday of the ISO week containing the given date. */
    private static LocalDate toMonday(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    // ── Getters and setters ────────────────────────────────────────

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public MonthlyPattern getMonthlyPattern() {
        return monthlyPattern;
    }

    public void setMonthlyPattern(MonthlyPattern monthlyPattern) {
        this.monthlyPattern = monthlyPattern;
    }

    public EndType getEndType() {
        return endType;
    }

    public void setEndType(EndType endType) {
        this.endType = endType;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public Set<LocalDate> getExcludedDates() {
        return excludedDates;
    }

    public void setExcludedDates(Set<LocalDate> excludedDates) {
        this.excludedDates = excludedDates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurrenceRule that = (RecurrenceRule) o;
        return interval == that.interval
                && occurrenceCount == that.occurrenceCount
                && frequency == that.frequency
                && Objects.equals(daysOfWeek, that.daysOfWeek)
                && monthlyPattern == that.monthlyPattern
                && endType == that.endType
                && Objects.equals(endDate, that.endDate)
                && Objects.equals(excludedDates, that.excludedDates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, interval, daysOfWeek, monthlyPattern,
                endType, endDate, occurrenceCount, excludedDates);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RecurrenceRule{");
        sb.append(frequency);
        if (interval > 1) sb.append(" every ").append(interval);
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) sb.append(" on ").append(daysOfWeek);
        if (monthlyPattern != null && monthlyPattern != MonthlyPattern.DAY_OF_MONTH) {
            sb.append(" ").append(monthlyPattern);
        }
        if (endType == EndType.UNTIL) sb.append(" until ").append(endDate);
        if (endType == EndType.COUNT) sb.append(" x").append(occurrenceCount);
        if (excludedDates != null && !excludedDates.isEmpty()) {
            sb.append(" excluding ").append(excludedDates.size()).append(" dates");
        }
        sb.append('}');
        return sb.toString();
    }
}
