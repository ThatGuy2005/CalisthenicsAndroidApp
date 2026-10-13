package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.Repos.ExerciseRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Read-only access to the premade exercise library (FR-3.3, FR-3.4).
 *
 * The library is stored only in the local SQLite database, so these calls work offline and
 * do not need a signed-in user. Like the per-user services, the SQLite work runs on the shared
 * background thread and every callback is delivered on the main thread.
 */
public class ExerciseService extends UserDataService {

    /** The allowed difficulty values, easiest first, for building the filter options. */
    public static final List<String> DIFFICULTIES = Collections.unmodifiableList(
            Arrays.asList("Beginner", "Intermediate", "Advanced"));

    public ExerciseService(Context context) {
        super(context);
    }

    /** Every exercise, sorted by name. */
    public void getAllExercises(ServiceCallback<List<Exercise>> callback) {
        onDb(() -> succeed(callback, new ExerciseRepository(db()).getAll()));
    }

    /** For the detail screen. Returns null if there is no exercise with that name. */
    public void getExercise(String name, ServiceCallback<Exercise> callback) {
        onDb(() -> succeed(callback, name == null ? null : new ExerciseRepository(db()).getOne(name)));
    }

    /** The muscle groups that appear in the library, sorted, for the filter options. */
    public void getMuscleGroups(ServiceCallback<List<String>> callback) {
        onDb(() -> succeed(callback, new ExerciseRepository(db()).getMuscleGroups()));
    }

    /**
     * Exercises matching all the given filters, sorted by name. Pass null or "" to skip a filter.
     * nameQuery matches anywhere in the name ("push" finds "Knee Push-Up"); muscleGroup and
     * difficulty must match a whole value. None of them are case-sensitive.
     */
    public void searchExercises(String nameQuery, String muscleGroup, String difficulty,
                                ServiceCallback<List<Exercise>> callback) {
        String name = blankToNull(nameQuery);
        String group = blankToNull(muscleGroup);
        String level = blankToNull(difficulty);
        if (level != null && !isDifficulty(level)) {
            fail(callback, "Difficulty must be Beginner, Intermediate or Advanced.");
            return;
        }
        onDb(() -> succeed(callback, new ExerciseRepository(db()).search(name, group, level)));
    }

    private static boolean isDifficulty(String value) {
        for (String d : DIFFICULTIES) {
            if (d.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
