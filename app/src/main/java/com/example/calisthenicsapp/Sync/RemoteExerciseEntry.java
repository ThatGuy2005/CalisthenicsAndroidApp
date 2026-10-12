package com.example.calisthenicsapp.Sync;

import java.util.Map;

/** One element of a customWorkouts/{id}.exercises array. */
public class RemoteExerciseEntry {
    public final String exerciseName;
    public final int position;
    public final Integer sets;
    public final Integer reps;
    public final Integer seconds;
    public final Integer restSeconds;

    private RemoteExerciseEntry(String exerciseName, int position, Integer sets, Integer reps,
                                 Integer seconds, Integer restSeconds) {
        this.exerciseName = exerciseName;
        this.position = position;
        this.sets = sets;
        this.reps = reps;
        this.seconds = seconds;
        this.restSeconds = restSeconds;
    }

    public static RemoteExerciseEntry fromMap(Map<String, Object> map) {
        return new RemoteExerciseEntry(
                (String) map.get("exerciseName"),
                asInt(map.get("position"), 1),
                asNullableInt(map.get("sets")),
                asNullableInt(map.get("reps")),
                asNullableInt(map.get("seconds")),
                asNullableInt(map.get("restSeconds")));
    }

    // Firestore decodes every JSON number as Long (or Double), never as Integer.
    private static int asInt(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static Integer asNullableInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }
}
