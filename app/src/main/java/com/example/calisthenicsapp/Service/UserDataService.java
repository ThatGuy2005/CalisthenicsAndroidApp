package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.function.Function;

/**
 * Shared plumbing for the per-user data services (custom workouts, logs, schedules, profile,
 * stats, account, workout sessions). The SQLite thread and the callbacks come from
 * {@link DbService}.
 *
 * Reads come from SQLite, which SyncManager keeps in step with Firestore.
 * Writes go ONLY to Firestore. A write is reported as successful as soon as it is handed to
 * Firestore: it is then in Firestore's local cache, the snapshot listeners mirror it into
 * SQLite right away (also offline), and Firestore sends it to the server when it can.
 * Waiting for the server instead would leave the UI hanging forever while offline.
 * If the server later rejects a write, Firestore rolls it back in its cache, the listeners
 * remove it from SQLite again, and the rejection is logged.
 */
abstract class UserDataService extends DbService {
    private static final String TAG = "UserDataService";

    protected static final String NOT_SIGNED_IN = "You are not signed in.";

    protected final SessionManager sessionManager = new SessionManager();

    protected UserDataService(Context context) {
        super(context);
    }

    /**
     * Asked for on every use, never kept: deleting the account stops the instance to clear its
     * offline cache, and getInstance() then hands out a fresh one.
     */
    protected static FirebaseFirestore firestore() {
        return FirebaseFirestore.getInstance();
    }

    protected DocumentReference userDoc(String uid) {
        return firestore().collection("users").document(uid);
    }

    protected static long now() {
        return System.currentTimeMillis() / 1000L;    // epoch seconds, like the rest of the schema
    }

    /** Runs a SQLite read for the signed-in user in the background and delivers the result. */
    protected <T> void readForUser(ServiceCallback<T> callback, Function<String, T> read) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> succeed(callback, read.apply(uid)));
    }

    /** Logs a write the server rejected; see the class Javadoc for why this is not a callback. */
    protected void watchWrite(Task<Void> write, String description) {
        write.addOnFailureListener(e -> Log.w(TAG, description + " was rejected", e));
    }
}
