package com.example.calisthenicsapp.Sync;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mirrors a users/{uid}/customWorkouts/{id} Firestore document. */
public class RemoteCustomWorkout {
    public final String id;
    public final String name;
    public final String description;        // null if none, also in workouts saved before it existed
    public final List<RemoteExerciseEntry> exercises;

    private RemoteCustomWorkout(String id, String name, String description,
                                List<RemoteExerciseEntry> exercises) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.exercises = exercises;
    }

    @SuppressWarnings("unchecked")
    public static RemoteCustomWorkout fromSnapshot(DocumentSnapshot doc) {
        List<RemoteExerciseEntry> exercises = new ArrayList<>();
        Object rawExercises = doc.get("exercises");
        if (rawExercises instanceof List) {
            for (Object entry : (List<Object>) rawExercises) {
                if (entry instanceof Map) {
                    exercises.add(RemoteExerciseEntry.fromMap((Map<String, Object>) entry));
                }
            }
        }
        return new RemoteCustomWorkout(doc.getId(), doc.getString("name"),
                doc.getString("description"), exercises);
    }
}
