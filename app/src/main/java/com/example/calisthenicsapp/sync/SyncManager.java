package com.example.calisthenicsapp.sync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.example.calisthenicsapp.database.SQLManager;
import com.example.calisthenicsapp.scheduling.ScheduleCalendar;
import com.example.calisthenicsapp.service.DataChanges;
import com.example.calisthenicsapp.service.SessionManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Keeps the local SQLite cache in step with Firestore for the signed-in user. Firestore is the
 * source of truth: every write goes there, and these snapshot listeners (registered with
 * MetadataChanges.INCLUDE, so pending local writes show up immediately offline too) hand
 * whatever Firestore holds to a {@link LocalMirror}, which writes it into the repositories the
 * rest of the app reads from. There is no outbox or dirty-flag tracking here on purpose: the
 * SDK's own offline cache plus these listeners are the whole sync mechanism.
 *
 * Listener order matters: the profile document is mirrored into Users, and only then are the
 * three per-user collections subscribed to, because CustomWorkout, WorkoutLog and
 * ScheduledWorkout all foreign-key to Users(Uid). Signing out deletes the user's local rows
 * and leaves their calendar events in the calendar.
 *
 * Deletions: Firestore normally reports them as REMOVED changes, also for documents deleted while
 * the app was closed (its own offline cache still has them until the server says otherwise).
 * But if the app is killed after Firestore has dropped a document and before it was removed
 * here, or Firestore's cache is reset, no REMOVED ever comes. So every snapshot confirmed by the
 * server is also a full list: local rows missing from it are deleted. Snapshots from the cache
 * are never used for that, because the cache may be incomplete.
 *
 * After each batch is in SQLite, DataChanges tells the screens what changed.
 *
 * A missing profile: without users/{uid} nothing else can be mirrored, so the app would keep
 * running on an old copy and never show the user's changes. When the server says the profile is
 * gone (see isProfileGone), the account counts as deleted: the user is signed out, which clears
 * the local data, and the onAccountGone callback runs.
 *
 * Threading: listener state (syncedUid, mirror, registrations) is changed only on the main
 * thread. SQLite work runs on dbExecutor, and each queued task first checks that its uid is
 * still the synced one, so events queued before a sign-out or user switch never touch the new
 * session.
 */
public class SyncManager {

    /** Sign-up saves the profile right after creating the account; until then it is missing. */
    static final long NEW_ACCOUNT_MILLIS = 10 * 60 * 1000L;

    private final Context appContext;
    private final Supplier<SQLiteDatabase> database;
    private final SessionManager sessionManager;
    private final Supplier<FirebaseFirestore> firestore;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile String syncedUid;
    /** The synced user's mirror; only used on dbExecutor. */
    private LocalMirror mirror;
    private boolean childListenersAttached;
    /** The profile as last mirrored; main thread only. Skips snapshots that only changed metadata. */
    private Map<String, Object> lastProfileData;
    /** The server has shown the profile this session; main thread only. */
    private boolean profileConfirmed;
    private Runnable onAccountGone;

    private ListenerRegistration profileRegistration;
    private ListenerRegistration customWorkoutsRegistration;
    private ListenerRegistration workoutLogsRegistration;
    private ListenerRegistration schedulesRegistration;

    /** Syncs the app's database with FirebaseFirestore.getInstance() for FirebaseAuth's user. */
    public SyncManager(Context context) {
        this(context, () -> SQLManager.getInstance(context.getApplicationContext()).getWritableDatabase(),
                new SessionManager(), FirebaseFirestore::getInstance);
    }

    /**
     * With dependencies of your own, e.g. in a test. The context is for the device calendar.
     * firestore is asked for at every sign-in, never kept: see UserDataService.firestore().
     */
    public SyncManager(Context context, Supplier<SQLiteDatabase> database,
                       SessionManager sessionManager, Supplier<FirebaseFirestore> firestore) {
        this.appContext = context.getApplicationContext();
        this.database = database;
        this.sessionManager = sessionManager;
        this.firestore = firestore;
    }

    /**
     * Runs on the main thread after the signed-in user was signed out because their profile is
     * gone, e.g. to show the sign-in screen. Set it before start().
     */
    public void setOnAccountGone(Runnable onAccountGone) {
        this.onAccountGone = onAccountGone;
    }

    /** Call once (e.g. from Application.onCreate()); reacts to every sign-in/out after that. */
    public void start() {
        sessionManager.addAuthStateListener(new SessionManager.SignedInListener() {
            @Override
            public void onSignedIn(FirebaseUser user) {
                startSyncing(user);
            }

            @Override
            public void onSignedOut() {
                stopSyncing();
            }
        });
    }

