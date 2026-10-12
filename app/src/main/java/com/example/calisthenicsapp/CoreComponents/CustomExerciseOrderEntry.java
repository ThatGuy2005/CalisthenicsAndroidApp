package com.example.calisthenicsapp.CoreComponents;

public class CustomExerciseOrderEntry {
    public CustomExerciseOrderEntry(String customWorkoutId, String exerciseName, int position,
                                    Integer sets, Integer reps, Integer seconds,
                                    Integer restSeconds) {
        this.customWorkoutId = customWorkoutId;
        this.exerciseName = exerciseName;
        this.position = position;
        this.sets = sets;
        this.reps = reps;
        this.seconds = seconds;
        this.restSeconds = restSeconds;
    }
    public String getCustomWorkoutId() {
        return this.customWorkoutId;
    }
    public String getExerciseName() {
        return this.exerciseName;
    }
    public int getPosition() {
        return  this.position;
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
    private String customWorkoutId;
    private String exerciseName;
    private int position;

    private Integer sets;
    private Integer reps;
    private Integer seconds;
    private Integer restSeconds;
}
