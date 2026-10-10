package com.example.calisthenicsapp.CoreComponents;

public class CustomExerciseOrderEntry {
    public CustomExerciseOrderEntry(long customWorkoutId, String exerciseName, int position) {
        this.customWorkoutId = customWorkoutId;
        this.exerciseName = exerciseName;
        this.position = position;
    }
    public long getCustomWorkoutId() {
        return this.customWorkoutId;
    }
    public String getExerciseName() {
        return this.exerciseName;
    }
    public int getPosition() {
        return  this.position;
    }
    private long customWorkoutId;
    private String exerciseName;
    private int position;
}
