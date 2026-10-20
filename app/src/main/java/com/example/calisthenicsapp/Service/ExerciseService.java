package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Equipment;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.Repos.ExerciseRepository;

import java.util.List;

/**
 * Read-only access to the premade exercise library (FR-3.3, FR-3.4).
 *
 * The library is stored only in the local SQLite database, so these calls work offline and
 * do not need a signed-in user. Like the per-user services, the SQLite work runs on the shared
 * background thread and every callback is delivered on the main thread (see {@link DbService}).
 */
public class ExerciseService extends DbService {

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
     * Exercises matching all the given filters, sorted by name. Pass null (or "" for the text
     * filters) to skip a filter. nameQuery matches anywhere in the name ("push" finds "Knee
     * Push-Up") and muscleGroup must match a whole value, both ignoring case. For the difficulty
     * and equipment filter options, use Difficulty.values() (easiest first) and
     * Equipment.values() (NONE first) with getLabel(). Equipment.NONE finds the exercises that
     * need nothing but the floor.
     */
    public void searchExercises(String nameQuery, String muscleGroup, Difficulty difficulty,
                                Equipment equipment, ServiceCallback<List<Exercise>> callback) {
        String name = blankToNull(nameQuery);
        String group = blankToNull(muscleGroup);
        onDb(() -> succeed(callback,
                new ExerciseRepository(db()).search(name, group, difficulty, equipment)));
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
