package com.example.calisthenicsapp.CoreComponents;

/** Summary of the workout logs in a {@link StatsPeriod} (FR-5.7). */
public class WorkoutStats {
    public WorkoutStats(int workouts, int completedWorkouts, int partialWorkouts,
                        long totalSeconds, double totalKcal) {
        this.workouts = workouts;
        this.completedWorkouts = completedWorkouts;
        this.partialWorkouts = partialWorkouts;
        this.totalSeconds = totalSeconds;
        this.totalKcal = totalKcal;
    }

    /** Number of logged workouts, completed and partial together. */
    public int getWorkouts() {
        return this.workouts;
    }
    public int getCompletedWorkouts() {
        return this.completedWorkouts;
    }
    public int getPartialWorkouts() {
        return this.partialWorkouts;
    }
    /** Total training time: the logs' durations added up, rests included, pauses excluded. */
    public long getTotalSeconds() {
        return this.totalSeconds;
    }
    public double getTotalKcal() {
        return this.totalKcal;
    }

    private int workouts;
    private int completedWorkouts;
    private int partialWorkouts;
    private long totalSeconds;
    private double totalKcal;
}
