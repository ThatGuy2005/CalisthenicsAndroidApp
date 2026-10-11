package com.example.calisthenicsapp.CoreComponents;

public class ExerciseOrderEntry {
    public ExerciseOrderEntry(String workoutName, String exerciseName, Integer position,
                              Integer sets, Integer reps, Integer seconds,
                              Integer restSeconds) {
        this.workoutName = workoutName;
        this.exerciseName = exerciseName;
        this.position = position;
        this.sets = sets;
        this.reps = reps;
        this.seconds = seconds;
        this.restSeconds = restSeconds;
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
    public Integer getSets() {
        return this.sets;
    }

    public Integer getReps() {
        return this.reps;
    }
    public Integer getSeconds() {
        return this.seconds;
    }
    public Integer getRestSeconds() {
        return this.restSeconds;
    }
    private String workoutName;
    private String exerciseName;
    private Integer position;
    private Integer sets;
    private Integer reps;
    private Integer seconds;
    private Integer restSeconds;
}
