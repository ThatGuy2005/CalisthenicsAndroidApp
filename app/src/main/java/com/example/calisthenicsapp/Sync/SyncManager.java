package com.example.calisthenicsapp.Sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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
import com.example.calisthenicsapp.Service.SessionManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.util.ArrayList;
import java.util.List;
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
 */
public class SyncManager {

    private final Context appContext;
    private final SessionManager sessionManager = new SessionManager();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private String syncedUid;
    private boolean childListenersAttached;

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

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        SQLiteDatabase sqlite = SQLManager.getInstance(appContext).getWritableDatabase();
        UserRepository userRepository = new UserRepository(sqlite);

        profileRegistration = firestore.collection("users").document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    RemoteUser remote = RemoteUser.fromSnapshot(uid, snapshot);
                    dbExecutor.execute(() -> {
                        mirrorUser(userRepository, remote);
                        if (!childListenersAttached) {
                            childListenersAttached = true;
                            attachChildListeners(uid, firestore, sqlite);
                        }
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
                    dbExecutor.execute(() -> {
                        for (DocumentChange change : changes) {
                            onCustomWorkoutChange(customWorkoutRepository, exerciseOrderRepository,
                                    uid, change);
                        }
                    });
                });

        workoutLogsRegistration = firestore.collection("users").document(uid)
                .collection("workoutLogs")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<DocumentChange> changes = snapshots.getDocumentChanges();
                    dbExecutor.execute(() -> {
                        for (DocumentChange change : changes) {
                            onWorkoutLogChange(workoutLogRepository, uid, change);
                        }
                    });
                });

        schedulesRegistration = firestore.collection("users").document(uid)
                .collection("schedules")
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<DocumentChange> changes = snapshots.getDocumentChanges();
                    dbExecutor.execute(() -> {
                        for (DocumentChange change : changes) {
                            onScheduleChange(scheduledWorkoutRepository, uid, change);
                        }
                    });
                });
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

    private void onCustomWorkoutChange(CustomWorkoutRepository workoutRepository,
                                        CustomExerciseOrderRepository exerciseOrderRepository,
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
                remote.durationSeconds, remote.kcal);
        if (workoutLogRepository.getOne(remote.id) == null) {
            workoutLogRepository.insert(log);
        } else {
            workoutLogRepository.update(log);
        }
    }

    private void onScheduleChange(ScheduledWorkoutRepository scheduledWorkoutRepository, String uid,
                                   DocumentChange change) {
        DocumentSnapshot doc = change.getDocument();
        if (change.getType() == DocumentChange.Type.REMOVED) {
            ScheduledWorkout existing = scheduledWorkoutRepository.getOne(doc.getId());
            if (existing != null) scheduledWorkoutRepository.delete(existing);
            return;
        }
        RemoteSchedule remote = RemoteSchedule.fromSnapshot(doc);
        ScheduledWorkout schedule = new ScheduledWorkout(remote.id, uid, remote.workoutName,
                remote.customWorkoutId, remote.scheduledAt);
        if (scheduledWorkoutRepository.getOne(remote.id) == null) {
            scheduledWorkoutRepository.insert(schedule);
        } else {
            scheduledWorkoutRepository.update(schedule);
        }
    }

    private void stopSyncing() {
        stopListeners();
        String uidToClear = syncedUid;
        syncedUid = null;
        childListenersAttached = false;
        if (uidToClear == null) return;
        dbExecutor.execute(() -> {
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
