package com.example.calisthenicsapp.CoreComponents;

public class ExerciseOrderEntry {
    public ExerciseOrderEntry(String workoutName, String exerciseName, Integer position) {
        this.workoutName = workoutName;
        this.exerciseName = exerciseName;
        this.position = position;
    }
    public String getWorkoutName() {
        return this.workoutName;
    }
    public String getExerciseName() {
        return this.exerciseName;
    }
    public Integer getPosition() {
        return this.position;
    }
    private String workoutName;
    private String exerciseName;
    private Integer position;
}
