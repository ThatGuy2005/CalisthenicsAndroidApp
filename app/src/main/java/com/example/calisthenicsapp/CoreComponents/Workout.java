package com.example.calisthenicsapp.CoreComponents;

import java.util.List;

public class Workout {
    /** level: the difficulty rating of a premade workout (FR-4.1); null for a custom workout. */
    public Workout(String name, Difficulty level) {
        this.name = name;
        this.level = level;
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
    private List<Exercise> exercises;
    private String name;
    private Difficulty level;
}
