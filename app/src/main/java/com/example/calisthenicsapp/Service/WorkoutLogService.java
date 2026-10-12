package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The signed-in user's finished workouts (users/{uid}/workoutLogs). */
public class WorkoutLogService extends UserDataService {

    public WorkoutLogService(Context context) {
        super(context);
    }

    /**
     * Calories burnt doing an activity: MET x body weight (kg) x hours.
     * Sum this over the exercises of a workout to get the kcal for {@link #logWorkout}.
     */
    public static double estimateKcal(double met, double weightKg, double seconds) {
        return met * weightKg * (seconds / 3600.0);
    }

    /** Newest first. */
    public void getMyLogs(ServiceCallback<List<WorkoutLog>> callback) {
        readForUser(callback, uid -> new WorkoutLogRepository(db()).getByUid(uid));
    }

    /**
     * Saves a finished workout. workoutName is stored as plain text, so the log survives the
     * workout being deleted later. startedAt is in epoch seconds. On success, returns the log id.
     */
    public void logWorkout(String workoutName, long startedAt, int durationSeconds, double kcal,
                           ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        // Same rules as the CHECK constraints on WorkoutLog.
        if (workoutName == null || workoutName.trim().isEmpty()) {
            fail(callback, "The workout has no name.");
            return;
        }
        if (startedAt <= 0) {
            fail(callback, "The start time is missing.");
            return;
        }
        if (durationSeconds < 0 || kcal < 0 || Double.isNaN(kcal)) {
            fail(callback, "Duration and calories cannot be negative.");
            return;
        }

        DocumentReference doc = userDoc(uid).collection("workoutLogs").document();
        Map<String, Object> data = new HashMap<>();
        data.put("workoutName", workoutName.trim());
        data.put("startedAt", startedAt);
        data.put("durationSeconds", durationSeconds);
        data.put("kcal", kcal);
        watchWrite(doc.set(data), "Saving workout log " + doc.getId());
        succeed(callback, doc.getId());
    }

    public void delete(String logId, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (logId == null) {
            fail(callback, "That workout log no longer exists.");
            return;
        }
        watchWrite(userDoc(uid).collection("workoutLogs").document(logId).delete(),
                "Deleting workout log " + logId);
        succeed(callback, null);
    }
}
