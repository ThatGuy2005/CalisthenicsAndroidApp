package com.example.calisthenicsapp.CoreComponents;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class StatsPeriodTest {
    private static final TimeZone BUDAPEST = TimeZone.getTimeZone("Europe/Budapest");
    private static final Locale HUNGARY = Locale.forLanguageTag("hu-HU");   // weeks start on Monday

    /** Epoch millis of a local date and time in the given zone. */
    private static long at(TimeZone zone, int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(zone);
        c.clear();
        c.set(year, month - 1, day, hour, minute);
        return c.getTimeInMillis();
    }

    @Test
    public void allTimeStartsAtZero() {
        assertEquals(0L, StatsPeriod.ALL_TIME.startSeconds(at(BUDAPEST, 2026, 10, 15, 12, 0),
                BUDAPEST, HUNGARY));
    }

    @Test
    public void weekStartsOnMondayMidnightInHungary() {
        // Thursday 15 October 2026 -> Monday 12 October 2026, 00:00 local time.
        long now = at(BUDAPEST, 2026, 10, 15, 18, 30);
        assertEquals(at(BUDAPEST, 2026, 10, 12, 0, 0) / 1000L,
                StatsPeriod.WEEK.startSeconds(now, BUDAPEST, HUNGARY));
    }

    @Test
    public void weekStartsOnSundayInTheUs() {
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        long now = at(newYork, 2026, 10, 15, 9, 0);                    // Thursday
        assertEquals(at(newYork, 2026, 10, 11, 0, 0) / 1000L,
                StatsPeriod.WEEK.startSeconds(now, newYork, Locale.US));
    }

    @Test
    public void weekCanStartInThePreviousMonthAndYear() {
        // Friday 1 January 2027 -> Monday 28 December 2026.
        long now = at(BUDAPEST, 2027, 1, 1, 10, 0);
        assertEquals(at(BUDAPEST, 2026, 12, 28, 0, 0) / 1000L,
                StatsPeriod.WEEK.startSeconds(now, BUDAPEST, HUNGARY));
    }

    @Test
    public void mondayItselfIsTheStartOfItsWeek() {
        long now = at(BUDAPEST, 2026, 10, 12, 0, 5);
        assertEquals(at(BUDAPEST, 2026, 10, 12, 0, 0) / 1000L,
                StatsPeriod.WEEK.startSeconds(now, BUDAPEST, HUNGARY));
    }

    @Test
    public void monthStartsOnTheFirstAtMidnight() {
        // 25 October 2026 is the day summer time ends in Budapest; the month still starts at 00:00.
        long now = at(BUDAPEST, 2026, 10, 25, 23, 0);
        assertEquals(at(BUDAPEST, 2026, 10, 1, 0, 0) / 1000L,
                StatsPeriod.MONTH.startSeconds(now, BUDAPEST, HUNGARY));
    }
}
