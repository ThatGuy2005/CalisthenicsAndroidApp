package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.Repos.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.ExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.ExerciseRepository;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The signed-in user's custom workouts (users/{uid}/customWorkouts).
 *
 * Exercise lists are passed as CustomExerciseOrderEntry objects. Their customWorkoutId and
 * position are ignored: the position is taken from the list order (first = 1). A null sets or
 * restSeconds means the default (3 sets, 60 s); a null reps or seconds means "use the exercise's
 * default", as in the premade workouts. restSeconds is the rest between the sets of an exercise;
 * restAfterSeconds is the rest after its last set, before the next exercise, and null means the
 * same as restSeconds (FR-4.2).
 */
public class CustomWorkoutService extends UserDataService {
    private static final int NAME_MAX = 40;
    /** firestore.rules allows at most this many exercises in a workout or a log. */
    private static final int EXERCISES_MAX = 50;
    private static final int DEFAULT_SETS = 3;
    private static final int DEFAULT_REST_SECONDS = 60;

    public CustomWorkoutService(Context context) {
        super(context);
    }

    public void getMyCustomWorkouts(ServiceCallback<List<CustomWorkout>> callback) {
        readForUser(callback, uid -> new CustomWorkoutRepository(db()).getByUid(uid));
    }

    /** The workout's exercises in order. */
    public void getExercises(String customWorkoutId,
                             ServiceCallback<List<CustomExerciseOrderEntry>> callback) {
        readForUser(callback,
                uid -> new CustomExerciseOrderRepository(db()).getByCustomWorkout(customWorkoutId));
    }

    /** On success, returns the new workout's id. */
    public void create(String name, List<CustomExerciseOrderEntry> exercises,
                       ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        onDb(() -> createOnDb(uid, trimmed, exercises, callback));
    }

