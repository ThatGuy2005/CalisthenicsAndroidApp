package com.example.calisthenicsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.Scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.Scheduling.ScheduleOccurrence;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleWeekTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long NOW = 1792080000L;          // Thu 2026-10-15 16:00:00 UTC
    private static final long THURSDAY = 1792022400L;     // Thu 2026-10-15 00:00:00 UTC
    private static final long DAY = 86_400L;

    /** Weeks start on Monday in the UK and on Sunday in the US. */
    private static ScheduleWeek week(Locale locale) {
        return new ScheduleWeek(Calendar.getInstance(UTC, locale), () -> NOW * 1000L);
    }

    private static ScheduleOccurrence at(long seconds) {
        ScheduledWorkout schedule = new ScheduledWorkout("s", "u", "Legs", null, seconds,
                null, null, null);
        return new ScheduleOccurrence(schedule, seconds, OccurrenceStatus.PENDING);
    }

    @Test
    public void startsOnTodayAtMidnight() {
        ScheduleWeek week = week(Locale.UK);
        assertEquals(THURSDAY * 1000L, week.getSelectedDay().getTimeInMillis());
        assertTrue(week.isTodaySelected());
    }

    @Test
    public void theWeekStartsOnTheLocalesFirstDay() {
        ScheduleWeek monday = week(Locale.UK);
        assertEquals(THURSDAY - 3 * DAY, monday.fromSeconds());
        assertEquals(THURSDAY + 4 * DAY, monday.toSeconds());

        ScheduleWeek sunday = week(Locale.US);
        assertEquals(THURSDAY - 4 * DAY, sunday.fromSeconds());
        assertEquals(THURSDAY + 3 * DAY, sunday.toSeconds());
    }

    @Test
    public void listsTheSevenDaysWithTheSelectedOne() {
        List<Calendar> days = week(Locale.UK).days();
        assertEquals(7, days.size());
        assertEquals(Calendar.MONDAY, days.get(0).get(Calendar.DAY_OF_WEEK));
        assertEquals(12, days.get(0).get(Calendar.DAY_OF_MONTH));
        assertEquals(18, days.get(6).get(Calendar.DAY_OF_MONTH));
        assertTrue(week(Locale.UK).isSelected(days.get(3)));
        assertFalse(week(Locale.UK).isSelected(days.get(4)));
    }

    @Test
    public void selectingAMomentSelectsItsDay() {
        ScheduleWeek week = week(Locale.UK);
        week.select((THURSDAY + 2 * DAY + 23 * 3600) * 1000L);   // Saturday 23:00
        assertEquals((THURSDAY + 2 * DAY) * 1000L, week.getSelectedDay().getTimeInMillis());
        assertFalse(week.isTodaySelected());
        week.selectToday();
        assertTrue(week.isTodaySelected());
    }

    @Test
    public void movesByWholeWeeks() {
        ScheduleWeek week = week(Locale.UK);
        week.moveWeeks(1);
        assertEquals(THURSDAY + 7 * DAY, week.getSelectedDay().getTimeInMillis() / 1000L);
        assertEquals(THURSDAY + 4 * DAY, week.fromSeconds());
        week.moveWeeks(-2);
        assertEquals(THURSDAY - 7 * DAY, week.getSelectedDay().getTimeInMillis() / 1000L);
    }

    @Test
    public void findsTheWorkoutsOfADay() {
        ScheduleWeek week = week(Locale.UK);
        week.setOccurrences(Arrays.asList(at(NOW), at(THURSDAY + 2 * DAY + 3600)));
        List<Calendar> days = week.days();

        assertTrue(week.hasOccurrenceOn(days.get(3)));    // Thursday
        assertFalse(week.hasOccurrenceOn(days.get(4)));   // Friday
        assertTrue(week.hasOccurrenceOn(days.get(5)));    // Saturday
        assertEquals(1, week.occurrencesOnSelectedDay().size());
        assertEquals(NOW, week.occurrencesOnSelectedDay().get(0).getStartSeconds());

        week.setOccurrences(Arrays.asList());
        assertFalse(week.hasOccurrenceOn(days.get(3)));
    }
}
