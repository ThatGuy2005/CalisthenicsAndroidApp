package com.example.calisthenicsapp.Scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

public class RecurrenceTest {
    private static final TimeZone BUDAPEST = TimeZone.getTimeZone("Europe/Budapest");

    /** Epoch seconds of a local date and time in Budapest. */
    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(BUDAPEST);
        c.clear();
        c.set(year, month - 1, day, hour, minute);
        return c.getTimeInMillis() / 1000L;
    }

    @Test
    public void parsesAndWritesRules() {
        assertEquals(Recurrence.weekly(), Recurrence.parse("FREQ=WEEKLY"));
        assertEquals(new Recurrence(Recurrence.Frequency.DAILY, 2), Recurrence.parse("freq=daily; interval=2"));
        assertEquals("FREQ=MONTHLY", Recurrence.monthly().toRule());
        assertEquals("FREQ=DAILY;INTERVAL=3", new Recurrence(Recurrence.Frequency.DAILY, 3).toRule());
        assertNull(Recurrence.parse(null));
        assertNull(Recurrence.parse("  "));
    }

    @Test
    public void describesItselfForPeople() {
        assertEquals("Every week", Recurrence.weekly().describe());
        assertEquals("Every 2 days", new Recurrence(Recurrence.Frequency.DAILY, 2).describe());
        assertEquals("Every 3 months", new Recurrence(Recurrence.Frequency.MONTHLY, 3).describe());
    }

    @Test
    public void rejectsRulesItCannotRepeatBy() {
        for (String rule : new String[]{"FREQ=YEARLY", "FREQ=WEEKLY;BYDAY=MO", "INTERVAL=2",
                "FREQ=DAILY;INTERVAL=0", "FREQ=DAILY;INTERVAL=x", "WEEKLY"}) {
            try {
                Recurrence.parse(rule);
                fail("Accepted " + rule);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    @Test
    public void weeklyKeepsTheLocalTimeAcrossDaylightSavingEnd() {
        // Summer time ends in Hungary on Sunday 2026-10-25.
        long start = at(2026, 10, 19, 18, 0);
        assertEquals(Arrays.asList(start, at(2026, 10, 26, 18, 0), at(2026, 11, 2, 18, 0)),
                Recurrence.weekly().occurrences(start, start, at(2026, 11, 3, 0, 0), BUDAPEST));
        assertEquals(7 * 86_400 + 3_600, at(2026, 10, 26, 18, 0) - start);
    }

    @Test
    public void onlyReturnsOccurrencesInsideTheWindow() {
        long start = at(2026, 10, 1, 7, 30);
        Recurrence everyOtherDay = new Recurrence(Recurrence.Frequency.DAILY, 2);
        assertEquals(Arrays.asList(at(2026, 10, 5, 7, 30), at(2026, 10, 7, 7, 30)),
                everyOtherDay.occurrences(start, at(2026, 10, 4, 0, 0), at(2026, 10, 7, 7, 30) + 1,
                        BUDAPEST));
        // Nothing before the first occurrence, and the end of the window is exclusive.
        assertEquals(Collections.singletonList(start),
                everyOtherDay.occurrences(start, at(2026, 9, 1, 0, 0), at(2026, 10, 3, 7, 30),
                        BUDAPEST));
    }

    @Test
    public void monthlyOnThe31stSkipsShortMonths() {
        long start = at(2026, 1, 31, 18, 0);
        assertEquals(Arrays.asList(start, at(2026, 3, 31, 18, 0), at(2026, 5, 31, 18, 0)),
                Recurrence.monthly().occurrences(start, start, at(2026, 6, 1, 0, 0), BUDAPEST));
    }

    @Test
    public void findsOccurrencesLongAfterTheStart() {
        long start = at(2020, 1, 6, 18, 0);    // a Monday
        assertEquals(Collections.singletonList(at(2026, 10, 12, 18, 0)),
                Recurrence.weekly().occurrences(start, at(2026, 10, 10, 0, 0),
                        at(2026, 10, 17, 0, 0), BUDAPEST));
        assertEquals(at(2026, 10, 19, 18, 0),
                Recurrence.weekly().next(start, at(2026, 10, 12, 18, 0) + 1, BUDAPEST));
    }

    @Test
    public void nextIsTheStartWhenNothingHasHappenedYet() {
        long start = at(2026, 10, 20, 18, 0);
        assertEquals(start, Recurrence.daily().next(start, at(2026, 10, 1, 0, 0), BUDAPEST));
        assertEquals(start, Recurrence.daily().next(start, start, BUDAPEST));
    }
}