    /**
     * Copies a premade workout into a new custom workout that the user can edit (FR-4.3).
     * Pass null as newName to reuse the premade workout's name, made unique with " (2)",
     * " (3)", ... if the user already has a workout called that. On success, returns the new
     * workout's id. Like create, this also works offline.
     */
    public void duplicatePremade(String workoutName, String newName,
                                 ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            if (workoutName == null || new WorkoutRepository(db()).getOne(workoutName) == null) {
                fail(callback, "That workout no longer exists.");
                return;
            }
            List<CustomExerciseOrderEntry> copy = new ArrayList<>();
            for (ExerciseOrderEntry e : new ExerciseOrderRepository(db()).getByWorkout(workoutName)) {
                // customWorkoutId and position are ignored by create; the list order is kept.
                copy.add(new CustomExerciseOrderEntry(null, e.getExerciseName(), e.getPosition(),
                        e.getSets(), e.getReps(), e.getSeconds(), e.getRestSeconds(),
                        e.getRestAfterSeconds()));
            }
            String name = newName == null ? uniqueName(uid, workoutName) : newName.trim();
            createOnDb(uid, name, copy, callback);
        });
    }

    public void rename(String customWorkoutId, String newName, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        String trimmed = newName == null ? "" : newName.trim();
        onDb(() -> {
            String error = checkExists(customWorkoutId);
            if (error == null) error = validateName(uid, trimmed, customWorkoutId);
            if (error != null) {
                fail(callback, error);
                return;
            }
            watchWrite(workouts(uid).document(customWorkoutId)
                            .update("name", trimmed, "updatedAt", now()),
                    "Renaming custom workout " + customWorkoutId);
            succeed(callback, null);
        });
    }

    /** Replaces the whole exercise list (use this for adding, removing and reordering). */
    public void replaceExercises(String customWorkoutId, List<CustomExerciseOrderEntry> exercises,
                                 ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            String error = checkExists(customWorkoutId);
            if (error == null) error = validateExercises(exercises);
            if (error != null) {
                fail(callback, error);
                return;
            }
            watchWrite(workouts(uid).document(customWorkoutId)
                            .update("exercises", toRemote(exercises), "updatedAt", now()),
                    "Updating exercises of custom workout " + customWorkoutId);
            succeed(callback, null);
        });
    }

    /** Deletes the workout and every schedule that points at it, in one batch. */
    public void delete(String customWorkoutId, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            String error = checkExists(customWorkoutId);
            if (error != null) {
                fail(callback, error);
                return;
            }
            WriteBatch batch = firestore.batch();
            batch.delete(workouts(uid).document(customWorkoutId));
            // Locally the schedules cascade, but in Firestore they would be left dangling.
            for (ScheduledWorkout s : new ScheduledWorkoutRepository(db()).getByUid(uid)) {
                if (customWorkoutId.equals(s.getCustomWorkoutId())) {
                    batch.delete(userDoc(uid).collection("schedules").document(s.getId()));
                }
            }
            watchWrite(batch.commit(), "Deleting custom workout " + customWorkoutId);
            succeed(callback, null);
        });
    }

    /** Shared by create and duplicatePremade; must run inside onDb. */
    private void createOnDb(String uid, String name, List<CustomExerciseOrderEntry> exercises,
                            ServiceCallback<String> callback) {
        String error = validateName(uid, name, null);
        if (error == null) error = validateExercises(exercises);
        if (error != null) {
            fail(callback, error);
            return;
        }
        DocumentReference doc = workouts(uid).document();     // Firestore auto id
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("updatedAt", now());
        data.put("exercises", toRemote(exercises));
        watchWrite(doc.set(data), "Creating custom workout " + doc.getId());
        succeed(callback, doc.getId());
    }

    /** base, or base + " (2)", " (3)", ... : the first one none of the user's workouts uses. */
    private String uniqueName(String uid, String base) {
        Set<String> taken = new HashSet<>();
        for (CustomWorkout w : new CustomWorkoutRepository(db()).getByUid(uid)) {
            taken.add(w.getName().toLowerCase(Locale.ROOT));
        }
        String name = base;
        for (int i = 2; taken.contains(name.toLowerCase(Locale.ROOT)); i++) {
            String suffix = " (" + i + ")";
            String start = base.length() + suffix.length() > NAME_MAX
                    ? base.substring(0, NAME_MAX - suffix.length()).trim() : base;
            name = start + suffix;
        }
        return name;
    }

    private CollectionReference workouts(String uid) {
        return userDoc(uid).collection("customWorkouts");
    }

    private String checkExists(String customWorkoutId) {
        if (customWorkoutId == null
                || new CustomWorkoutRepository(db()).getOne(customWorkoutId) == null) {
            return "That custom workout no longer exists.";
        }
        return null;
    }

    /** ignoreId: the workout being renamed, so keeping its own name is allowed. */
    private String validateName(String uid, String name, String ignoreId) {
        if (name.isEmpty()) return "Give the workout a name.";
        if (name.length() > NAME_MAX) return "The name can be at most " + NAME_MAX + " characters.";
        // The local table has UNIQUE (Uid, Name): a duplicate could never be mirrored into SQLite.
        for (CustomWorkout w : new CustomWorkoutRepository(db()).getByUid(uid)) {
            if (w.getName().equalsIgnoreCase(name) && !w.getId().equals(ignoreId)) {
                return "You already have a workout called \"" + name + "\".";
            }
        }
        return null;
    }

    /** Mirrors the CHECK constraints and the Exercise foreign key of CustomExerciseOrder. */
    private String validateExercises(List<CustomExerciseOrderEntry> exercises) {
        if (exercises == null || exercises.isEmpty()) return "Add at least one exercise.";
        if (exercises.size() > EXERCISES_MAX) {
            return "A workout can have at most " + EXERCISES_MAX + " exercises.";
        }
        ExerciseRepository exerciseRepository = new ExerciseRepository(db());
        for (CustomExerciseOrderEntry e : exercises) {
            if (e.getExerciseName() == null || exerciseRepository.getOne(e.getExerciseName()) == null) {
                return "Unknown exercise: " + e.getExerciseName();
            }
            if (e.getSets() != null && e.getSets() <= 0) return "Sets must be at least 1.";
            if (e.getReps() != null && e.getReps() <= 0) return "Reps must be at least 1.";
            if (e.getSeconds() != null && e.getSeconds() <= 0) return "Seconds must be at least 1.";
            if (e.getRestSeconds() != null && e.getRestSeconds() < 0) return "Rest cannot be negative.";
            if (e.getRestAfterSeconds() != null && e.getRestAfterSeconds() < 0) {
                return "Rest cannot be negative.";
            }
        }
        return null;
    }

    private static List<Map<String, Object>> toRemote(List<CustomExerciseOrderEntry> exercises) {
        List<Map<String, Object>> list = new ArrayList<>();
        int position = 1;
        for (CustomExerciseOrderEntry e : exercises) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("exerciseName", e.getExerciseName());
            entry.put("position", position++);
            entry.put("sets", e.getSets() == null ? DEFAULT_SETS : e.getSets());
            entry.put("reps", e.getReps());                       // null = exercise default
            entry.put("seconds", e.getSeconds());                 // null = exercise default
            entry.put("restSeconds", e.getRestSeconds() == null
                    ? DEFAULT_REST_SECONDS : e.getRestSeconds());
            entry.put("restAfterSeconds", e.getRestAfterSeconds());     // null = restSeconds
            list.add(entry);
        }
        return list;
    }
}
