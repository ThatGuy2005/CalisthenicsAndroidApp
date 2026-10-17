package com.example.calisthenicsapp.Scheduling;

import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Session.WorkoutExercises;
import com.example.calisthenicsapp.Session.WorkoutSession.SessionExercise;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * What a scheduled workout's calendar event (FR-6.2) and email (FR-6.3) say: the workout name,
 * the date and time, and the exercises. The event lasts as long as the workout is expected to.
 */
public final class WorkoutSummary {
    /** Reminder before the workout, in the calendar event and the .ics file. */
    public static final int REMINDER_MINUTES = 30;

    static final int MIN_EVENT_SECONDS = 15 * 60;
    private static final int ROUND_TO_SECONDS = 5 * 60;

    private final String scheduleId;
    private final String workoutName;
    private final long startSeconds;
    private final Recurrence recurrence;    // null = once
    private final List<SessionExercise> exercises;

    public WorkoutSummary(String scheduleId, String workoutName, long startSeconds,
                          List<SessionExercise> exercises) {
        this(scheduleId, workoutName, startSeconds, null, exercises);
    }

    /** startSeconds is the first occurrence; recurrence is null for a one-off workout. */
    public WorkoutSummary(String scheduleId, String workoutName, long startSeconds,
                          Recurrence recurrence, List<SessionExercise> exercises) {
        this.scheduleId = scheduleId;
        this.workoutName = workoutName;
        this.startSeconds = startSeconds;
        this.recurrence = recurrence;
        this.exercises = Collections.unmodifiableList(new ArrayList<>(exercises));
    }

    /** Reads the schedule's workout from SQLite. Null if the workout no longer exists. */
    public static WorkoutSummary of(SQLiteDatabase db, ScheduledWorkout schedule) {
        return of(db, schedule.getId(), schedule.getWorkoutName(), schedule.getCustomWorkoutId(),
                schedule.getScheduledAt(), schedule.getRecurrence());
    }

    /** Exactly one of workoutName (premade) and customWorkoutId is set. */
    public static WorkoutSummary of(SQLiteDatabase db, String scheduleId, String workoutName,
                                    String customWorkoutId, long startSeconds,
                                    Recurrence recurrence) {
        if (customWorkoutId != null) {
            CustomWorkout workout = new CustomWorkoutRepository(db).getOne(customWorkoutId);
            if (workout == null) return null;
            return new WorkoutSummary(scheduleId, workout.getName(), startSeconds, recurrence,
                    WorkoutExercises.ofCustom(db, customWorkoutId));
        }
        if (workoutName == null || new WorkoutRepository(db).getOne(workoutName) == null) return null;
        return new WorkoutSummary(scheduleId, workoutName, startSeconds, recurrence,
                WorkoutExercises.ofPremade(db, workoutName));
    }

    public String getScheduleId() { return scheduleId; }
    public String getWorkoutName() { return workoutName; }
    public long getStartSeconds() { return startSeconds; }
    /** Null if the workout happens once. */
    public Recurrence getRecurrence() { return recurrence; }
    public List<SessionExercise> getExercises() { return exercises; }

    /** Every set plus the rests between them; see {@link WorkoutExercises#estimateSeconds}. */
    public int getEstimatedSeconds() {
        return WorkoutExercises.estimateSeconds(exercises);
    }

    /** How long the calendar event lasts: the estimate rounded up to whole 5 minutes, at least 15. */
    public int getEventSeconds() {
        int rounded = (getEstimatedSeconds() + ROUND_TO_SECONDS - 1) / ROUND_TO_SECONDS * ROUND_TO_SECONDS;
        return Math.max(rounded, MIN_EVENT_SECONDS);
    }

    /** End of the (first) occurrence. */
    public long getEndSeconds() {
        return startSeconds + getEventSeconds();
    }

    public String getTitle() {
        return "Workout: " + workoutName;
    }

    /**
     * One numbered line per exercise, e.g. "1. Push-up: 3 x 10 reps, 60 s rest", with
     * ", 90 s before the next exercise" when that rest is different and "12 reps per side" for
     * a per-side exercise.
     */
    public String getExerciseList() {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < exercises.size(); i++) {
            SessionExercise e = exercises.get(i);
            if (i > 0) list.append('\n');
            list.append(i + 1).append(". ").append(e.getName()).append(": ")
                    .append(e.getSets()).append(" x ")
                    .append(e.isTimed() ? valueOf(e.getTargetSeconds()) + " s"
                            : valueOf(e.getTargetReps()) + " reps");
            if (e.isPerSide()) list.append(" per side");
            if (e.getRestSeconds() > 0) list.append(", ").append(e.getRestSeconds()).append(" s rest");
            boolean last = i == exercises.size() - 1;
            if (!last && e.getRestAfterSeconds() != e.getRestSeconds()) {
                list.append(", ").append(e.getRestAfterSeconds()).append(" s before the next exercise");
            }
        }
        return list.toString();
    }

    /** The calendar event's description. */
    public String getDescription() {
        return "About " + estimatedMinutes() + " min." + repeatsLine()
                + "\n\nExercises:\n" + getExerciseList();
    }

    public String getEmailSubject(TimeZone zone, Locale locale) {
        return "Scheduled workout: " + workoutName + ", " + formatStart(zone, locale);
    }

    public String getEmailBody(TimeZone zone, Locale locale) {
        return "Workout: " + workoutName + "\n"
                + "When: " + formatStart(zone, locale) + " (about " + estimatedMinutes() + " min)"
                + repeatsLine() + "\n\n"
                + "Exercises:\n" + getExerciseList() + "\n\n"
                + "Sent from CalisthenicsApp.\n";
    }

    /** The .ics file; its UID is fixed per schedule, so importing it again updates the event. */
    public String toIcs(long nowSeconds) {
        return IcsWriter.event(scheduleId + "@calisthenicsapp", startSeconds, getEndSeconds(),
                recurrence == null ? null : recurrence.toRule(),
                getTitle(), getDescription(), REMINDER_MINUTES, nowSeconds);
    }

    private String repeatsLine() {
        return recurrence == null ? "" : "\nRepeats: " + recurrence.describe().toLowerCase(Locale.ROOT);
    }

    private int estimatedMinutes() {
        return Math.max(1, (getEstimatedSeconds() + 59) / 60);
    }

    private String formatStart(TimeZone zone, Locale locale) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, locale);
        format.setTimeZone(zone);
        return format.format(new Date(startSeconds * 1000L));
    }

    private static int valueOf(Integer value) {
        return value == null ? 0 : value;
    }
}
