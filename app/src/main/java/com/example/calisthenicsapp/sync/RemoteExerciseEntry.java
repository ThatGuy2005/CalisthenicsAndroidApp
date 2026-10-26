package com.example.calisthenicsapp.sync;

import java.util.Map;

/** One element of a customWorkouts/{id}.exercises array. */
public class RemoteExerciseEntry {
    /** The exercise, by its unique name. */
    public final String exerciseName;
    /** Its place in the workout, from 1; 1 if missing. */
    public final int position;
    /** Number of sets; null if missing. */
    public final Integer sets;
    /** Reps per set; null for a timed exercise. */
    public final Integer reps;
    /** Seconds per set; null for a rep-based exercise. */
    public final Integer seconds;
    /** The rest between sets, in seconds. */
    public final Integer restSeconds;
    /** Null in workouts saved before it existed: the same as restSeconds. */
    public final Integer restAfterSeconds;

    private RemoteExerciseEntry(String exerciseName, int position, Integer sets, Integer reps,
                                 Integer seconds, Integer restSeconds, Integer restAfterSeconds) {
        this.exerciseName = exerciseName;
        this.position = position;
        this.sets = sets;
        this.reps = reps;
        this.seconds = seconds;
        this.restSeconds = restSeconds;
        this.restAfterSeconds = restAfterSeconds;
    }

    /** Reads one array element, as Firestore hands it over: a map of field names to values. */
    public static RemoteExerciseEntry fromMap(Map<String, Object> map) {
        return new RemoteExerciseEntry(
                (String) map.get("exerciseName"),
                asInt(map.get("position"), 1),
                asNullableInt(map.get("sets")),
                asNullableInt(map.get("reps")),
                asNullableInt(map.get("seconds")),
                asNullableInt(map.get("restSeconds")),
                asNullableInt(map.get("restAfterSeconds")));
    }

    // Firestore decodes every JSON number as Long (or Double), never as Integer.
    private static int asInt(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    /** Like asInt, but null when the field is missing or not a number. */
    private static Integer asNullableInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }
}
