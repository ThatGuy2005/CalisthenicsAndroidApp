package com.example.calisthenicsapp.CoreComponents;

public class ScheduledWorkout {
    public ScheduledWorkout(String id, String uid, String workoutName,
                            String customWorkoutId, long scheduledAt) {
        this(id, uid, workoutName, customWorkoutId, scheduledAt, null);
    }

    public ScheduledWorkout(String id, String uid, String workoutName,
                            String customWorkoutId, long scheduledAt, Long calendarEventId) {
        this.id = id;
        this.uid = uid;
        this.workoutName = workoutName;
        this.customWorkoutId = customWorkoutId;
        this.scheduledAt = scheduledAt;
        this.calendarEventId = calendarEventId;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public String getWorkoutName() {
        return this.workoutName;
    }

    public String getCustomWorkoutId() {
        return this.customWorkoutId;
    }

    public long getScheduledAt() {
        return this.scheduledAt;
    }

    public boolean isCustom() {
        return this.customWorkoutId != null;
    }

    /** The event in this device's calendar, or null if it was not added here (FR-6.2). */
    public Long getCalendarEventId() {
        return this.calendarEventId;
    }

    private String id;               // null until inserted
    private String uid;
    private String workoutName;      // premade workout, or null
    private String customWorkoutId;  // custom workout, or null
    private long scheduledAt;
    private Long calendarEventId;    // local to this device, see the CalendarEvent table
}
