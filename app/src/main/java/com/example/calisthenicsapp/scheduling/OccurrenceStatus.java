package com.example.calisthenicsapp.scheduling;

/**
 * The status of one occurrence of a scheduled workout (FR-6.1). Only COMPLETED is stored
 * (the schedule's list of completed occurrence times); MISSED and PENDING follow from the time,
 * so nothing has to run in the background to flip an occurrence to missed.
 */
public enum OccurrenceStatus {
    /** Still to come, or started less than MISSED_AFTER_SECONDS ago. */
    PENDING,
    /** Marked as done, by hand or by finishing the workout. */
    COMPLETED,
    /** Not done within MISSED_AFTER_SECONDS of its start. */
    MISSED;

    /** An occurrence not completed this long after its start counts as missed. */
    public static final long MISSED_AFTER_SECONDS = 12 * 3600L;

    /**
     * COMPLETED if it was marked done, MISSED once {@link #MISSED_AFTER_SECONDS} have passed since
     * its start without that, otherwise PENDING.
     */
    public static OccurrenceStatus of(boolean completed, long occurrenceSeconds, long nowSeconds) {
        if (completed) return COMPLETED;
        return nowSeconds >= occurrenceSeconds + MISSED_AFTER_SECONDS ? MISSED : PENDING;
    }
}
