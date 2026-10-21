package com.example.calisthenicsapp.Export;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * The user's data as one JSON document (NFR-5): profile, custom workouts with their exercises,
 * workout logs with their snapshots, and schedules. Times are epoch seconds, like in Firestore.
 */
public final class JsonExport {
    private JsonExport() {
    }

    public static JSONObject build(ExportData data, long exportedAt) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("exportedAt", exportedAt);

        User user = data.getUser();
        JSONObject profile = new JSONObject();
        profile.put("uid", data.getUid());
        if (user != null) {
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("heightCm", orNull(user.getHeight()));
            profile.put("weightKg", orNull(user.getWeight()));
            profile.put("gender", orNull(user.getGender()));
            profile.put("birthYear", orNull(user.getBirthYear()));
            profile.put("createdAt", user.getCreatedAt());
            profile.put("updatedAt", user.getUpdatedAt());
        }
        root.put("profile", profile);

        JSONArray workouts = new JSONArray();
        for (CustomWorkout w : data.getCustomWorkouts()) {
            List<CustomExerciseOrderEntry> entries = data.getExercisesByWorkout()
                    .getOrDefault(w.getId(), Collections.emptyList());
            JSONArray exercises = new JSONArray();
            for (CustomExerciseOrderEntry e : entries) {
                JSONObject o = new JSONObject();
                o.put("exerciseName", e.getExerciseName());
                o.put("position", e.getPosition());
                o.put("sets", orNull(e.getSets()));
                o.put("reps", orNull(e.getReps()));
                o.put("seconds", orNull(e.getSeconds()));
                o.put("restSeconds", orNull(e.getRestSeconds()));
                o.put("restAfterSeconds", orNull(e.getRestAfterSeconds()));
                exercises.put(o);
            }
            JSONObject o = new JSONObject();
            o.put("id", w.getId());
            o.put("name", w.getName());
            o.put("description", orNull(w.getDescription()));
            o.put("exercises", exercises);
            workouts.put(o);
        }
        root.put("customWorkouts", workouts);

        JSONArray logs = new JSONArray();
        for (WorkoutLog l : data.getLogs()) {
            JSONObject o = new JSONObject();
            o.put("id", l.getId());
            o.put("workoutName", l.getWorkoutName());
            o.put("startedAt", l.getStartedAt());
            o.put("durationSeconds", l.getDurationSeconds());
            o.put("kcal", l.getKcal());
            o.put("status", l.getStatus());
            o.put("exercises", WorkoutLogRepository.snapshotToJson(l.getExercises()));
            logs.put(o);
        }
        root.put("workoutLogs", logs);

        JSONArray schedules = new JSONArray();
        for (ScheduledWorkout s : data.getSchedules()) {
            JSONObject o = new JSONObject();
            o.put("id", s.getId());
            o.put("workoutName", orNull(s.getWorkoutName()));
            o.put("customWorkoutId", orNull(s.getCustomWorkoutId()));
            o.put("scheduledAt", s.getScheduledAt());
            o.put("recurrenceRule", orNull(s.getRecurrenceRule()));
            o.put("completedOccurrences", new JSONArray(s.getCompletedOccurrences()));
            schedules.put(o);
        }
        root.put("schedules", schedules);
        return root;
    }

    private static Object orNull(Object value) {
        return value == null ? JSONObject.NULL : value;
    }
}
