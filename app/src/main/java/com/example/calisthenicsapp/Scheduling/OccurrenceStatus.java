package com.example.calisthenicsapp.Scheduling;

/**
 * The status of one occurrence of a scheduled workout (FR-6.1). Only COMPLETED is stored
 * (the schedule's list of completed occurrence times); MISSED and PENDING follow from the time,
 * so nothing has to run in the background to flip an occurrence to missed.
 */
public enum OccurrenceStatus {
    PENDING, COMPLETED, MISSED;

    /** An occurrence not completed this long after its start counts as missed. */
    public static final long MISSED_AFTER_SECONDS = 12 * 3600L;

    public static OccurrenceStatus of(boolean completed, long occurrenceSeconds, long nowSeconds) {
        if (completed) return COMPLETED;
        return nowSeconds >= occurrenceSeconds + MISSED_AFTER_SECONDS ? MISSED : PENDING;
    }
}
