package com.example.calisthenicsapp.sync;

import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.model.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.model.CustomWorkout;
import com.example.calisthenicsapp.model.ScheduledWorkout;
import com.example.calisthenicsapp.model.User;
import com.example.calisthenicsapp.model.WorkoutLog;
import com.example.calisthenicsapp.repository.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.repository.CustomWorkoutRepository;
import com.example.calisthenicsapp.repository.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.repository.UserRepository;
import com.example.calisthenicsapp.repository.WorkoutLogRepository;
import com.example.calisthenicsapp.scheduling.ScheduleCalendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The SQLite side of the sync for one signed-in user: writes what Firestore holds into the
 * repositories. SyncManager listens to Firestore and hands every snapshot here; it makes a new
 * LocalMirror at each sign-in.
 *
 * Each apply method takes a snapshot's added or changed documents, the ids of its removed ones,
 * and serverIds: the ids of every document in the collection if the server confirmed the
 * snapshot, or null if it came from Firestore's cache. With a full list, local rows missing from
 * it are deleted as well (see SyncManager for why). They return whether SQLite changed.
 *
 * Schedules can have an event in the device calendar (FR-6.2). When a schedule's time or repeat
 * rule changes or it is removed, from this device or another, the event is moved or deleted
 * here, so the calendar follows Firestore too.
 *
 * Not thread-safe: SyncManager only calls it on its one SQLite thread.
 */
final class LocalMirror {
    private final String uid;
    private final UserRepository users;
    private final CustomWorkoutRepository customWorkouts;
    private final CustomExerciseOrderRepository exerciseOrders;
    private final WorkoutLogRepository workoutLogs;
    private final ScheduledWorkoutRepository schedules;
    private final ScheduleCalendar calendar;

    /**
     * Schedules whose custom workout has not been mirrored yet (the two collections' snapshots
     * arrive in any order, and the foreign key would reject them). Keyed by schedule id, flushed
     * when that custom workout arrives.
     */
    private final Map<String, ScheduledWorkout> pendingSchedules = new HashMap<>();

    LocalMirror(String uid, SQLiteDatabase sqlite, ScheduleCalendar calendar) {
        this.uid = uid;
        this.users = new UserRepository(sqlite);
        this.customWorkouts = new CustomWorkoutRepository(sqlite);
        this.exerciseOrders = new CustomExerciseOrderRepository(sqlite);
        this.workoutLogs = new WorkoutLogRepository(sqlite);
        this.schedules = new ScheduledWorkoutRepository(sqlite);
        this.calendar = calendar;
    }

    /** The profile. Its Users row must exist before anything else can be mirrored. */
    void applyUser(RemoteUser remote) {
        User user = new User(remote.uid, remote.username, remote.email,
                remote.height == null ? 0 : remote.height,
                remote.weight == null ? 0 : remote.weight,
                remote.gender, remote.birthYear, remote.createdAt, remote.updatedAt);
        if (users.getOne(remote.uid) == null) {
            users.insert(user);
        } else {
            users.update(user);
        }
    }

    boolean applyCustomWorkouts(List<RemoteCustomWorkout> changed, List<String> removed,
                                Set<String> serverIds) {
        for (String id : removed) {
            CustomWorkout existing = customWorkouts.getOne(id);
            if (existing != null) customWorkouts.delete(existing);   // cascades its exercise rows
        }
        for (RemoteCustomWorkout remote : changed) putCustomWorkout(remote);
        boolean dirty = !changed.isEmpty() || !removed.isEmpty();
        if (serverIds != null) {
            for (CustomWorkout workout : customWorkouts.getByUid(uid)) {
                if (!serverIds.contains(workout.getId())) {
                    customWorkouts.delete(workout);
                    dirty = true;
                }
            }
        }
        return dirty;
    }

