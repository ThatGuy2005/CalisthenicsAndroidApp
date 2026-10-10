package com.example.calisthenicsapp.CoreComponents;

public class CustomWorkout extends Workout{
    public CustomWorkout(long id, String name, String username) {
        super(name);
        this.id = id;
        this.username = username;
    }
    public String getUsername() {
        return this.username;
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    private String username;
    private long id;
}