    private void startSyncing(FirebaseUser user) {
        String uid = user.getUid();
        if (uid.equals(syncedUid)) return;   // already syncing this user (e.g. a token refresh)
        stopListeners();
        SQLiteDatabase sqlite = database.get();
        LocalMirror mirror = new LocalMirror(uid, sqlite, new ScheduleCalendar(appContext, sqlite));
        syncedUid = uid;
        this.mirror = mirror;
        childListenersAttached = false;
        lastProfileData = null;
        profileConfirmed = false;
        Long createdMillis = user.getMetadata() == null ? null
                : user.getMetadata().getCreationTimestamp();

        DocumentReference userDoc = firestore.get().collection("users").document(uid);
        profileRegistration = userDoc.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
            if (error != null || snapshot == null) return;
            boolean fromCache = snapshot.getMetadata().isFromCache();
            if (!snapshot.exists()) {
                if (isProfileGone(fromCache, profileConfirmed, createdMillis,
                        System.currentTimeMillis())) {
                    signOutGoneAccount(uid);
                }
                return;
            }
            if (!fromCache) profileConfirmed = true;
            Map<String, Object> data = snapshot.getData();
            if (data != null && data.equals(lastProfileData)) return;   // e.g. pending -> sent
            lastProfileData = data;
            RemoteUser remote = RemoteUser.fromSnapshot(uid, snapshot);
            dbExecutor.execute(() -> {
                if (!uid.equals(syncedUid)) return;   // signed out or switched user since
                mirror.applyUser(remote);
                DataChanges.notifyChanged(DataChanges.Kind.PROFILE);
                // The Users row now exists; attach the children back on the main thread.
                mainHandler.post(() -> {
                    if (!uid.equals(syncedUid) || childListenersAttached) return;
                    childListenersAttached = true;
                    attachChildListeners(uid, userDoc, mirror);
                });
            });
        });
    }

    private void attachChildListeners(String uid, DocumentReference userDoc, LocalMirror mirror) {
        customWorkoutsRegistration = listen(uid, userDoc, "customWorkouts",
                (changed, removed, serverIds) -> mirror.applyCustomWorkouts(
                        parse(changed, RemoteCustomWorkout::fromSnapshot), removed, serverIds),
                // Schedules show the workout's name, and deleting one cascades.
                DataChanges.Kind.CUSTOM_WORKOUTS, DataChanges.Kind.SCHEDULES);
        workoutLogsRegistration = listen(uid, userDoc, "workoutLogs",
                (changed, removed, serverIds) -> mirror.applyWorkoutLogs(
                        parse(changed, RemoteWorkoutLog::fromSnapshot), removed, serverIds),
                DataChanges.Kind.WORKOUT_LOGS);
        schedulesRegistration = listen(uid, userDoc, "schedules",
                (changed, removed, serverIds) -> mirror.applySchedules(
                        parse(changed, RemoteSchedule::fromSnapshot), removed, serverIds),
                DataChanges.Kind.SCHEDULES);
    }

    /** One of LocalMirror's apply methods; runs on dbExecutor and says whether SQLite changed. */
    private interface CollectionMirror {
        boolean apply(List<DocumentSnapshot> changed, List<String> removed, Set<String> serverIds);
    }

    /**
     * Listens to one of the user's collections and hands each snapshot to {@code into} on
     * dbExecutor. If that changed SQLite, the screens are told about each of {@code kinds}.
     */
    private ListenerRegistration listen(String uid, DocumentReference userDoc, String collection,
                                        CollectionMirror into, DataChanges.Kind... kinds) {
        return userDoc.collection(collection)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<DocumentSnapshot> changed = new ArrayList<>();
                    List<String> removed = new ArrayList<>();
                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.REMOVED) {
                            removed.add(change.getDocument().getId());
                        } else {
                            changed.add(change.getDocument());
                        }
                    }
                    Set<String> serverIds = serverIds(snapshots);
                    dbExecutor.execute(() -> {
                        if (!uid.equals(syncedUid)) return;
                        if (!into.apply(changed, removed, serverIds)) return;
                        for (DataChanges.Kind kind : kinds) DataChanges.notifyChanged(kind);
                    });
                });
    }

    private static <T> List<T> parse(List<DocumentSnapshot> docs, Function<DocumentSnapshot, T> from) {
        List<T> parsed = new ArrayList<>();
        for (DocumentSnapshot doc : docs) parsed.add(from.apply(doc));
        return parsed;
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

    /**
     * Whether a missing profile means the account is gone. Only the server can say so; the cache
     * may simply not have it yet. Once the server has shown the profile this session, a later
     * miss is left alone: it is this device's own Delete account (AccountService deletes the
     * profile just before the Firebase account) or another device's, which deletes the account
     * too. A new account (or one of unknown age) is left alone while sign-up saves its profile.
     */
    static boolean isProfileGone(boolean fromCache, boolean confirmedBefore, Long createdMillis,
                                 long nowMillis) {
        if (fromCache || confirmedBefore || createdMillis == null) return false;
        return nowMillis - createdMillis > NEW_ACCOUNT_MILLIS;
    }

    /** Signing out stops syncing and clears the local data (onSignedOut, stopSyncing). */
    private void signOutGoneAccount(String uid) {
        if (!uid.equals(syncedUid)) return;
        sessionManager.signOut();
        if (onAccountGone != null) onAccountGone.run();
    }

    private void stopSyncing() {
        stopListeners();
        LocalMirror toClear = mirror;
        syncedUid = null;
        mirror = null;
        childListenersAttached = false;
        lastProfileData = null;
        if (toClear == null) return;
        dbExecutor.execute(() -> {
            toClear.clear();
            DataChanges.notifyAllChanged();
        });
    }

    private void stopListeners() {
        if (profileRegistration != null) { profileRegistration.remove(); profileRegistration = null; }
        if (customWorkoutsRegistration != null) { customWorkoutsRegistration.remove(); customWorkoutsRegistration = null; }
        if (workoutLogsRegistration != null) { workoutLogsRegistration.remove(); workoutLogsRegistration = null; }
        if (schedulesRegistration != null) { schedulesRegistration.remove(); schedulesRegistration = null; }
    }
}
