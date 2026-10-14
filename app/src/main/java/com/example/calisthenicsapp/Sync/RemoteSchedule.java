package com.example.calisthenicsapp.Sync;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Mirrors a users/{uid}/schedules/{id} Firestore document. */
public class RemoteSchedule {
    public final String id;
    public final String workoutName;
    public final String customWorkoutId;
    public final long scheduledAt;
    public final String recurrenceRule;   // null = once
    public final List<Long> completed;    // completed occurrence start times

    private RemoteSchedule(String id, String workoutName, String customWorkoutId, long scheduledAt,
                           String recurrenceRule, List<Long> completed) {
        this.id = id;
        this.workoutName = workoutName;
        this.customWorkoutId = customWorkoutId;
        this.scheduledAt = scheduledAt;
        this.recurrenceRule = recurrenceRule;
        this.completed = completed;
    }

    public static RemoteSchedule fromSnapshot(DocumentSnapshot doc) {
        Long scheduledAt = doc.getLong("scheduledAt");
        List<Long> completed = new ArrayList<>();
        Object raw = doc.get("completed");
        if (raw instanceof List) {
            for (Object value : (List<?>) raw) {
                if (value instanceof Number) completed.add(((Number) value).longValue());
            }
        }
        return new RemoteSchedule(doc.getId(), doc.getString("workoutName"),
                doc.getString("customWorkoutId"), scheduledAt == null ? 0L : scheduledAt,
                doc.getString("recurrenceRule"), completed);
    }
}
