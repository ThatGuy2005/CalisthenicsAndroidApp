package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.StatsPeriod;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.example.calisthenicsapp.Scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.Scheduling.ScheduleOccurrence;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * The signed-in user's finished workouts (users/{uid}/workoutLogs). A log is saved from a
 * {@link WorkoutSession}, built by WorkoutSessionService.
 */
public class WorkoutLogService extends UserDataService {

    public WorkoutLogService(Context context) {
        super(context);
    }

    /** All of them, newest first. */
    public void getMyLogs(ServiceCallback<List<WorkoutLog>> callback) {
        getMyLogs(StatsPeriod.ALL_TIME, callback);
    }

    /**
     * The logs of this week, this month or all time (a null period), newest first: exactly the
     * logs StatsService.getStats counts for the same period (FR-5.7).
     */
    public void getMyLogs(StatsPeriod period, ServiceCallback<List<WorkoutLog>> callback) {
        long from = (period == null ? StatsPeriod.ALL_TIME : period).startSeconds();
        readForUser(callback, uid -> new WorkoutLogRepository(db()).getByUidSince(uid, from));
    }

    /**
     * Saves a finished or cancelled session as a log (FR-5.3 to FR-5.6): the workout name and
     * a snapshot of every exercise as done, so the log survives later edits or deletion of the
     * workout. Fails if the session is still going or no set was completed.
     * Saving the same session again returns the same log id instead of logging it twice,
     * also after the session was restored from saved instance state. Works offline.
     * A Completed session also completes the scheduled workout it was planned as, if any
     * (FR-6.1, see {@link ScheduleOccurrence#matching}); a Partial one does not.
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
            entry.put("perSide", e.isPerSide());
            entry.put("activeSeconds", e.getActiveSeconds());
            entry.put("kcal", e.getKcal(session.getKcalPerMetHour()));
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
        if (!WorkoutSession.STATUS_COMPLETED.equals(session.getStatus())) {
            succeed(callback, doc.getId());
            return;
        }
        onDb(() -> {
            completeMatchingSchedule(uid, session);
            succeed(callback, doc.getId());
        });
    }

    /** Must run inside onDb. */
    private void completeMatchingSchedule(String uid, WorkoutSession session) {
        long started = session.getStartedAt();
        long window = OccurrenceStatus.MISSED_AFTER_SECONDS;
        ScheduleOccurrence match = ScheduleOccurrence.matching(
                new ScheduledWorkoutRepository(db()).getBetween(uid, started - window, started + window + 1),
                session.getWorkoutName(), session.getCustomWorkoutId(), started, TimeZone.getDefault());
        if (match == null) return;
        String scheduleId = match.getSchedule().getId();
        watchWrite(userDoc(uid).collection("schedules").document(scheduleId)
                        .update("completed", FieldValue.arrayUnion(match.getStartSeconds())),
                "Completing schedule " + scheduleId + " at " + match.getStartSeconds());
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
