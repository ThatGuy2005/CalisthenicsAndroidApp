package com.example.calisthenicsapp.CoreComponents;

import java.util.List;

public class Workout {
    /** level: the difficulty rating of a premade workout (FR-4.1); null for a custom workout. */
    public Workout(String name, Difficulty level) {
        this(name, level, null);
    }
    /** category: the general category of a premade workout (FR-4.1). */
    public Workout(String name, Difficulty level, WorkoutCategory category) {
        this(name, level, category, null);
    }
    /** description: a sentence or two about the workout (DR-2). */
    public Workout(String name, Difficulty level, WorkoutCategory category, String description) {
        this(name, level, category, description, null);
    }
    /** estimatedSeconds: how long the workout is expected to take (FR-4.1). */
    public Workout(String name, Difficulty level, WorkoutCategory category, String description,
                   Integer estimatedSeconds) {
        this.name = name;
        this.level = level;
        this.category = category;
        this.description = description;
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
    /** Full body, Upper body, Lower body, Core or Cardio; null for a custom workout. */
    public WorkoutCategory getCategory() {
        return this.category;
    }
    /** What it trains and what it needs, in a sentence or two; null for a custom workout. */
    public String getDescription() {
        return this.description;
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
    private WorkoutCategory category;
    private String description;
    private Integer estimatedSeconds;
}
