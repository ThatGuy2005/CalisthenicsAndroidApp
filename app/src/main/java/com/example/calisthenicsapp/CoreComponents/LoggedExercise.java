package com.example.calisthenicsapp.CoreComponents;

/**
 * One exercise of a workout log, as it was done (FR-5.5). It is a copy, not a reference, so the
 * log stays intact when the workout or the exercise is later edited or deleted.
 * reps and seconds are the per-set targets: reps for rep-based exercises, seconds for timed
 * ones, the other is null. For a per-side exercise they count for each side.
 */
public class LoggedExercise {
    public LoggedExercise(String exerciseName, double met, boolean timed, int sets,
                          int completedSets, Integer reps, Integer seconds, int activeSeconds,
                          double kcal) {
        this(exerciseName, met, timed, sets, completedSets, reps, seconds, activeSeconds, kcal,
                false);
    }

    /** perSide: the reps or seconds count for each side (FR-4.2). */
    public LoggedExercise(String exerciseName, double met, boolean timed, int sets,
                          int completedSets, Integer reps, Integer seconds, int activeSeconds,
                          double kcal, boolean perSide) {
        this.exerciseName = exerciseName;
        this.met = met;
        this.timed = timed;
        this.sets = sets;
        this.completedSets = completedSets;
        this.reps = reps;
        this.seconds = seconds;
        this.activeSeconds = activeSeconds;
        this.kcal = kcal;
        this.perSide = perSide;
    }

    public String getExerciseName() {
        return this.exerciseName;
    }
    public double getMet() {
        return this.met;
    }
    public boolean isTimed() {
        return this.timed;
    }
    /** Planned number of sets. */
    public int getSets() {
        return this.sets;
    }
    public int getCompletedSets() {
        return this.completedSets;
    }
    public Integer getReps() {
        return this.reps;
    }
    public Integer getSeconds() {
        return this.seconds;
    }
    /** Show "12 reps per side". False for logs saved before the flag existed. */
    public boolean isPerSide() {
        return this.perSide;
    }
    /** Time spent doing the completed sets, rests and pauses excluded. */
    public int getActiveSeconds() {
        return this.activeSeconds;
    }
    public double getKcal() {
        return this.kcal;
    }

    private String exerciseName;
    private double met;
    private boolean timed;
    private int sets;
    private int completedSets;
    private Integer reps;
    private Integer seconds;
    private int activeSeconds;
    private double kcal;
    private boolean perSide;
}
