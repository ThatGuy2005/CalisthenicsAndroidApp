package com.example.calisthenicsapp.Sync;

import com.google.firebase.firestore.DocumentSnapshot;

/** Mirrors a users/{uid}/workoutLogs/{id} Firestore document. */
public class RemoteWorkoutLog {
    public final String id;
    public final String workoutName;
    public final long startedAt;
    public final int durationSeconds;
    public final double kcal;

    private RemoteWorkoutLog(String id, String workoutName, long startedAt,
                              int durationSeconds, double kcal) {
        this.id = id;
        this.workoutName = workoutName;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.kcal = kcal;
    }

    public static RemoteWorkoutLog fromSnapshot(DocumentSnapshot doc) {
        Long startedAt = doc.getLong("startedAt");
        Long durationSeconds = doc.getLong("durationSeconds");
        Double kcal = doc.getDouble("kcal");
        return new RemoteWorkoutLog(doc.getId(), doc.getString("workoutName"),
                startedAt == null ? 0L : startedAt,
                durationSeconds == null ? 0 : durationSeconds.intValue(),
                kcal == null ? 0.0 : kcal);
    }
}
