package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The signed-in user's planned workouts (users/{uid}/schedules). A schedule points at exactly
 * one premade workout (by name) or one custom workout (by id). Times are epoch seconds.
 */
public class ScheduleService extends UserDataService {

    public ScheduleService(Context context) {
        super(context);
    }

    /** Soonest first. */
    public void getMySchedules(ServiceCallback<List<ScheduledWorkout>> callback) {
        readForUser(callback, uid -> new ScheduledWorkoutRepository(db()).getByUid(uid));
    }

    /** Schedules from now on, soonest first. */
    public void getUpcoming(ServiceCallback<List<ScheduledWorkout>> callback) {
        long from = now();
        readForUser(callback, uid -> new ScheduledWorkoutRepository(db()).getUpcoming(uid, from));
    }

    /** On success, returns the new schedule's id. */
    public void schedulePremade(String workoutName, long scheduledAt, ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            if (workoutName == null || new WorkoutRepository(db()).getOne(workoutName) == null) {
                fail(callback, "Unknown workout: " + workoutName);
                return;
            }
            add(uid, "workoutName", workoutName, scheduledAt, callback);
        });
    }

    /** On success, returns the new schedule's id. */
    public void scheduleCustom(String customWorkoutId, long scheduledAt, ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            if (customWorkoutId == null
                    || new CustomWorkoutRepository(db()).getOne(customWorkoutId) == null) {
                fail(callback, "That custom workout no longer exists.");
                return;
            }
            add(uid, "customWorkoutId", customWorkoutId, scheduledAt, callback);
        });
    }

    public void reschedule(String scheduleId, long newScheduledAt, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (newScheduledAt <= 0) {
            fail(callback, "Pick a date and time.");
            return;
        }
        onDb(() -> {
            if (scheduleId == null || new ScheduledWorkoutRepository(db()).getOne(scheduleId) == null) {
                fail(callback, "That scheduled workout no longer exists.");
                return;
            }
            watchWrite(schedules(uid).document(scheduleId).update("scheduledAt", newScheduledAt),
                    "Rescheduling " + scheduleId);
            succeed(callback, null);
        });
    }

    public void delete(String scheduleId, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (scheduleId == null) {
            fail(callback, "That scheduled workout no longer exists.");
            return;
        }
        watchWrite(schedules(uid).document(scheduleId).delete(), "Deleting schedule " + scheduleId);
        succeed(callback, null);
    }

    /** Writes only the one workout field, so the document has exactly one of the two. */
    private void add(String uid, String workoutField, String workoutValue, long scheduledAt,
                     ServiceCallback<String> callback) {
        if (scheduledAt <= 0) {
            fail(callback, "Pick a date and time.");
            return;
        }
        DocumentReference doc = schedules(uid).document();
        Map<String, Object> data = new HashMap<>();
        data.put(workoutField, workoutValue);
        data.put("scheduledAt", scheduledAt);
        watchWrite(doc.set(data), "Creating schedule " + doc.getId());
        succeed(callback, doc.getId());
    }

    private CollectionReference schedules(String uid) {
        return userDoc(uid).collection("schedules");
    }
}
