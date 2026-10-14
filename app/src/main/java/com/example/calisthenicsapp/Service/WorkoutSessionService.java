package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Session.WorkoutExercises;
import com.example.calisthenicsapp.Session.WorkoutSession;

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
            start(uid, workoutName, null, WorkoutExercises.ofPremade(db(), workoutName), callback);
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
            start(uid, workout.getName(), customWorkoutId,
                    WorkoutExercises.ofCustom(db(), customWorkoutId), callback);
        });
    }

    private void start(String uid, String workoutName, String customWorkoutId,
                       List<WorkoutSession.SessionExercise> exercises,
                       ServiceCallback<WorkoutSession> callback) {
        if (exercises.isEmpty()) {
            fail(callback, "This workout has no exercises.");
            return;
        }
        User user = new UserRepository(db()).getOne(uid);
        Double weight = user == null ? null : user.getWeight();
        double weightKg = weight == null || weight <= 0 ? DEFAULT_WEIGHT_KG : weight;
        succeed(callback, new WorkoutSession(workoutName, customWorkoutId, exercises, weightKg));
    }
}
