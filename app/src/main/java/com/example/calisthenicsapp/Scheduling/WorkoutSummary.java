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

    /** Rough time one rep takes, for the duration estimate. */
    static final int SECONDS_PER_REP = 3;
    static final int MIN_EVENT_SECONDS = 15 * 60;
    private static final int ROUND_TO_SECONDS = 5 * 60;

    private final String scheduleId;
    private final String workoutName;
    private final long startSeconds;
    private final List<SessionExercise> exercises;

    public WorkoutSummary(String scheduleId, String workoutName, long startSeconds,
                          List<SessionExercise> exercises) {
        this.scheduleId = scheduleId;
        this.workoutName = workoutName;
        this.startSeconds = startSeconds;
        this.exercises = Collections.unmodifiableList(new ArrayList<>(exercises));
    }

    /** Reads the schedule's workout from SQLite. Null if the workout no longer exists. */
    public static WorkoutSummary of(SQLiteDatabase db, ScheduledWorkout schedule) {
        return of(db, schedule.getId(), schedule.getWorkoutName(), schedule.getCustomWorkoutId(),
                schedule.getScheduledAt());
    }

    /** Exactly one of workoutName (premade) and customWorkoutId is set. */
    public static WorkoutSummary of(SQLiteDatabase db, String scheduleId, String workoutName,
                                    String customWorkoutId, long startSeconds) {
        if (customWorkoutId != null) {
            CustomWorkout workout = new CustomWorkoutRepository(db).getOne(customWorkoutId);
            if (workout == null) return null;
            return new WorkoutSummary(scheduleId, workout.getName(), startSeconds,
                    WorkoutExercises.ofCustom(db, customWorkoutId));
        }
        if (workoutName == null || new WorkoutRepository(db).getOne(workoutName) == null) return null;
        return new WorkoutSummary(scheduleId, workoutName, startSeconds,
                WorkoutExercises.ofPremade(db, workoutName));
    }

    public String getScheduleId() { return scheduleId; }
    public String getWorkoutName() { return workoutName; }
    public long getStartSeconds() { return startSeconds; }
    public List<SessionExercise> getExercises() { return exercises; }

    /** Every set plus the rests between them; no rest after the very last set. */
    public int getEstimatedSeconds() {
        int total = 0;
        for (SessionExercise e : exercises) {
            int work = e.isTimed() ? valueOf(e.getTargetSeconds())
                    : valueOf(e.getTargetReps()) * SECONDS_PER_REP;
            total += e.getSets() * (work + e.getRestSeconds());
        }
        if (!exercises.isEmpty()) total -= exercises.get(exercises.size() - 1).getRestSeconds();
        return Math.max(total, 0);
    }

    /** The estimate rounded up to whole 5 minutes, and at least 15 minutes. */
    public long getEndSeconds() {
        int rounded = (getEstimatedSeconds() + ROUND_TO_SECONDS - 1) / ROUND_TO_SECONDS * ROUND_TO_SECONDS;
        return startSeconds + Math.max(rounded, MIN_EVENT_SECONDS);
    }

    public String getTitle() {
        return "Workout: " + workoutName;
    }

    /** One numbered line per exercise, e.g. "1. Push-up: 3 x 10 reps, 60 s rest". */
    public String getExerciseList() {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < exercises.size(); i++) {
            SessionExercise e = exercises.get(i);
            if (i > 0) list.append('\n');
            list.append(i + 1).append(". ").append(e.getName()).append(": ")
                    .append(e.getSets()).append(" x ")
                    .append(e.isTimed() ? valueOf(e.getTargetSeconds()) + " s"
                            : valueOf(e.getTargetReps()) + " reps");
            if (e.getRestSeconds() > 0) list.append(", ").append(e.getRestSeconds()).append(" s rest");
        }
        return list.toString();
    }

    /** The calendar event's description. */
    public String getDescription() {
        return "About " + estimatedMinutes() + " min.\n\nExercises:\n" + getExerciseList();
    }

    public String getEmailSubject(TimeZone zone, Locale locale) {
        return "Scheduled workout: " + workoutName + ", " + formatStart(zone, locale);
    }

    public String getEmailBody(TimeZone zone, Locale locale) {
        return "Workout: " + workoutName + "\n"
                + "When: " + formatStart(zone, locale) + " (about " + estimatedMinutes() + " min)\n\n"
                + "Exercises:\n" + getExerciseList() + "\n\n"
                + "Sent from CalisthenicsApp.\n";
    }

    /** The .ics file; its UID is fixed per schedule, so importing it again updates the event. */
    public String toIcs(long nowSeconds) {
        return IcsWriter.event(scheduleId + "@calisthenicsapp", startSeconds, getEndSeconds(),
                getTitle(), getDescription(), REMINDER_MINUTES, nowSeconds);
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
