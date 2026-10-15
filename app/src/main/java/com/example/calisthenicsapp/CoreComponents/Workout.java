package com.example.calisthenicsapp.CoreComponents;

import java.util.List;

public class Workout {
    /** level: the difficulty rating of a premade workout (FR-4.1); null for a custom workout. */
    public Workout(String name, Difficulty level) {
        this(name, level, null);
    }
    /** estimatedSeconds: how long the workout is expected to take (FR-4.1). */
    public Workout(String name, Difficulty level, Integer estimatedSeconds) {
        this.name = name;
        this.level = level;
        this.estimatedSeconds = estimatedSeconds;
    }
    public Workout(String name) {
        this(name,null);
    }
    public String getName() {
        return this.name;
    }
    /** Null for a custom workout, which has no level. */
    public Difficulty getLevel() {
        return this.level;
    }
    /**
     * Expected length in seconds: every set plus the rests between them. Filled in by
     * WorkoutService; null for a Workout read straight from the repository.
     */
    public Integer getEstimatedSeconds() {
        return this.estimatedSeconds;
    }
    private List<Exercise> exercises;
    private String name;
    private Difficulty level;
    private Integer estimatedSeconds;
}
