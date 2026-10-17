package com.example.calisthenicsapp.CoreComponents;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The periods the history statistics can cover (FR-5.7). WEEK and MONTH are calendar periods
 * in the device's time zone ("this week", "this month"), not the last 7 or 30 days. The week
 * starts on the locale's first day of the week (Monday in most of Europe, Sunday in the US).
 */
public enum StatsPeriod {
    WEEK, MONTH, ALL_TIME;

    /** Start of the period containing nowMillis, in epoch seconds. 0 for ALL_TIME. */
    public long startSeconds(long nowMillis, TimeZone zone, Locale locale) {
        if (this == ALL_TIME) return 0L;
        Calendar c = Calendar.getInstance(zone, locale);
        c.setTimeInMillis(nowMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (this == WEEK) {
            // Step back day by day: setting DAY_OF_WEEK directly is unreliable across week edges.
            while (c.get(Calendar.DAY_OF_WEEK) != c.getFirstDayOfWeek()) {
                c.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else {
            c.set(Calendar.DAY_OF_MONTH, 1);
        }
        return c.getTimeInMillis() / 1000L;
    }

    /** Start of the period containing the current time, on this device. */
    public long startSeconds() {
        return startSeconds(System.currentTimeMillis(), TimeZone.getDefault(), Locale.getDefault());
    }
}
