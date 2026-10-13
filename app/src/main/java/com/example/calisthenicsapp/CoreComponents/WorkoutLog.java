package com.example.calisthenicsapp.CoreComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkoutLog {
    public static final String STATUS_COMPLETED = "Completed";
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

    public String getId() {
        return this.id;
    }

    public String getUid() {
        return this.uid;
    }

    public String getWorkoutName() {
        return this.workoutName;
    }

    public long getStartedAt() {
        return this.startedAt;
    }

    public int getDurationSeconds() {
        return this.durationSeconds;
    }

    public double getKcal() {
        return this.kcal;
    }

    /** STATUS_COMPLETED or STATUS_PARTIAL. */
    public String getStatus() {
        return this.status;
    }

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
