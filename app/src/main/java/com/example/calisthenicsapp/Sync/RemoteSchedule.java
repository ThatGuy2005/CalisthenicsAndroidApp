package com.example.calisthenicsapp.Sync;

import com.google.firebase.firestore.DocumentSnapshot;

/** Mirrors a users/{uid}/schedules/{id} Firestore document. */
public class RemoteSchedule {
    public final String id;
    public final String workoutName;
    public final String customWorkoutId;
    public final long scheduledAt;

    private RemoteSchedule(String id, String workoutName, String customWorkoutId, long scheduledAt) {
        this.id = id;
        this.workoutName = workoutName;
        this.customWorkoutId = customWorkoutId;
        this.scheduledAt = scheduledAt;
    }

    public static RemoteSchedule fromSnapshot(DocumentSnapshot doc) {
        Long scheduledAt = doc.getLong("scheduledAt");
        return new RemoteSchedule(doc.getId(), doc.getString("workoutName"),
                doc.getString("customWorkoutId"), scheduledAt == null ? 0L : scheduledAt);
    }
}
