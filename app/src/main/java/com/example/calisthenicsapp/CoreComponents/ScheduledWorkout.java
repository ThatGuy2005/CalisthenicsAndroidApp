package com.example.calisthenicsapp.CoreComponents;

public class ScheduledWorkout {
    public ScheduledWorkout(Long id, String username, String workoutName,
                            Long customWorkoutId, long scheduledAt) {
        this.id = id;
        this.username = username;
        this.workoutName = workoutName;
        this.customWorkoutId = customWorkoutId;
        this.scheduledAt = scheduledAt;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getWorkoutName() {
        return this.workoutName;
    }

    public Long getCustomWorkoutId() {
        return this.customWorkoutId;
    }

    public long getScheduledAt() {
        return this.scheduledAt;
    }

    public boolean isCustom() {
        return this.customWorkoutId != null;
    }

    private Long id;                 // null until inserted
    private String username;
    private String workoutName;      // premade workout, or null
    private Long customWorkoutId;    // custom workout, or null
    private long scheduledAt;
}