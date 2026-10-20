package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Repos.ExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Session.WorkoutExercises;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Read-only access to the premade workouts (FR-4.1, FR-4.2). Premade workouts cannot be
 * edited (FR-4.3); to change one, copy it with {@link CustomWorkoutService#duplicatePremade}.
 *
 * The premade workouts are stored only in the local SQLite database, so these calls work
 * offline and do not need a signed-in user. Every callback is delivered on the main thread.
 */
public class WorkoutService extends DbService {

    public WorkoutService(Context context) {
        this(context, appDatabase(context));
    }

    /** With a database of your own, e.g. in a test; see {@link DbService}. */
    public WorkoutService(Context context, Supplier<SQLiteDatabase> database) {
        super(context, database);
    }

    /** Every premade workout, sorted by name, with its estimated length. */
    public void getAllWorkouts(ServiceCallback<List<Workout>> callback) {
        onDb(() -> {
            List<Workout> workouts = new ArrayList<>();
            for (Workout w : new WorkoutRepository(db()).getAll()) workouts.add(withEstimate(w));
            succeed(callback, workouts);
        });
    }

    /** Returns null if there is no premade workout with that name. */
    public void getWorkout(String name, ServiceCallback<Workout> callback) {
        onDb(() -> {
            Workout workout = name == null ? null : new WorkoutRepository(db()).getOne(name);
            succeed(callback, workout == null ? null : withEstimate(workout));
        });
    }

    /** The same estimate as a scheduled workout's calendar event (FR-4.1). */
    private Workout withEstimate(Workout w) {
        int seconds = WorkoutExercises.estimateSeconds(WorkoutExercises.ofPremade(db(), w.getName()));
        return new Workout(w.getName(), w.getLevel(), w.getCategory(), w.getDescription(), seconds);
    }

    /**
     * The workout's exercises in order. A null reps or seconds means "use the exercise's
     * default" (Exercise.getDefaultReps / getDefaultSeconds).
     */
    public void getExercises(String workoutName, ServiceCallback<List<ExerciseOrderEntry>> callback) {
        onDb(() -> succeed(callback,
                new ExerciseOrderRepository(db()).getByWorkout(workoutName)));
    }
}
