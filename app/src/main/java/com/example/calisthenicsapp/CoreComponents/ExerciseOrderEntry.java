package com.example.calisthenicsapp.CoreComponents;

public class ExerciseOrderEntry {
    /** restAfterSeconds: the rest after the last set, before the next exercise; null = restSeconds. */
    public ExerciseOrderEntry(String workoutName, String exerciseName, Integer position,
                              Integer sets, Integer reps, Integer seconds,
                              Integer restSeconds,
                              Integer restAfterSeconds) {
        this.workoutName = workoutName;
        this.exerciseName = exerciseName;
        this.position = position;
        this.sets = sets;
        this.reps = reps;
        this.seconds = seconds;
        this.restSeconds = restSeconds;
        this.restAfterSeconds = restAfterSeconds;
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
    /** The rest between the sets of this exercise. */
    public Integer getRestSeconds() {
        return this.restSeconds;
    }
    /** The rest after its last set, before the next exercise; null means the same as getRestSeconds. */
    public Integer getRestAfterSeconds() {
        return this.restAfterSeconds;
    }
    private String workoutName;
    private String exerciseName;
    private Integer position;
    private Integer sets;
    private Integer reps;
    private Integer seconds;
    private Integer restSeconds;
    private Integer restAfterSeconds;
}
