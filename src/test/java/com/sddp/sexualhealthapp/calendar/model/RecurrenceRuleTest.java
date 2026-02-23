package com.sddp.sexualhealthapp.calendar.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RecurrenceRule} covering all frequency types, intervals,
 * end conditions, edge cases (leap years, month boundaries), and exclusions.
 */
class RecurrenceRuleTest {

    // ── Daily ──────────────────────────────────────────────────────

    @Test
    void testDaily_OccursEveryDay() {
        RecurrenceRule rule = RecurrenceRule.daily();
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 1)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 2)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 15)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 1)));
    }

    @Test
    void testDaily_WithInterval() {
        RecurrenceRule rule = RecurrenceRule.daily(3);
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 1)));   // day 0
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 2)));  // day 1
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 3)));  // day 2
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 4)));   // day 3
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 7)));   // day 6
    }

    @Test
    void testDaily_BeforeStartDate() {
        RecurrenceRule rule = RecurrenceRule.daily();
        LocalDate start = LocalDate.of(2026, 2, 10);

        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 9)));
    }

    // ── Weekly ─────────────────────────────────────────────────────

    @Test
    void testWeekly_DefaultDay() {
        // Start on a Wednesday — should recur every Wednesday
        LocalDate start = LocalDate.of(2026, 2, 18); // Wednesday
        RecurrenceRule rule = RecurrenceRule.weekly();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 18)));   // same Wed
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 25)));   // next Wed
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 19)));  // Thursday
    }

    @Test
    void testWeekly_SpecificDays() {
        LocalDate start = LocalDate.of(2026, 2, 16); // Monday
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 16))); // Monday
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 17)));  // Tuesday
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 18))); // Wednesday
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 19)));  // Thursday
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 24)));  // next Tuesday
    }

    @Test
    void testWeekly_EveryOtherWeek() {
        LocalDate start = LocalDate.of(2026, 2, 16); // Monday
        RecurrenceRule rule = RecurrenceRule.weekly(2, DayOfWeek.MONDAY);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 16)));   // week 0
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 23)));  // week 1 (skip)
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 2)));    // week 2
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 3, 9)));   // week 3 (skip)
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 16)));   // week 4
    }

    @Test
    void testWeekly_MultiDayEveryOtherWeek() {
        LocalDate start = LocalDate.of(2026, 2, 16); // Monday
        RecurrenceRule rule = RecurrenceRule.weekly(2, DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 16)));   // Mon week 0
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 20)));   // Fri week 0
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 23)));  // Mon week 1
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 27)));  // Fri week 1
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 2)));    // Mon week 2
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 6)));    // Fri week 2
    }

    // ── Monthly (day of month) ─────────────────────────────────────

    @Test
    void testMonthlyOnDay_SameDay() {
        LocalDate start = LocalDate.of(2026, 1, 15);
        RecurrenceRule rule = RecurrenceRule.monthlyOnDay();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 1, 15)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 15)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 15)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 16)));
    }

    @Test
    void testMonthlyOnDay_31stSkipsShortMonths() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        RecurrenceRule rule = RecurrenceRule.monthlyOnDay();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 1, 31)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 28)));  // Feb has no 31st
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 31)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 4, 30)));  // Apr has no 31st
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 5, 31)));
    }

    @Test
    void testMonthlyOnDay_EveryOtherMonth() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        RecurrenceRule rule = RecurrenceRule.monthlyOnDay(2);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 1, 10)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 10)));  // skip
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 10)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 4, 10)));  // skip
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 5, 10)));
    }

    // ── Monthly (last day) ─────────────────────────────────────────

    @Test
    void testMonthlyOnLastDay() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        RecurrenceRule rule = RecurrenceRule.monthlyOnLastDay();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 1, 31)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 28)));   // Feb (non-leap)
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 31)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 4, 30)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 27))); // not last day
    }

    @Test
    void testMonthlyOnLastDay_LeapYear() {
        LocalDate start = LocalDate.of(2028, 1, 31);
        RecurrenceRule rule = RecurrenceRule.monthlyOnLastDay();

        assertTrue(rule.occursOn(start, LocalDate.of(2028, 2, 29)));   // leap year
        assertFalse(rule.occursOn(start, LocalDate.of(2028, 2, 28)));  // not last day in leap year
    }

    // ── Monthly (Nth weekday) ──────────────────────────────────────

    @Test
    void testMonthlyOnNthWeekday() {
        // Feb 18, 2026 is the 3rd Wednesday of February
        LocalDate start = LocalDate.of(2026, 2, 18);
        RecurrenceRule rule = RecurrenceRule.monthlyOnNthWeekday();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 18)));   // 3rd Wed Feb
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 18)));   // 3rd Wed Mar
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 4, 15)));   // 3rd Wed Apr
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 3, 11)));  // 2nd Wed Mar
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 3, 25)));  // 4th Wed Mar
    }

    @Test
    void testMonthlyOnNthWeekday_FirstMonday() {
        // Feb 2, 2026 is the 1st Monday
        LocalDate start = LocalDate.of(2026, 2, 2);
        RecurrenceRule rule = RecurrenceRule.monthlyOnNthWeekday();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 2)));
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 2)));    // 1st Mon Mar
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 4, 6)));    // 1st Mon Apr
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 3, 9)));   // 2nd Mon Mar
    }

    // ── Yearly ─────────────────────────────────────────────────────

    @Test
    void testYearly() {
        LocalDate start = LocalDate.of(2026, 3, 15);
        RecurrenceRule rule = RecurrenceRule.yearly();

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 15)));
        assertTrue(rule.occursOn(start, LocalDate.of(2027, 3, 15)));
        assertTrue(rule.occursOn(start, LocalDate.of(2030, 3, 15)));
        assertFalse(rule.occursOn(start, LocalDate.of(2027, 3, 16)));
        assertFalse(rule.occursOn(start, LocalDate.of(2027, 4, 15)));
    }

    @Test
    void testYearly_EveryOtherYear() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        RecurrenceRule rule = RecurrenceRule.yearly(2);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 6, 1)));
        assertFalse(rule.occursOn(start, LocalDate.of(2027, 6, 1)));   // skip
        assertTrue(rule.occursOn(start, LocalDate.of(2028, 6, 1)));
    }

    @Test
    void testYearly_Feb29OnlyOnLeapYears() {
        LocalDate start = LocalDate.of(2028, 2, 29); // leap year
        RecurrenceRule rule = RecurrenceRule.yearly();

        assertTrue(rule.occursOn(start, LocalDate.of(2028, 2, 29)));
        // 2029 has no Feb 29, so the event simply doesn't occur
        assertFalse(rule.occursOn(start, LocalDate.of(2029, 2, 28)));
        // Next leap year
        assertTrue(rule.occursOn(start, LocalDate.of(2032, 2, 29)));
    }

    // ── End conditions ─────────────────────────────────────────────

    @Test
    void testUntil() {
        RecurrenceRule rule = RecurrenceRule.daily().until(LocalDate.of(2026, 2, 5));
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 5)));    // inclusive
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 6)));   // past end
    }

    @Test
    void testCount_Daily() {
        RecurrenceRule rule = RecurrenceRule.daily().times(3);
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 1)));    // occurrence 1
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 2)));    // occurrence 2
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 3)));    // occurrence 3
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 4)));   // past count
    }

    @Test
    void testCount_Weekly() {
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.MONDAY).times(4);
        LocalDate start = LocalDate.of(2026, 2, 16); // Monday

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 16)));   // 1
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 23)));   // 2
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 2)));    // 3
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 3, 9)));    // 4
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 3, 16))); // 5 (past count)
    }

    @Test
    void testCount_WeeklyMultiDay() {
        // 4 occurrences across Tue/Thu = 2 weeks of Tue+Thu
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY).times(4);
        LocalDate start = LocalDate.of(2026, 2, 16); // Monday (week start)

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 17)));   // Tue week 1 → occ 1
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 19)));   // Thu week 1 → occ 2
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 24)));   // Tue week 2 → occ 3
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 26)));   // Thu week 2 → occ 4
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 3, 3)));   // Tue week 3 → occ 5 (past)
    }

    // ── Excluded dates ─────────────────────────────────────────────

    @Test
    void testExcludedDates() {
        RecurrenceRule rule = RecurrenceRule.daily()
                .excluding(LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 5));
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 2)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 3)));   // excluded
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 4)));
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 5)));   // excluded
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 6)));
    }

    @Test
    void testExcludedDates_WithCount() {
        // 3 occurrences of daily, excluding day 2 — should extend to day 4
        RecurrenceRule rule = RecurrenceRule.daily().times(3)
                .excluding(LocalDate.of(2026, 2, 2));
        LocalDate start = LocalDate.of(2026, 2, 1);

        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 1)));    // occ 1
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 2)));   // excluded
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 3)));    // occ 2
        assertTrue(rule.occursOn(start, LocalDate.of(2026, 2, 4)));    // occ 3
        assertFalse(rule.occursOn(start, LocalDate.of(2026, 2, 5)));   // past count
    }

    // ── Factory methods and fluent API ─────────────────────────────

    @Test
    void testFactoryMethods_SetCorrectFrequency() {
        assertEquals(RecurrenceRule.Frequency.DAILY, RecurrenceRule.daily().getFrequency());
        assertEquals(RecurrenceRule.Frequency.WEEKLY, RecurrenceRule.weekly().getFrequency());
        assertEquals(RecurrenceRule.Frequency.MONTHLY, RecurrenceRule.monthlyOnDay().getFrequency());
        assertEquals(RecurrenceRule.Frequency.MONTHLY, RecurrenceRule.monthlyOnLastDay().getFrequency());
        assertEquals(RecurrenceRule.Frequency.MONTHLY, RecurrenceRule.monthlyOnNthWeekday().getFrequency());
        assertEquals(RecurrenceRule.Frequency.YEARLY, RecurrenceRule.yearly().getFrequency());
    }

    @Test
    void testFactoryMethods_DefaultInterval() {
        assertEquals(1, RecurrenceRule.daily().getInterval());
        assertEquals(1, RecurrenceRule.weekly().getInterval());
        assertEquals(1, RecurrenceRule.monthlyOnDay().getInterval());
        assertEquals(1, RecurrenceRule.yearly().getInterval());
    }

    @Test
    void testFactoryMethods_CustomInterval() {
        assertEquals(3, RecurrenceRule.daily(3).getInterval());
        assertEquals(2, RecurrenceRule.weekly(2).getInterval());
        assertEquals(4, RecurrenceRule.monthlyOnDay(4).getInterval());
        assertEquals(5, RecurrenceRule.yearly(5).getInterval());
    }

    @Test
    void testFluentUntil() {
        RecurrenceRule rule = RecurrenceRule.daily().until(LocalDate.of(2026, 12, 31));
        assertEquals(RecurrenceRule.EndType.UNTIL, rule.getEndType());
        assertEquals(LocalDate.of(2026, 12, 31), rule.getEndDate());
    }

    @Test
    void testFluentTimes() {
        RecurrenceRule rule = RecurrenceRule.weekly().times(10);
        assertEquals(RecurrenceRule.EndType.COUNT, rule.getEndType());
        assertEquals(10, rule.getOccurrenceCount());
    }

    @Test
    void testFluentExcluding() {
        RecurrenceRule rule = RecurrenceRule.daily()
                .excluding(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 25));
        assertNotNull(rule.getExcludedDates());
        assertEquals(2, rule.getExcludedDates().size());
    }

    // ── Equals, hashCode, toString ─────────────────────────────────

    @Test
    void testEquals() {
        RecurrenceRule a = RecurrenceRule.daily(2).until(LocalDate.of(2026, 12, 31));
        RecurrenceRule b = RecurrenceRule.daily(2).until(LocalDate.of(2026, 12, 31));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testNotEquals_DifferentFrequency() {
        assertNotEquals(RecurrenceRule.daily(), RecurrenceRule.weekly());
    }

    @Test
    void testToString_ContainsFrequency() {
        String str = RecurrenceRule.daily(2).until(LocalDate.of(2026, 6, 1)).toString();
        assertTrue(str.contains("DAILY"));
        assertTrue(str.contains("every 2"));
        assertTrue(str.contains("until"));
    }
}