    boolean applyWorkoutLogs(List<RemoteWorkoutLog> changed, List<String> removed,
                             Set<String> serverIds) {
        for (String id : removed) {
            WorkoutLog existing = workoutLogs.getOne(id);
            if (existing != null) workoutLogs.delete(existing);
        }
        for (RemoteWorkoutLog remote : changed) {
            WorkoutLog log = new WorkoutLog(remote.id, uid, remote.workoutName, remote.startedAt,
                    remote.durationSeconds, remote.kcal, remote.status, remote.exercises);
            if (workoutLogs.getOne(remote.id) == null) {
                workoutLogs.insert(log);
            } else {
                workoutLogs.update(log);
            }
        }
        boolean dirty = !changed.isEmpty() || !removed.isEmpty();
        if (serverIds != null) {
            for (WorkoutLog log : workoutLogs.getByUid(uid)) {
                if (!serverIds.contains(log.getId())) {
                    workoutLogs.delete(log);
                    dirty = true;
                }
            }
        }
        return dirty;
    }

    boolean applySchedules(List<RemoteSchedule> changed, List<String> removed,
                           Set<String> serverIds) {
        for (String id : removed) removeSchedule(id);
        for (RemoteSchedule remote : changed) {
            ScheduledWorkout schedule = new ScheduledWorkout(remote.id, uid, remote.workoutName,
                    remote.customWorkoutId, remote.scheduledAt, remote.recurrenceRule,
                    remote.completed, null);
            if (remote.customWorkoutId != null && customWorkouts.getOne(remote.customWorkoutId) == null) {
                pendingSchedules.put(remote.id, schedule);   // its custom workout is not mirrored yet
                continue;
            }
            pendingSchedules.remove(remote.id);
            upsertSchedule(schedule);
        }
        boolean dirty = !changed.isEmpty() || !removed.isEmpty();
        if (serverIds != null) {
            for (ScheduledWorkout schedule : schedules.getByUid(uid)) {
                if (!serverIds.contains(schedule.getId())) {
                    removeSchedule(schedule.getId());
                    dirty = true;
                }
            }
            pendingSchedules.keySet().retainAll(serverIds);
        }
        return dirty;
    }

    /** On sign-out: deleting the Users row cascades CustomWorkout, WorkoutLog and ScheduledWorkout. */
    void clear() {
        pendingSchedules.clear();
        User cached = users.getOne(uid);
        if (cached != null) users.delete(cached);
    }

    private void putCustomWorkout(RemoteCustomWorkout remote) {
        CustomWorkout workout = new CustomWorkout(remote.id, remote.name, uid, remote.description,
                null);
        if (customWorkouts.getOne(remote.id) == null) {
            customWorkouts.insert(workout);
        } else {
            customWorkouts.update(workout);
        }

        List<CustomExerciseOrderEntry> entries = new ArrayList<>();
        for (RemoteExerciseEntry e : remote.exercises) {
            entries.add(new CustomExerciseOrderEntry(remote.id, e.exerciseName, e.position,
                    e.sets, e.reps, e.seconds, e.restSeconds, e.restAfterSeconds));
        }
        exerciseOrders.replaceForCustomWorkout(remote.id, entries);

        if (customWorkouts.getOne(remote.id) != null) flushPendingSchedules(remote.id);
    }

    /** Mirrors the schedules that were waiting for this custom workout to arrive. */
    private void flushPendingSchedules(String customWorkoutId) {
        Iterator<ScheduledWorkout> it = pendingSchedules.values().iterator();
        while (it.hasNext()) {
            ScheduledWorkout schedule = it.next();
            if (customWorkoutId.equals(schedule.getCustomWorkoutId())) {
                it.remove();
                upsertSchedule(schedule);
            }
        }
    }

    /** Deletes a schedule that no longer exists: its row and its device calendar event. */
    private void removeSchedule(String scheduleId) {
        pendingSchedules.remove(scheduleId);
        ScheduledWorkout existing = schedules.getOne(scheduleId);
        if (existing != null) schedules.delete(existing);
        calendar.remove(scheduleId);
    }

    private void upsertSchedule(ScheduledWorkout schedule) {
        ScheduledWorkout existing = schedules.getOne(schedule.getId());
        if (existing == null) {
            schedules.insert(schedule);
            return;
        }
        schedules.update(schedule);
        // Metadata-only changes arrive here too, so act only on a real change.
        boolean changed = existing.getScheduledAt() != schedule.getScheduledAt()
                || !Objects.equals(existing.getRecurrenceRule(), schedule.getRecurrenceRule());
        if (changed && existing.getCalendarEventId() != null) {
            calendar.update(schedule, existing.getCalendarEventId());
        }
    }
}
