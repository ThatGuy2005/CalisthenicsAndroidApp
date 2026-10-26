package com.example.calisthenicsapp.model;

/**
 * One exercise of a custom workout, at its place in the workout (a row of the CustomExerciseOrder
 * table; in Firestore, an item of the custom workout document's exercises list). The user sets the
 * sets, the reps or seconds per set and the rests (FR-4.3). Exactly one of reps and seconds is set:
 * seconds for a timed exercise, reps otherwise.
 */
public class CustomExerciseOrderEntry {
    /** Without a rest after the last set, so it is the same as restSeconds. */
    public CustomExerciseOrderEntry(String customWorkoutId, String exerciseName, int position,
                                    Integer sets, Integer reps, Integer seconds,
                                    Integer restSeconds) {
        this(customWorkoutId, exerciseName, position, sets, reps, seconds, restSeconds, null);
    }
    /** restAfterSeconds: the rest after the last set, before the next exercise; null = restSeconds. */
    public CustomExerciseOrderEntry(String customWorkoutId, String exerciseName, int position,
                                    Integer sets, Integer reps, Integer seconds,
                                    Integer restSeconds,
                                    Integer restAfterSeconds) {
        this.customWorkoutId = customWorkoutId;
        this.exerciseName = exerciseName;
        this.position = position;
        this.sets = sets;
        this.reps = reps;
        this.seconds = seconds;
        this.restSeconds = restSeconds;
        this.restAfterSeconds = restAfterSeconds;
    }
    /** The custom workout this entry belongs to. */
    public String getCustomWorkoutId() {
        return this.customWorkoutId;
    }
    /** The exercise, by its unique name (see {@link Exercise#getName()}). */
    public String getExerciseName() {
        return this.exerciseName;
    }
    /**
     * Where it comes in the workout, from 1 for the first; together with the workout id it
     * identifies the entry.
     */
    public int getPosition() {
        return  this.position;
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
    private String customWorkoutId;
    private String exerciseName;
    private int position;

    private Integer sets;
    private Integer reps;
    private Integer seconds;
    private Integer restSeconds;
    private Integer restAfterSeconds;
}
