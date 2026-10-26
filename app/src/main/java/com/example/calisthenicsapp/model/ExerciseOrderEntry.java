package com.example.calisthenicsapp.model;

/**
 * One exercise of a premade workout, at its place in the workout (a row of the ExerciseOrder table
 * from seed.sql). Exactly one of reps and seconds is set: seconds for a timed exercise, reps
 * otherwise. The custom workout counterpart is {@link CustomExerciseOrderEntry}.
 */
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
    /** The premade workout, by its unique name (see {@link Workout#getName()}). */
    public String getWorkoutName() {
        return this.workoutName;
    }
    /** The exercise, by its unique name (see {@link Exercise#getName()}). */
    public String getExerciseName() {
        return this.exerciseName;
    }
    /** Where it comes in the workout, from 1 for the first. */
    public Integer getPosition() {
        return this.position;
    }
    /** How many sets to do. */
    public Integer getSets() {
        return this.sets;
    }

    /** Reps per set (per side for a per-side exercise); null for a timed exercise. */
    public Integer getReps() {
        return this.reps;
    }
    /** Seconds per set (per side for a per-side exercise); null for a rep-based exercise. */
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
