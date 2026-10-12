package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.calisthenicsapp.Database.SQLManager;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Shared plumbing for the per-user data services (custom workouts, logs, schedules, profile).
 *
 * Reads come from SQLite, which SyncManager keeps in step with Firestore.
 * Writes go ONLY to Firestore. A write is reported as successful as soon as it is handed to
 * Firestore: it is then in Firestore's local cache, the snapshot listeners mirror it into
 * SQLite right away (also offline), and Firestore sends it to the server when it can.
 * Waiting for the server instead would leave the UI hanging forever while offline.
 * If the server later rejects a write, Firestore rolls it back in its cache, the listeners
 * remove it from SQLite again, and the rejection is logged.
 *
 * All SQLite work runs on one background thread; every callback is delivered on the main thread.
 */
abstract class UserDataService {
    private static final String TAG = "UserDataService";
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    protected static final String NOT_SIGNED_IN = "You are not signed in.";

    protected final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    protected final SessionManager sessionManager = new SessionManager();
    private final Context appContext;

    protected UserDataService(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** Only call from inside {@link #onDb}: opening the database can be slow the first time. */
    protected SQLiteDatabase db() {
        return SQLManager.getInstance(appContext).getWritableDatabase();
    }

    protected DocumentReference userDoc(String uid) {
        return firestore.collection("users").document(uid);
    }

    protected static long now() {
        return System.currentTimeMillis() / 1000L;    // epoch seconds, like the rest of the schema
    }

    protected void onDb(Runnable task) {
        DB_EXECUTOR.execute(task);
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

    protected static <T> void succeed(ServiceCallback<T> callback, T result) {
        MAIN.post(() -> callback.onSuccess(result));
    }

    protected static <T> void fail(ServiceCallback<T> callback, String message) {
        MAIN.post(() -> callback.onError(message));
    }
}
