package com.example.calisthenicsapp.Sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.Database.SQLManager;
import com.example.calisthenicsapp.Repos.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.example.calisthenicsapp.Scheduling.DeviceCalendar;
import com.example.calisthenicsapp.Scheduling.WorkoutSummary;
import com.example.calisthenicsapp.Service.SessionManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps the local SQLite cache in step with Firestore for the signed-in user. Firestore is the
 * source of truth: every write goes there, and these snapshot listeners (registered with
 * MetadataChanges.INCLUDE, so pending local writes show up immediately offline too) mirror
 * whatever Firestore holds into the repositories the rest of the app reads from. There is no
 * outbox or dirty-flag tracking here on purpose: the SDK's own offline cache plus these
 * listeners are the whole sync mechanism.
 *
 * Listener order matters: the profile document is mirrored into Users, and only then are the
 * three per-user collections subscribed to, because CustomWorkout, WorkoutLog and
 * ScheduledWorkout all foreign-key to Users(Uid).
 *
 * Schedules can have an event in the device calendar (FR-6.2). When a schedule's time or
 * repeat rule changes or it is removed, from this device or another, the event is moved or deleted here, so the
 * calendar follows Firestore too. Signing out leaves the events in the calendar.
 *
 * Deletions: Firestore normally reports them as REMOVED changes, also for documents deleted while
 * the app was closed (its own offline cache still has them until the server says otherwise).
 * But if the app is killed after Firestore has dropped a document and before it was removed
 * here, or Firestore's cache is reset, no REMOVED ever comes. So every snapshot confirmed by the
 * server is also a full list: local rows missing from it are deleted. Snapshots from the cache
 * are never used for that, because the cache may be incomplete.
 *
 * Threading: listener state (syncedUid, registrations) is changed only on the main thread.
 * SQLite work runs on dbExecutor, and each queued task first checks that its uid is still the
 * synced one, so events queued before a sign-out or user switch never touch the new session.
 */
public class SyncManager {

    private final Context appContext;
    private final SessionManager sessionManager = new SessionManager();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile String syncedUid;
    private boolean childListenersAttached;

    /**
     * Schedules whose custom workout has not been mirrored yet (the two collections' snapshots
     * arrive in any order, and the foreign key would reject them). Keyed by schedule id, flushed
     * when that custom workout arrives. Only touched on dbExecutor.
     */
    private final Map<String, ScheduledWorkout> pendingSchedules = new HashMap<>();

    private ListenerRegistration profileRegistration;
    private ListenerRegistration customWorkoutsRegistration;
    private ListenerRegistration workoutLogsRegistration;
    private ListenerRegistration schedulesRegistration;

    public SyncManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** Call once (e.g. from Application.onCreate()); reacts to every sign-in/out after that. */
    public void start() {
        sessionManager.addAuthStateListener(new SessionManager.SignedInListener() {
            @Override
            public void onSignedIn(FirebaseUser user) {
                startSyncing(user.getUid());
            }

            @Override
            public void onSignedOut() {
                stopSyncing();
            }
        });
    }

