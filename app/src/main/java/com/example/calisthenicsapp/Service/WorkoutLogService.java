package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The signed-in user's finished workouts (users/{uid}/workoutLogs). A log is saved from a
 * {@link WorkoutSession}, built by WorkoutSessionService.
 */
public class WorkoutLogService extends UserDataService {

    public WorkoutLogService(Context context) {
        super(context);
    }

    /** Newest first. */
    public void getMyLogs(ServiceCallback<List<WorkoutLog>> callback) {
        readForUser(callback, uid -> new WorkoutLogRepository(db()).getByUid(uid));
    }

    /**
     * Saves a finished or cancelled session as a log (FR-5.3 to FR-5.6): the workout name and
     * a snapshot of every exercise as done, so the log survives later edits or deletion of the
     * workout. Fails if the session is still going or no set was completed.
     * Saving the same session again returns the same log id instead of logging it twice,
     * also after the session was restored from saved instance state. Works offline.
     * On success, returns the log id.
     */
    public void saveSession(WorkoutSession session, ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (session == null || !session.isOver()) {
            fail(callback, "Finish or cancel the workout before saving it.");
            return;
        }
        if (session.getLogId() != null) {
            succeed(callback, session.getLogId());
            return;
        }
        if (!session.isLoggable()) {
            fail(callback, "No set was completed, so there is nothing to save.");
            return;
        }

        List<Map<String, Object>> exercises = new ArrayList<>();
        for (WorkoutSession.SessionExercise e : session.getExercises()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("exerciseName", e.getName());
            entry.put("met", e.getMet());
            entry.put("timed", e.isTimed());
            entry.put("sets", e.getSets());
            entry.put("completedSets", e.getCompletedSets());
            entry.put("reps", e.getTargetReps());
            entry.put("seconds", e.getTargetSeconds());
            entry.put("activeSeconds", e.getActiveSeconds());
            entry.put("kcal", e.getKcal(session.getWeightKg()));
            exercises.add(entry);
        }

        DocumentReference doc = userDoc(uid).collection("workoutLogs").document();
        Map<String, Object> data = new HashMap<>();
        data.put("workoutName", session.getWorkoutName());
        data.put("startedAt", session.getStartedAt());
        data.put("durationSeconds", session.getTotalSeconds());
        data.put("kcal", session.getTotalKcal());
        data.put("status", session.getStatus());
        data.put("exercises", exercises);
        session.setLogId(doc.getId());
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
