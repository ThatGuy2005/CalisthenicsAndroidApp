package com.example.calisthenicsapp;

import com.example.calisthenicsapp.Scheduling.ScheduleOccurrence;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * The week the Schedule screen shows: the selected day, the seven days of its week and the
 * workouts planned in them. Days start at midnight in the calendar's time zone, and weeks on its
 * locale's first day of the week (Monday or Sunday).
 *
 * Plain Java with no views, so the date math can be unit-tested.
 */
final class ScheduleWeek {
    private static final int DAYS_PER_WEEK = 7;

    private final Calendar selectedDay;
    private final LongSupplier clock;
    private final List<ScheduleOccurrence> occurrences = new ArrayList<>();

    /** Starts on today, in the device's time zone and locale. */
    ScheduleWeek() {
        this(Calendar.getInstance(), System::currentTimeMillis);
    }

    /** calendar sets the time zone and the first day of the week; clock returns epoch millis. */
    ScheduleWeek(Calendar calendar, LongSupplier clock) {
        this.selectedDay = (Calendar) calendar.clone();
        this.clock = clock;
        selectToday();
    }

    /** A copy, at midnight. */
    Calendar getSelectedDay() {
        return (Calendar) selectedDay.clone();
    }

    void selectToday() {
        select(clock.getAsLong());
    }

    /** Selects the day this moment falls on. */
    void select(long epochMillis) {
        selectedDay.setTimeInMillis(epochMillis);
        startOfDay(selectedDay);
    }

    /** The same weekday, weeks later (negative: earlier). */
    void moveWeeks(int weeks) {
        selectedDay.add(Calendar.DAY_OF_MONTH, weeks * DAYS_PER_WEEK);
    }

    boolean isSelected(Calendar day) {
        return sameDay(day, selectedDay);
    }

    boolean isTodaySelected() {
        return isSelected(dayOf(clock.getAsLong()));
    }

    /** The seven days of the selected week, each at midnight. */
    List<Calendar> days() {
        List<Calendar> days = new ArrayList<>(DAYS_PER_WEEK);
        Calendar first = weekStart();
        for (int i = 0; i < DAYS_PER_WEEK; i++) {
            Calendar day = (Calendar) first.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            days.add(day);
        }
        return days;
    }

    /** Start of the week, in epoch seconds, for ScheduleService.getOccurrences. */
    long fromSeconds() {
        return weekStart().getTimeInMillis() / 1000L;
    }

    /** Start of the next week, in epoch seconds (exclusive end). */
    long toSeconds() {
        Calendar end = weekStart();
        end.add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK);
        return end.getTimeInMillis() / 1000L;
    }

    /** The workouts planned this week, as loaded from ScheduleService. */
    void setOccurrences(List<ScheduleOccurrence> weekOccurrences) {
        occurrences.clear();
        occurrences.addAll(weekOccurrences);
    }

    boolean hasOccurrenceOn(Calendar day) {
        for (ScheduleOccurrence occurrence : occurrences) {
            if (startsOn(occurrence, day)) return true;
        }
        return false;
    }

    List<ScheduleOccurrence> occurrencesOnSelectedDay() {
        List<ScheduleOccurrence> onDay = new ArrayList<>();
        for (ScheduleOccurrence occurrence : occurrences) {
            if (startsOn(occurrence, selectedDay)) onDay.add(occurrence);
        }
        return onDay;
    }

    private Calendar weekStart() {
        Calendar start = (Calendar) selectedDay.clone();
        int difference = (start.get(Calendar.DAY_OF_WEEK) - start.getFirstDayOfWeek() + DAYS_PER_WEEK)
                % DAYS_PER_WEEK;
        start.add(Calendar.DAY_OF_MONTH, -difference);
        return start;
    }

    private boolean startsOn(ScheduleOccurrence occurrence, Calendar day) {
        return sameDay(dayOf(occurrence.getStartSeconds() * 1000L), day);
    }

    /** The day of a moment, in the same time zone and locale as the selected day. */
    private Calendar dayOf(long epochMillis) {
        Calendar day = (Calendar) selectedDay.clone();
        day.setTimeInMillis(epochMillis);
        return startOfDay(day);
    }

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private static Calendar startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
