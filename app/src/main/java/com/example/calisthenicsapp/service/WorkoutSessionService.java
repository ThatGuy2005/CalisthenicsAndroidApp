package com.example.calisthenicsapp.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.model.CustomWorkout;
import com.example.calisthenicsapp.model.User;
import com.example.calisthenicsapp.repository.CustomWorkoutRepository;
import com.example.calisthenicsapp.repository.UserRepository;
import com.example.calisthenicsapp.repository.WorkoutRepository;
import com.example.calisthenicsapp.session.Bmr;
import com.example.calisthenicsapp.session.WorkoutExercises;
import com.example.calisthenicsapp.session.WorkoutSession;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.List;
import java.util.function.Supplier;

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
    /** The body weight assumed when the profile has none, for the calorie estimate. */
    public static final double DEFAULT_WEIGHT_KG = 70;

    /** Uses the app's database, FirebaseAuth and Firestore. */
    public WorkoutSessionService(Context context) {
        super(context);
    }

    /** With dependencies of your own, e.g. in a test; see {@link UserDataService}. */
    public WorkoutSessionService(Context context, Supplier<SQLiteDatabase> database,
                                 SessionManager sessionManager, Supplier<FirebaseFirestore> firestore) {
        super(context, database, sessionManager, firestore);
    }

    /** Starts the premade workout with this name (FR-5.1). */
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

    /** Starts one of the user's custom workouts (FR-5.1). */
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

    /** Makes the session with the user's calorie rate. Must run inside onDb. */
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
