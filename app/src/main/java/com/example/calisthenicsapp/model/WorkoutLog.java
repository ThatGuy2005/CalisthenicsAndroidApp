package com.example.calisthenicsapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One workout the user did (FR-5.5), a document of users/{uid}/workoutLogs mirrored in the
 * WorkoutLog table. It keeps a copy of the exercises as they were done, so editing or deleting the
 * workout later does not change the history. Times are epoch seconds.
 */
public class WorkoutLog {
    /** Every set of every exercise was done. */
    public static final String STATUS_COMPLETED = "Completed";
    /** The workout was ended early, or some sets were skipped. */
    public static final String STATUS_PARTIAL = "Partial";

    /** exercises is the snapshot of what was done (FR-5.5); empty if none was stored. */
    public WorkoutLog(String id, String uid, String workoutName,
                      long startedAt, int durationSeconds, double kcal,
                      String status, List<LoggedExercise> exercises) {
        this.id = id;
        this.uid = uid;
        this.workoutName = workoutName;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.kcal = kcal;
        this.status = STATUS_PARTIAL.equals(status) ? STATUS_PARTIAL : STATUS_COMPLETED;
        this.exercises = exercises == null ? new ArrayList<>() : new ArrayList<>(exercises);
    }

    /** The Firestore document id. */
    public String getId() {
        return this.id;
    }

    /** The Firebase Auth uid of the user who did it. */
    public String getUid() {
        return this.uid;
    }

    /** The workout's name at the time, premade or custom. */
    public String getWorkoutName() {
        return this.workoutName;
    }

    /** When the session started. */
    public long getStartedAt() {
        return this.startedAt;
    }

    /** How long it took, rests included and pauses excluded. */
    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    /** Estimated calories burned in total (FR-5.4). */
    public double getKcal() {
        return this.kcal;
    }

    /** STATUS_COMPLETED or STATUS_PARTIAL. */
    public String getStatus() {
        return this.status;
    }

    /** The exercises in the order they came; read only. */
    public List<LoggedExercise> getExercises() {
        return Collections.unmodifiableList(this.exercises);
    }

    private String id;
    private String uid;
    private String workoutName;
    private long startedAt;
    private int durationSeconds;
    private double kcal;
    private String status;
    private List<LoggedExercise> exercises;
}