    private void startSyncing(String uid) {
        if (uid.equals(syncedUid)) return;   // already syncing this user (e.g. a token refresh)
        stopListeners();
        syncedUid = uid;
        childListenersAttached = false;
        dbExecutor.execute(pendingSchedules::clear);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        SQLiteDatabase sqlite = SQLManager.getInstance(appContext).getWritableDatabase();
        UserRepository userRepository = new UserRepository(sqlite);

        profileRegistration = firestore.collection("users").document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    RemoteUser remote = RemoteUser.fromSnapshot(uid, snapshot);
                    dbExecutor.execute(() -> {
                        if (!uid.equals(syncedUid)) return;   // signed out or switched user since
                        mirrorUser(userRepository, remote);
                        // The Users row now exists; attach the children back on the main thread.
                        mainHandler.post(() -> {
                            if (!uid.equals(syncedUid) || childListenersAttached) return;
                            childListenersAttached = true;
                            attachChildListeners(uid, firestore, sqlite);
                        });
                    });
                });
    }

    private void attachChildListeners(String uid, FirebaseFirestore firestore, SQLiteDatabase sqlite) {
        CustomWorkoutRepository customWorkoutRepository = new CustomWorkoutRepository(sqlite);
        CustomExerciseOrderRepository exerciseOrderRepository = new CustomExerciseOrderRepository(sqlite);
        WorkoutLogRepository workoutLogRepository = new WorkoutLogRepository(sqlite);
        ScheduledWorkoutRepository scheduledWorkoutRepository = new ScheduledWorkoutRepository(sqlite);

        customWorkoutsRegistration = firestore.collection("users").document(uid)
                .collection("customWorkouts")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<DocumentChange> changes = snapshots.getDocumentChanges();
                    Set<String> serverIds = serverIds(snapshots);
                    dbExecutor.execute(() -> {
                        if (!uid.equals(syncedUid)) return;
                        for (DocumentChange change : changes) {
                            onCustomWorkoutChange(sqlite, customWorkoutRepository,
                                    exerciseOrderRepository, scheduledWorkoutRepository, uid, change);
                        }
                        if (serverIds == null) return;
                        for (CustomWorkout workout : customWorkoutRepository.getByUid(uid)) {
                            if (!serverIds.contains(workout.getId())) customWorkoutRepository.delete(workout);
                        }
                    });
                });

        workoutLogsRegistration = firestore.collection("users").document(uid)
                .collection("workoutLogs")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<DocumentChange> changes = snapshots.getDocumentChanges();
                    Set<String> serverIds = serverIds(snapshots);
                    dbExecutor.execute(() -> {
                        if (!uid.equals(syncedUid)) return;
                        for (DocumentChange change : changes) {
                            onWorkoutLogChange(workoutLogRepository, uid, change);
                        }
                        if (serverIds == null) return;
                        for (WorkoutLog log : workoutLogRepository.getByUid(uid)) {
                            if (!serverIds.contains(log.getId())) workoutLogRepository.delete(log);
                        }
                    });
                });

        schedulesRegistration = firestore.collection("users").document(uid)
                .collection("schedules")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<DocumentChange> changes = snapshots.getDocumentChanges();
                    Set<String> serverIds = serverIds(snapshots);
                    dbExecutor.execute(() -> {
                        if (!uid.equals(syncedUid)) return;
                        for (DocumentChange change : changes) {
                            onScheduleChange(sqlite, scheduledWorkoutRepository,
                                    customWorkoutRepository, uid, change);
                        }
                        if (serverIds == null) return;
                        for (ScheduledWorkout schedule : scheduledWorkoutRepository.getByUid(uid)) {
                            if (!serverIds.contains(schedule.getId())) {
                                removeSchedule(scheduledWorkoutRepository, schedule.getId());
                            }
                        }
                        pendingSchedules.keySet().retainAll(serverIds);
                    });
                });
    }

    /**
     * The ids of all documents in the snapshot if the server confirmed it, or null if it comes
     * from Firestore's cache, which may be incomplete and must not be used to delete anything.
     * Documents written on this device but not yet sent are included either way.
     */
    private static Set<String> serverIds(QuerySnapshot snapshots) {
        if (snapshots.getMetadata().isFromCache()) return null;
        Set<String> ids = new HashSet<>();
        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            ids.add(doc.getId());
        }
        return ids;
    }

    private void mirrorUser(UserRepository userRepository, RemoteUser remote) {
        User user = new User(remote.uid, remote.username, remote.email,
                remote.height == null ? 0 : remote.height,
                remote.weight == null ? 0 : remote.weight,
                remote.gender, remote.createdAt, remote.updatedAt);
        if (userRepository.getOne(remote.uid) == null) {
            userRepository.insert(user);
        } else {
            userRepository.update(user);
        }
    }

    private void onCustomWorkoutChange(SQLiteDatabase sqlite,
                                        CustomWorkoutRepository workoutRepository,
                                        CustomExerciseOrderRepository exerciseOrderRepository,
                                        ScheduledWorkoutRepository scheduledWorkoutRepository,
                                        String uid, DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            CustomWorkout existing = workoutRepository.getOne(doc.getId());
            if (existing != null) workoutRepository.delete(existing);   // cascades its exercise rows
            return;
        }
        RemoteCustomWorkout remote = RemoteCustomWorkout.fromSnapshot(doc);
        CustomWorkout workout = new CustomWorkout(remote.id, remote.name, uid);
        if (workoutRepository.getOne(remote.id) == null) {
            workoutRepository.insert(workout);
        } else {
            workoutRepository.update(workout);
        }

        List<CustomExerciseOrderEntry> entries = new ArrayList<>();
        for (RemoteExerciseEntry e : remote.exercises) {
            entries.add(new CustomExerciseOrderEntry(remote.id, e.exerciseName, e.position,
                    e.sets, e.reps, e.seconds, e.restSeconds));
        }
        exerciseOrderRepository.replaceForCustomWorkout(remote.id, entries);

        if (workoutRepository.getOne(remote.id) != null) {
            flushPendingSchedules(sqlite, scheduledWorkoutRepository, remote.id);
        }
    }

    /** Mirrors the schedules that were waiting for this custom workout to arrive. */
    private void flushPendingSchedules(SQLiteDatabase sqlite,
                                       ScheduledWorkoutRepository scheduledWorkoutRepository,
                                       String customWorkoutId) {
        Iterator<ScheduledWorkout> it = pendingSchedules.values().iterator();
        while (it.hasNext()) {
            ScheduledWorkout schedule = it.next();
            if (customWorkoutId.equals(schedule.getCustomWorkoutId())) {
                it.remove();
                upsertSchedule(sqlite, scheduledWorkoutRepository, schedule);
            }
        }
    }

    private void onWorkoutLogChange(WorkoutLogRepository workoutLogRepository, String uid,
                                     DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            WorkoutLog existing = workoutLogRepository.getOne(doc.getId());
            if (existing != null) workoutLogRepository.delete(existing);
            return;
        }
        RemoteWorkoutLog remote = RemoteWorkoutLog.fromSnapshot(doc);
        WorkoutLog log = new WorkoutLog(remote.id, uid, remote.workoutName, remote.startedAt,
                remote.durationSeconds, remote.kcal, remote.status, remote.exercises);
        if (workoutLogRepository.getOne(remote.id) == null) {
            workoutLogRepository.insert(log);
        } else {
            workoutLogRepository.update(log);
        }
    }

    private void onScheduleChange(SQLiteDatabase sqlite,
                                   ScheduledWorkoutRepository scheduledWorkoutRepository,
                                   CustomWorkoutRepository customWorkoutRepository, String uid,
                                   DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            removeSchedule(scheduledWorkoutRepository, doc.getId());
            return;
        }
        RemoteSchedule remote = RemoteSchedule.fromSnapshot(doc);
        ScheduledWorkout schedule = new ScheduledWorkout(remote.id, uid, remote.workoutName,
                remote.customWorkoutId, remote.scheduledAt, remote.recurrenceRule, remote.completed,
                null);
        if (remote.customWorkoutId != null
                && customWorkoutRepository.getOne(remote.customWorkoutId) == null) {
            pendingSchedules.put(remote.id, schedule);   // its custom workout is not mirrored yet
            return;
        }
        pendingSchedules.remove(remote.id);
        upsertSchedule(sqlite, scheduledWorkoutRepository, schedule);
    }

    /** Deletes a schedule that no longer exists: its row and its device calendar event. */
    private void removeSchedule(ScheduledWorkoutRepository scheduledWorkoutRepository,
                                String scheduleId) {
        pendingSchedules.remove(scheduleId);
        ScheduledWorkout existing = scheduledWorkoutRepository.getOne(scheduleId);
        if (existing != null) scheduledWorkoutRepository.delete(existing);
        // Looked up by id, not through the row: deleting a custom workout may already have
        // cascaded the row away.
        Long eventId = scheduledWorkoutRepository.getCalendarEventId(scheduleId);
        if (eventId != null) {
            DeviceCalendar.deleteEvent(appContext, eventId);
            scheduledWorkoutRepository.clearCalendarEventId(scheduleId);
        }
    }

    private void upsertSchedule(SQLiteDatabase sqlite,
                                ScheduledWorkoutRepository scheduledWorkoutRepository,
                                ScheduledWorkout schedule) {
        ScheduledWorkout existing = scheduledWorkoutRepository.getOne(schedule.getId());
        if (existing == null) {
            scheduledWorkoutRepository.insert(schedule);
            return;
        }
        scheduledWorkoutRepository.update(schedule);
        // Metadata-only changes arrive here too, so act only on a real change.
        boolean changed = existing.getScheduledAt() != schedule.getScheduledAt()
                || !Objects.equals(existing.getRecurrenceRule(), schedule.getRecurrenceRule());
        if (changed && existing.getCalendarEventId() != null) {
            updateCalendarEvent(sqlite, scheduledWorkoutRepository, schedule,
                    existing.getCalendarEventId());
        }
    }

    private void updateCalendarEvent(SQLiteDatabase sqlite,
                                   ScheduledWorkoutRepository scheduledWorkoutRepository,
                                   ScheduledWorkout schedule, long eventId) {
        WorkoutSummary summary = WorkoutSummary.of(sqlite, schedule);
        if (summary == null) return;
        if (!DeviceCalendar.updateEvent(appContext, eventId, summary)) {
            // The user deleted the event in their calendar app; do not bring it back.
            scheduledWorkoutRepository.clearCalendarEventId(schedule.getId());
        }
    }

    private void stopSyncing() {
        stopListeners();
        String uidToClear = syncedUid;
        syncedUid = null;
        childListenersAttached = false;
        if (uidToClear == null) return;
        dbExecutor.execute(() -> {
            pendingSchedules.clear();
            SQLiteDatabase sqlite = SQLManager.getInstance(appContext).getWritableDatabase();
            UserRepository userRepository = new UserRepository(sqlite);
            User cached = userRepository.getOne(uidToClear);
            // Deleting the Users row cascades CustomWorkout, WorkoutLog and ScheduledWorkout with it.
            if (cached != null) userRepository.delete(cached);
        });
    }

    private void stopListeners() {
        if (profileRegistration != null) { profileRegistration.remove(); profileRegistration = null; }
        if (customWorkoutsRegistration != null) { customWorkoutsRegistration.remove(); customWorkoutsRegistration = null; }
        if (workoutLogsRegistration != null) { workoutLogsRegistration.remove(); workoutLogsRegistration = null; }
        if (schedulesRegistration != null) { schedulesRegistration.remove(); schedulesRegistration = null; }
    }
}
