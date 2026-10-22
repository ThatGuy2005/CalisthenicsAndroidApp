package com.example.calisthenicsapp.CoreComponents;

import com.example.calisthenicsapp.Scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.Scheduling.Recurrence;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

public class ScheduledWorkout {
    public ScheduledWorkout(String id, String uid, String workoutName,
                            String customWorkoutId, long scheduledAt) {
        this(id, uid, workoutName, customWorkoutId, scheduledAt, null, null, null);
    }

    public ScheduledWorkout(String id, String uid, String workoutName,
                            String customWorkoutId, long scheduledAt, String recurrenceRule,
                            Collection<Long> completedOccurrences, Long calendarEventId) {
        this.id = id;
        this.uid = uid;
        this.workoutName = workoutName;
        this.customWorkoutId = customWorkoutId;
        this.scheduledAt = scheduledAt;
        this.recurrenceRule = recurrenceRule;
        this.completedOccurrences = completedOccurrences == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new TreeSet<>(completedOccurrences));
        this.calendarEventId = calendarEventId;
    }

    public String getId() {
        return this.id;
    }

    public String getUid() {
        return this.uid;
    }

    public String getWorkoutName() {
        return this.workoutName;
    }

    public String getCustomWorkoutId() {
        return this.customWorkoutId;
    }

    /** The first (for a one-off schedule, the only) occurrence. */
    public long getScheduledAt() {
        return this.scheduledAt;
    }

    public boolean isCustom() {
        return this.customWorkoutId != null;
    }

    /** The RRULE text, e.g. "FREQ=WEEKLY", or null if it happens once (FR-6.1). */
    public String getRecurrenceRule() {
        return this.recurrenceRule;
    }

    /** Null if it happens once. A rule this app cannot read also counts as once. */
    public Recurrence getRecurrence() {
        try {
            return Recurrence.parse(this.recurrenceRule);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isRecurring() {
        return getRecurrence() != null;
    }

    /** The first occurrence at or after afterSeconds, or null if there is none. */
    public Long nextOccurrence(long afterSeconds, TimeZone zone) {
        Recurrence recurrence = getRecurrence();
        if (recurrence != null) return recurrence.next(this.scheduledAt, afterSeconds, zone);
        return this.scheduledAt >= afterSeconds ? this.scheduledAt : null;
    }

    /** Start times of the occurrences in [fromSeconds, toSeconds), soonest first. */
    public List<Long> occurrences(long fromSeconds, long toSeconds, TimeZone zone) {
        Recurrence recurrence = getRecurrence();
        if (recurrence != null) return recurrence.occurrences(this.scheduledAt, fromSeconds, toSeconds, zone);
        boolean inside = this.scheduledAt >= fromSeconds && this.scheduledAt < toSeconds;
        return inside ? Collections.singletonList(this.scheduledAt) : Collections.emptyList();
    }

    /** Whether the schedule really falls on this time. */
    public boolean isOccurrence(long startSeconds, TimeZone zone) {
        return !occurrences(startSeconds, startSeconds + 1, zone).isEmpty();
    }

    /** Start times of the occurrences marked as done, ascending. */
    public Set<Long> getCompletedOccurrences() {
        return this.completedOccurrences;
    }

    public OccurrenceStatus statusOf(long occurrenceSeconds, long nowSeconds) {
        return OccurrenceStatus.of(this.completedOccurrences.contains(occurrenceSeconds),
                occurrenceSeconds, nowSeconds);
    }

    /** The event in this device's calendar, or null if it was not added here (FR-6.2). */
    public Long getCalendarEventId() {
        return this.calendarEventId;
    }

    private String id;               // null until inserted
    private String uid;
    private String workoutName;      // premade workout, or null
    private String customWorkoutId;  // custom workout, or null
    private long scheduledAt;
    private String recurrenceRule;   // null = once
    private Set<Long> completedOccurrences;   // occurrence start times, epoch seconds
    private Long calendarEventId;    // local to this device, see the CalendarEvent table
}
