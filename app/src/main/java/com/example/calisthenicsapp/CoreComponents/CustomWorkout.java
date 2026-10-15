package com.example.calisthenicsapp.CoreComponents;

public class CustomWorkout extends Workout{
    public CustomWorkout(String id, String name, String uid) {
        this(id, name, uid, null);
    }
    /** estimatedSeconds: see Workout.getEstimatedSeconds(); filled in by CustomWorkoutService. */
    public CustomWorkout(String id, String name, String uid, Integer estimatedSeconds) {
        super(name, null, null, null, estimatedSeconds);
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
