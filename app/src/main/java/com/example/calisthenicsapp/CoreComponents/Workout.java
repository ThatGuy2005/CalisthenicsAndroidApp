package com.example.calisthenicsapp.CoreComponents;

public class Workout {
    /**
     * level: the difficulty rating of a premade workout (FR-4.1); null for a custom workout.
     * category: the general category of a premade workout (FR-4.1).
     * description: a sentence or two about the workout (DR-2).
     */
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
    /**
     * What it trains and what it needs, in a sentence or two. For a custom workout it is the
     * user's own text, or null if they wrote none.
     */
    public String getDescription() {
        return this.description;
    }
    /**
     * Expected length in seconds: every set plus the rests between them. Filled in by
     * WorkoutService and CustomWorkoutService; null for one read straight from a repository.
     */
    public Integer getEstimatedSeconds() {
        return this.estimatedSeconds;
    }
    private String name;
    private Difficulty level;
    private WorkoutCategory category;
    private String description;
    private Integer estimatedSeconds;
}
