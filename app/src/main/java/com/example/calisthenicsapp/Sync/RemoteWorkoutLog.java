package com.example.calisthenicsapp.Sync;

import com.example.calisthenicsapp.CoreComponents.LoggedExercise;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mirrors a users/{uid}/workoutLogs/{id} Firestore document. */
public class RemoteWorkoutLog {
    public final String id;
    public final String workoutName;
    public final long startedAt;
    public final int durationSeconds;
    public final double kcal;
    public final String status;                    // null for logs saved before statuses existed
    public final List<LoggedExercise> exercises;   // empty for logs saved before snapshots existed

    private RemoteWorkoutLog(String id, String workoutName, long startedAt,
                              int durationSeconds, double kcal, String status,
                              List<LoggedExercise> exercises) {
        this.id = id;
        this.workoutName = workoutName;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
        this.kcal = kcal;
        this.status = status;
        this.exercises = exercises;
    }

    public static RemoteWorkoutLog fromSnapshot(DocumentSnapshot doc) {
        Long startedAt = doc.getLong("startedAt");
        Long durationSeconds = doc.getLong("durationSeconds");
        Double kcal = doc.getDouble("kcal");
        return new RemoteWorkoutLog(doc.getId(), doc.getString("workoutName"),
                startedAt == null ? 0L : startedAt,
                durationSeconds == null ? 0 : durationSeconds.intValue(),
                kcal == null ? 0.0 : kcal,
                doc.getString("status"),
                exercisesFrom(doc.get("exercises")));
    }

    /** Firestore hands an array of maps back as a List of Maps, with numbers as Long or Double. */
    private static List<LoggedExercise> exercisesFrom(Object raw) {
        List<LoggedExercise> list = new ArrayList<>();
        if (!(raw instanceof List)) return list;
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) item;
            Object name = m.get("exerciseName");
            list.add(new LoggedExercise(
                    name instanceof String ? (String) name : null,
                    number(m.get("met"), 0).doubleValue(),
                    Boolean.TRUE.equals(m.get("timed")),
                    number(m.get("sets"), 0).intValue(),
                    number(m.get("completedSets"), 0).intValue(),
                    m.get("reps") instanceof Number ? ((Number) m.get("reps")).intValue() : null,
                    m.get("seconds") instanceof Number ? ((Number) m.get("seconds")).intValue() : null,
                    number(m.get("activeSeconds"), 0).intValue(),
                    number(m.get("kcal"), 0).doubleValue()));
        }
        return list;
    }

    private static Number number(Object value, Number fallback) {
        return value instanceof Number ? (Number) value : fallback;
    }
}
