package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.ExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.ExerciseRepository;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Session.WorkoutSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Starts workout sessions (FR-5.1). The returned {@link WorkoutSession} is already running;
 * the screen drives it from there and saves it with WorkoutLogService.saveSession when it is over.
 *
 * Everything is read from SQLite, so starting works offline. The user's weight comes from their
 * profile; if it is not set, {@link #DEFAULT_WEIGHT_KG} is used for the calorie estimate.
 */
public class WorkoutSessionService extends UserDataService {
    public static final double DEFAULT_WEIGHT_KG = 70;

    // Fallbacks for an exercise that has no default of its own.
    private static final int FALLBACK_REPS = 10;
    private static final int FALLBACK_SECONDS = 30;

    public WorkoutSessionService(Context context) {
        super(context);
    }

    public void startPremade(String workoutName, ServiceCallback<WorkoutSession> callback) {
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
            ExerciseRepository exerciseRepository = new ExerciseRepository(db());
            List<WorkoutSession.SessionExercise> exercises = new ArrayList<>();
            for (ExerciseOrderEntry e : new ExerciseOrderRepository(db()).getByWorkout(workoutName)) {
                exercises.add(toSessionExercise(exerciseRepository.getOne(e.getExerciseName()),
                        e.getSets(), e.getReps(), e.getSeconds(), e.getRestSeconds()));
            }
            start(uid, workoutName, exercises, callback);
        });
    }

    public void startCustom(String customWorkoutId, ServiceCallback<WorkoutSession> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            CustomWorkout workout = customWorkoutId == null
                    ? null : new CustomWorkoutRepository(db()).getOne(customWorkoutId);
            if (workout == null || !uid.equals(workout.getUid())) {
                fail(callback, "That custom workout no longer exists.");
                return;
            }
            ExerciseRepository exerciseRepository = new ExerciseRepository(db());
            List<WorkoutSession.SessionExercise> exercises = new ArrayList<>();
            for (CustomExerciseOrderEntry e
                    : new CustomExerciseOrderRepository(db()).getByCustomWorkout(customWorkoutId)) {
                exercises.add(toSessionExercise(exerciseRepository.getOne(e.getExerciseName()),
                        e.getSets(), e.getReps(), e.getSeconds(), e.getRestSeconds()));
            }
            start(uid, workout.getName(), exercises, callback);
        });
    }

    private void start(String uid, String workoutName, List<WorkoutSession.SessionExercise> exercises,
                       ServiceCallback<WorkoutSession> callback) {
        if (exercises.isEmpty()) {
            fail(callback, "This workout has no exercises.");
            return;
        }
        User user = new UserRepository(db()).getOne(uid);
        Double weight = user == null ? null : user.getWeight();
        double weightKg = weight == null || weight <= 0 ? DEFAULT_WEIGHT_KG : weight;
        succeed(callback, new WorkoutSession(workoutName, exercises, weightKg));
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
}
