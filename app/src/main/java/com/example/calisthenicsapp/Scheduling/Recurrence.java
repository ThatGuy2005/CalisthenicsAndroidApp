package com.example.calisthenicsapp.Scheduling;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * How a scheduled workout repeats (FR-6.1): every N days, weeks or months. Stored as an RFC 5545
 * RRULE ("FREQ=WEEKLY", "FREQ=DAILY;INTERVAL=2"), the same text the calendar event and the .ics
 * file use, so all three repeat the same way. A schedule without a rule happens once.
 *
 * Occurrences keep the local wall-clock time of the first one (18:00 stays 18:00 across daylight
 * saving changes). A monthly rule that starts on the 29th-31st skips months without that day,
 * like calendar apps do.
 */
public final class Recurrence {
    public enum Frequency { DAILY, WEEKLY, MONTHLY }

    public static final int MAX_INTERVAL = 99;

    private final Frequency frequency;
    private final int interval;

    public Recurrence(Frequency frequency, int interval) {
        if (frequency == null) throw new IllegalArgumentException("A repeat needs a frequency.");
        if (interval < 1 || interval > MAX_INTERVAL) {
            throw new IllegalArgumentException("Repeat every 1-" + MAX_INTERVAL + " days, weeks or months.");
        }
        this.frequency = frequency;
        this.interval = interval;
    }

    public static Recurrence daily() { return new Recurrence(Frequency.DAILY, 1); }
    public static Recurrence weekly() { return new Recurrence(Frequency.WEEKLY, 1); }
    public static Recurrence monthly() { return new Recurrence(Frequency.MONTHLY, 1); }

    /**
     * Reads a rule written by {@link #toRule()}. Null or blank means "does not repeat" and gives
     * null. Throws IllegalArgumentException for anything else this class cannot repeat by.
     */
    public static Recurrence parse(String rule) {
        if (rule == null || rule.trim().isEmpty()) return null;
        Frequency frequency = null;
        int interval = 1;
        for (String part : rule.trim().toUpperCase(Locale.ROOT).split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("Unsupported repeat rule: " + rule);
            String name = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            if (name.equals("FREQ")) {
                try {
                    frequency = Frequency.valueOf(value);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unsupported repeat rule: " + rule);
                }
            } else if (name.equals("INTERVAL")) {
                try {
                    interval = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported repeat rule: " + rule);
                }
            } else {
                throw new IllegalArgumentException("Unsupported repeat rule: " + rule);
            }
        }
        if (frequency == null) throw new IllegalArgumentException("Unsupported repeat rule: " + rule);
        return new Recurrence(frequency, interval);
    }

    public Frequency getFrequency() { return frequency; }
    public int getInterval() { return interval; }

    /** For people: "Every week", "Every 2 days". */
    public String describe() {
        String unit = frequency == Frequency.DAILY ? "day"
                : frequency == Frequency.WEEKLY ? "week" : "month";
        return interval == 1 ? "Every " + unit : "Every " + interval + " " + unit + "s";
    }

    /** The RRULE value, e.g. "FREQ=WEEKLY" or "FREQ=DAILY;INTERVAL=2". */
    public String toRule() {
        return "FREQ=" + frequency.name() + (interval == 1 ? "" : ";INTERVAL=" + interval);
    }

    /**
     * Start times (epoch seconds) of the occurrences in [fromSeconds, toSeconds), soonest
     * first. The first occurrence is startSeconds itself.
     */
    public List<Long> occurrences(long startSeconds, long fromSeconds, long toSeconds, TimeZone zone) {
        List<Long> result = new ArrayList<>();
        Calendar start = startCalendar(startSeconds, zone);
        for (int n = firstCandidate(start, startSeconds, fromSeconds, zone); ; n++) {
            Long at = occurrence(start, n);
            if (at == null) continue;                     // a month without that day
            if (at >= toSeconds) return result;
            if (at >= fromSeconds) result.add(at);
        }
    }

    /** The first occurrence at or after afterSeconds. */
    public long next(long startSeconds, long afterSeconds, TimeZone zone) {
        Calendar start = startCalendar(startSeconds, zone);
        for (int n = firstCandidate(start, startSeconds, afterSeconds, zone); ; n++) {
            Long at = occurrence(start, n);
            if (at != null && at >= afterSeconds) return at;
        }
    }

    /**
     * The n-th occurrence, counted from the start each time so months never drift
     * (Jan 31 + 2 months is Mar 31, not Mar 28). Null if that month has no such day.
     */
    private Long occurrence(Calendar start, int n) {
        Calendar c = (Calendar) start.clone();
        switch (frequency) {
            case DAILY:
                c.add(Calendar.DAY_OF_MONTH, n * interval);
                break;
            case WEEKLY:
                c.add(Calendar.DAY_OF_MONTH, n * interval * 7);
                break;
            default:
                c.add(Calendar.MONTH, n * interval);
                if (c.get(Calendar.DAY_OF_MONTH) != start.get(Calendar.DAY_OF_MONTH)) return null;
        }
        return c.getTimeInMillis() / 1000L;
    }

    /**
     * An n a little before the first occurrence at or after fromSeconds, so a schedule that
     * started long ago is not walked from its first occurrence. One period of margin covers
     * daylight saving shifts.
     */
    private int firstCandidate(Calendar start, long startSeconds, long fromSeconds, TimeZone zone) {
        if (fromSeconds <= startSeconds) return 0;
        long periods;
        if (frequency == Frequency.MONTHLY) {
            Calendar from = startCalendar(fromSeconds, zone);
            int months = (from.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12
                    + from.get(Calendar.MONTH) - start.get(Calendar.MONTH);
            periods = months / interval;
        } else {
            long periodSeconds = 86_400L * interval * (frequency == Frequency.WEEKLY ? 7 : 1);
            periods = (fromSeconds - startSeconds) / periodSeconds;
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE / 1000, periods - 1));
    }

    private static Calendar startCalendar(long epochSeconds, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone, Locale.ROOT);
        c.setTimeInMillis(epochSeconds * 1000L);
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Recurrence)) return false;
        Recurrence other = (Recurrence) o;
        return frequency == other.frequency && interval == other.interval;
    }

    @Override
    public int hashCode() {
        return frequency.hashCode() * 31 + interval;
    }

    @Override
    public String toString() {
        return toRule();
    }
}
