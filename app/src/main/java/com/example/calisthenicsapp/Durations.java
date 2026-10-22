package com.example.calisthenicsapp;

import android.content.Context;

import androidx.annotation.StringRes;

/** Lengths of time as the screens show them. */
final class Durations {

    private Durations() {
    }

    /**
     * How long a workout is expected to take: "25 min", rounded up and at least 1 min.
     * Without an estimate (null or 0) it is the text of unknown, e.g. "Time varies".
     */
    static String estimate(Context context, Integer seconds, @StringRes int unknown) {
        if (seconds == null || seconds <= 0) return context.getString(unknown);
        return context.getString(R.string.duration_minutes, Math.max(1, (seconds + 59) / 60));
    }

    /** Time spent training: "0 min", "<1 min", "42 min", "1 h" or "1 h 5 min". */
    static String spent(Context context, long seconds) {
        if (seconds > 0 && seconds < 60) return context.getString(R.string.duration_under_minute);
        long minutes = Math.max(0, seconds) / 60;
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours == 0) return context.getString(R.string.duration_minutes, minutes);
        if (remainingMinutes == 0) return context.getString(R.string.duration_hours, hours);
        return context.getString(R.string.duration_hours_minutes, hours, remainingMinutes);
    }
}
