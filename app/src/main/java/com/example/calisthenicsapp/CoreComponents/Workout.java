package com.example.calisthenicsapp.CoreComponents;

import java.util.List;

public class Workout {
    public Workout(String name, String level) {
        this.name = name;
        this.level = level;
    }
    public Workout(String name) {
        this(name,null);
    }
    public String getName() {
        return this.name;
    }
    public String getLevel() {
        return this.level;
    }
    private List<Exercise> exercises;
    private String name;
    private String level;
}
