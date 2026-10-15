package com.example.calisthenicsapp.Session;

import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.Repos.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.ExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.ExerciseRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a workout's exercises in order, with every target filled in: what a session runs and
 * what a scheduled workout's calendar event and email list. Must run on a background thread.
 */
public final class WorkoutExercises {
    // Fallbacks for an exercise that has no default of its own.
    private static final int FALLBACK_REPS = 10;
    private static final int FALLBACK_SECONDS = 30;

    /** Rough time one rep takes, for the duration estimate. */
    static final int SECONDS_PER_REP = 3;

    private WorkoutExercises() {
    }

    /**
     * How long the exercises are expected to take: every set plus the rests between them, with
     * no rest after the very last set. A rep takes {@link #SECONDS_PER_REP} seconds.
     */
    public static int estimateSeconds(List<WorkoutSession.SessionExercise> exercises) {
        int total = 0;
        for (WorkoutSession.SessionExercise e : exercises) {
            int work = e.isTimed() ? valueOf(e.getTargetSeconds())
                    : valueOf(e.getTargetReps()) * SECONDS_PER_REP;
            total += e.getSets() * (work + e.getRestSeconds());
        }
        if (!exercises.isEmpty()) total -= exercises.get(exercises.size() - 1).getRestSeconds();
        return Math.max(total, 0);
    }

    public static List<WorkoutSession.SessionExercise> ofPremade(SQLiteDatabase db, String workoutName) {
        ExerciseRepository exerciseRepository = new ExerciseRepository(db);
        List<WorkoutSession.SessionExercise> exercises = new ArrayList<>();
        for (ExerciseOrderEntry e : new ExerciseOrderRepository(db).getByWorkout(workoutName)) {
            exercises.add(toSessionExercise(exerciseRepository.getOne(e.getExerciseName()),
                    e.getSets(), e.getReps(), e.getSeconds(), e.getRestSeconds()));
        }
        return exercises;
    }

    public static List<WorkoutSession.SessionExercise> ofCustom(SQLiteDatabase db, String customWorkoutId) {
        ExerciseRepository exerciseRepository = new ExerciseRepository(db);
        List<WorkoutSession.SessionExercise> exercises = new ArrayList<>();
        for (CustomExerciseOrderEntry e
                : new CustomExerciseOrderRepository(db).getByCustomWorkout(customWorkoutId)) {
            exercises.add(toSessionExercise(exerciseRepository.getOne(e.getExerciseName()),
                    e.getSets(), e.getReps(), e.getSeconds(), e.getRestSeconds()));
        }
        return exercises;
    }

    /**
     * Uses the exercise's own default when the workout leaves reps or seconds open, and only
     * the one that fits the exercise: seconds for timed exercises, reps for the others.
     * The foreign keys guarantee the exercise exists.
     */
    private static WorkoutSession.SessionExercise toSessionExercise(
            Exercise exercise, Integer sets, Integer reps, Integer seconds, Integer restSeconds) {
        Integer targetReps = null;
        Integer targetSeconds = null;
        if (exercise.isTimed()) {
            targetSeconds = firstNonNull(seconds, exercise.getDefaultSeconds(), FALLBACK_SECONDS);
        } else {
            targetReps = firstNonNull(reps, exercise.getDefaultReps(), FALLBACK_REPS);
        }
        return new WorkoutSession.SessionExercise(exercise.getName(), exercise.getInstructions(),
                exercise.getAnimationFile(), exercise.isTimed(), targetReps, targetSeconds,
                sets == null ? 1 : sets, restSeconds == null ? 0 : restSeconds, exercise.getMet());
    }

    private static int firstNonNull(Integer first, Integer second, int fallback) {
        if (first != null) return first;
        if (second != null) return second;
        return fallback;
    }

    private static int valueOf(Integer value) {
        return value == null ? 0 : value;
    }
}
