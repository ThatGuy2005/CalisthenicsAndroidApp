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
}
