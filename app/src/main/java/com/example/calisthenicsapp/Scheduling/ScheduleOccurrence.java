package com.example.calisthenicsapp.Scheduling;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * One date and time a scheduled workout falls on, with its status (FR-6.1). A one-off schedule
 * has one occurrence; a repeating one has one per repeat. This is what an agenda or calendar
 * screen lists.
 */
public final class ScheduleOccurrence {
    private final ScheduledWorkout schedule;
    private final long startSeconds;
    private final OccurrenceStatus status;

    public ScheduleOccurrence(ScheduledWorkout schedule, long startSeconds, OccurrenceStatus status) {
        this.schedule = schedule;
        this.startSeconds = startSeconds;
        this.status = status;
    }

    public ScheduledWorkout getSchedule() { return schedule; }
    /** Pass this to ScheduleService.markCompleted to complete this occurrence. */
    public long getStartSeconds() { return startSeconds; }
    public OccurrenceStatus getStatus() { return status; }

    /** The occurrences of the schedules in [fromSeconds, toSeconds), soonest first. */
    public static List<ScheduleOccurrence> between(List<ScheduledWorkout> schedules, long fromSeconds,
                                                   long toSeconds, long nowSeconds, TimeZone zone) {
        List<ScheduleOccurrence> result = new ArrayList<>();
        for (ScheduledWorkout schedule : schedules) {
            for (long start : schedule.occurrences(fromSeconds, toSeconds, zone)) {
                result.add(new ScheduleOccurrence(schedule, start,
                        schedule.statusOf(start, nowSeconds)));
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(a.startSeconds, b.startSeconds));
        return result;
    }

    /**
     * The not yet completed occurrence a workout done at startedSeconds counts for, or null.
     * It must be the same workout (a premade one by name, a custom one by id) and start within
     * {@link OccurrenceStatus#MISSED_AFTER_SECONDS} of the workout, the same window in which it
     * can be marked completed and before it counts as missed. The closest one wins.
     *
     * @param customWorkoutId the custom workout that was done, or null for a premade one
     */
    public static ScheduleOccurrence matching(List<ScheduledWorkout> schedules, String workoutName,
                                              String customWorkoutId, long startedSeconds,
                                              TimeZone zone) {
        long window = OccurrenceStatus.MISSED_AFTER_SECONDS;
        ScheduleOccurrence best = null;
        for (ScheduledWorkout schedule : schedules) {
            boolean sameWorkout = customWorkoutId != null
                    ? customWorkoutId.equals(schedule.getCustomWorkoutId())
                    : !schedule.isCustom() && schedule.getWorkoutName().equals(workoutName);
            if (!sameWorkout) continue;
            // startedSeconds in [start - window, start + window)  <=>  start in (started - window, started + window]
            for (long start : schedule.occurrences(startedSeconds - window + 1,
                    startedSeconds + window + 1, zone)) {
                if (schedule.getCompletedOccurrences().contains(start)) continue;
                if (best == null || Math.abs(start - startedSeconds) < Math.abs(best.startSeconds - startedSeconds)) {
                    best = new ScheduleOccurrence(schedule, start, OccurrenceStatus.PENDING);
                }
            }
        }
        return best;
    }
}
