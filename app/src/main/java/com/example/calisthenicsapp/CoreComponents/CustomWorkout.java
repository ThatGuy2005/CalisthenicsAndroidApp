package com.example.calisthenicsapp.CoreComponents;

public class CustomWorkout extends Workout{
    public CustomWorkout(String id, String name, String uid) {
        super(name);
        this.id = id;
        this.uid = uid;
    }
    public String getUid() {
        return this.uid;
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    private String uid;
    private String id;
}
