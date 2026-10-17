package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Session.Bmr;
import com.example.calisthenicsapp.Session.WorkoutExercises;
import com.example.calisthenicsapp.Session.WorkoutSession;

import java.util.Calendar;
import java.util.List;

/**
 * Starts workout sessions (FR-5.1). The returned {@link WorkoutSession} is already running;
 * the screen drives it from there and saves it with WorkoutLogService.saveSession when it is over.
 *
 * Everything is read from SQLite, so starting works offline. The calorie estimate uses the
 * user's BMR from their profile's weight, height, birth year and gender (FR-1.2, see {@link Bmr});
 * without height or birth year it uses the weight alone, and without a weight
 * {@link #DEFAULT_WEIGHT_KG}.
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
        double kcalPerMetHour = Bmr.kcalPerMetHour(user, weightKg,
                Calendar.getInstance().get(Calendar.YEAR));
        succeed(callback, new WorkoutSession(workoutName, customWorkoutId, exercises, weightKg,
                kcalPerMetHour));
    }
}
