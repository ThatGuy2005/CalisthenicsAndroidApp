package com.example.calisthenicsapp.CoreComponents;

import java.util.List;

public class Workout {
    public Workout(String name) {
        this.name = name;
    }
    public String getName() {
        return this.name;
    }
    private List<Exercise> exercises;
    private String name;
}
