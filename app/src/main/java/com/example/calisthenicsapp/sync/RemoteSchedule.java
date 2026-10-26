package com.example.calisthenicsapp.sync;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Mirrors a users/{uid}/schedules/{id} Firestore document. */
public class RemoteSchedule {
    /** The document id. */
    public final String id;
    /** The premade workout's name, or null. */
    public final String workoutName;
    /** The custom workout's id, or null. */
    public final String customWorkoutId;
    /** The first occurrence, in epoch seconds; 0 if missing. */
    public final long scheduledAt;
    /** The RRULE text. */
    public final String recurrenceRule;   // null = once
    /** In epoch seconds; empty if none. */
    public final List<Long> completed;    // completed occurrence start times

    /** Made by {@link #fromSnapshot}. */
    RemoteSchedule(String id, String workoutName, String customWorkoutId, long scheduledAt,
                           String recurrenceRule, List<Long> completed) {
        this.id = id;
        this.workoutName = workoutName;
        this.customWorkoutId = customWorkoutId;
        this.scheduledAt = scheduledAt;
        this.recurrenceRule = recurrenceRule;
        this.completed = completed;
    }

    /** Reads the document; values that are not numbers in the completed array are skipped. */
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
