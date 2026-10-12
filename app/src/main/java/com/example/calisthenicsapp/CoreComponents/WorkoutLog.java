package com.example.calisthenicsapp.CoreComponents;

public class WorkoutLog {
    public WorkoutLog(String id, String uid, String workoutName,
                      long startedAt, int durationSeconds, double kcal) {
        this.id = id;
        this.uid = uid;
        this.workoutName = workoutName;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.kcal = kcal;
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

    private String id;
    private String uid;
    private String workoutName;
    private long startedAt;
    private int durationSeconds;
    private double kcal;
}
