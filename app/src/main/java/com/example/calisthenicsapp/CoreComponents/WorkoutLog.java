package com.example.calisthenicsapp.CoreComponents;

public class WorkoutLog {
    public WorkoutLog(long id, String username, String workoutName,
                      long startedAt, int durationSeconds, double kcal) {
        this.id = id;
        this.username = username;
        this.workoutName = workoutName;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.kcal = kcal;
    }

    public long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
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

    private long id;
    private String username;
    private String workoutName;
    private long startedAt;
    private int durationSeconds;
    private double kcal;
}
