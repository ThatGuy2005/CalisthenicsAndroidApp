package com.example.calisthenicsapp.sync;

import com.example.calisthenicsapp.model.LoggedExercise;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mirrors a users/{uid}/workoutLogs/{id} Firestore document. */
public class RemoteWorkoutLog {
    /** The document id. */
    public final String id;
    /** The workout's name at the time. */
    public final String workoutName;
    /** Epoch seconds; 0 if missing. */
    public final long startedAt;
    /** Rests included, pauses excluded; 0 if missing. */
    public final int durationSeconds;
    /** Estimated calories; 0 if missing. */
    public final double kcal;
    /** Completed or Partial (see WorkoutLog). */
    public final String status;                    // null for logs saved before statuses existed
    /** The snapshot of what was done. */
    public final List<LoggedExercise> exercises;   // empty for logs saved before snapshots existed

    /** Made by {@link #fromSnapshot}. */
    RemoteWorkoutLog(String id, String workoutName, long startedAt,
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

    /** Reads the document; missing numbers count as 0. */
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

    /**
     * The exercises array of a log document; package-private for the tests. Firestore hands an
     * array of maps back as a List of Maps, with numbers as Long or Double.
     */
    static List<LoggedExercise> exercisesFrom(Object raw) {
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
                    number(m.get("kcal"), 0).doubleValue(),
                    Boolean.TRUE.equals(m.get("perSide"))));      // absent in older logs
        }
        return list;
    }

    /** value if it is a number, otherwise fallback. */
    private static Number number(Object value, Number fallback) {
        return value instanceof Number ? (Number) value : fallback;
    }
}
