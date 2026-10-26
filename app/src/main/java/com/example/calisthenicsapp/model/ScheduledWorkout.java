package com.example.calisthenicsapp.model;

import com.example.calisthenicsapp.scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.scheduling.Recurrence;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

/**
 * A planned workout (FR-6.1), a document of users/{uid}/schedules mirrored in the Schedule table.
 * It points at one premade workout by name or one custom workout by id, never both. It happens once
 * at {@link #getScheduledAt()}, or repeats from then on by its {@link Recurrence}. All times are
 * epoch seconds.
 *
 * Occurrences are not stored, they are worked out from the rule; only the ones the user marked as
 * done are kept ({@link #getCompletedOccurrences()}).
 */
public class ScheduledWorkout {
    /**
     * A one-off schedule with nothing completed and no calendar event. Pass a premade workout's
     * name or a custom workout's id, and null for the other.
     */
    public ScheduledWorkout(String id, String uid, String workoutName,
                            String customWorkoutId, long scheduledAt) {
        this(id, uid, workoutName, customWorkoutId, scheduledAt, null, null, null);
    }

    /**
     * recurrenceRule: an RRULE such as "FREQ=WEEKLY", or null for once. completedOccurrences: start
     * times of the occurrences marked as done; null means none. calendarEventId: the event in this
     * device's calendar, or null.
     */
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

    /** The Firestore document id; null before it is saved. */
    public String getId() {
        return this.id;
    }

    /** The Firebase Auth uid of the user who planned it. */
    public String getUid() {
        return this.uid;
    }

    /** The premade workout's name, or null for a custom workout. */
    public String getWorkoutName() {
        return this.workoutName;
    }

    /** The custom workout's id, or null for a premade workout. */
    public String getCustomWorkoutId() {
        return this.customWorkoutId;
    }

    /** The first (for a one-off schedule, the only) occurrence. */
    public long getScheduledAt() {
        return this.scheduledAt;
    }

    /** True when it points at a custom workout. */
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

    /** True when it repeats; see {@link #getRecurrence()}. */
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

    /**
     * Whether the occurrence starting at occurrenceSeconds is pending, completed or missed at
     * nowSeconds (FR-6.4).
     */
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
