package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Repos.ExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;

import java.util.List;

/**
 * Read-only access to the premade workouts (FR-4.1, FR-4.2). Premade workouts cannot be
 * edited (FR-4.3); to change one, copy it with {@link CustomWorkoutService#duplicatePremade}.
 *
 * The premade workouts are stored only in the local SQLite database, so these calls work
 * offline and do not need a signed-in user. Every callback is delivered on the main thread.
 */
public class WorkoutService extends UserDataService {

    public WorkoutService(Context context) {
        super(context);
    }

    /** Every premade workout, sorted by name. */
    public void getAllWorkouts(ServiceCallback<List<Workout>> callback) {
        onDb(() -> succeed(callback, new WorkoutRepository(db()).getAll()));
    }

    /** Returns null if there is no premade workout with that name. */
    public void getWorkout(String name, ServiceCallback<Workout> callback) {
        onDb(() -> succeed(callback, name == null ? null : new WorkoutRepository(db()).getOne(name)));
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
