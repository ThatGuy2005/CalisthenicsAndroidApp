package com.example.calisthenicsapp.ui.schedule;

import android.content.Context;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.model.CustomWorkout;
import com.example.calisthenicsapp.model.Workout;
import com.example.calisthenicsapp.service.CustomWorkoutService;
import com.example.calisthenicsapp.service.ServiceCallback;
import com.example.calisthenicsapp.service.WorkoutService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The workouts the "Plan a workout" dialog offers: the premade ones first, then the user's own.
 * Also gives the schedule cards the names of custom workouts. Main thread only.
 */
final class WorkoutChoices {
    /** One entry of the workout spinner; toString is what the spinner shows. */
    static final class Option {
        /** The workout's name: the premade one's key, or the custom one's name. */
        final String name;
        /** The custom workout's id. */
        final String customWorkoutId;   // null for a premade workout
        private final String label;

        private Option(String name, String customWorkoutId, String label) {
            this.name = name;
            this.customWorkoutId = customWorkoutId;
            this.label = label;
        }

        /** True for one of the user's own workouts. */
        boolean isCustom() {
            return customWorkoutId != null;
        }

        @Override public String toString() {
            return label;
        }
    }

    private final Context context;
    private final WorkoutService workoutService;
    private final CustomWorkoutService customWorkoutService;
    private final List<Option> options = new ArrayList<>();
    /** A load has finished at least once. */
    private boolean loaded;
    /** A load is under way. */
    private boolean loading;
    /** Something changed during the load: load again after it. */
    private boolean reload;

    /** context is for the option labels. */
    WorkoutChoices(Context context, WorkoutService workoutService,
                   CustomWorkoutService customWorkoutService) {
        this.context = context;
        this.workoutService = workoutService;
        this.customWorkoutService = customWorkoutService;
    }

    /** Whether a load has finished, so get() holds the workouts. */
    boolean isLoaded() {
        return loaded;
    }

    /** A copy of the choices as last loaded. */
    List<Option> get() {
        return new ArrayList<>(options);
    }

    /** The custom workout's name, or a generic one if it is not among the choices. */
    String customWorkoutName(String id) {
        for (Option option : options) {
            if (option.isCustom() && option.customWorkoutId.equals(id)) return option.name;
        }
        return context.getString(R.string.schedule_custom_workout);
    }

    /**
     * Loads the choices. The results go into their own lists and replace the choices at the end,
     * so they are never half-filled.
     */
    void load() {
        if (loading) {
            // The load under way may have read the old data, so load again once it is done.
            reload = true;
            return;
        }
        loading = true;
        List<Option> premade = new ArrayList<>();
        List<Option> custom = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(2);
        // Runs after each of the two loads; the second one publishes the choices.
        Runnable finished = () -> {
            if (pending.decrementAndGet() > 0) return;
            options.clear();
            options.addAll(premade);
            options.addAll(custom);
            loading = false;
            loaded = true;
            if (reload) {
                reload = false;
                load();
            }
        };
        workoutService.getAllWorkouts(new ServiceCallback<List<Workout>>() {
            @Override public void onSuccess(List<Workout> result) {
                for (Workout workout : result) {
                    premade.add(new Option(workout.getName(), null, workout.getName()));
                }
                finished.run();
            }
            @Override public void onError(String message) { finished.run(); }
        });
        customWorkoutService.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
            @Override public void onSuccess(List<CustomWorkout> result) {
                for (CustomWorkout workout : result) {
                    custom.add(new Option(workout.getName(), workout.getId(),
                            context.getString(R.string.schedule_my_workout_option, workout.getName())));
                }
                finished.run();
            }
            @Override public void onError(String message) { finished.run(); }
        });
    }
}
