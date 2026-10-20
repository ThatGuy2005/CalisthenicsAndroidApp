package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.example.calisthenicsapp.Database.SQLManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared plumbing for every service that reads the local SQLite database: the premade library
 * services extend it directly, the per-user ones through {@link UserDataService}.
 *
 * All SQLite work runs on one background thread; every callback is delivered on the main thread.
 */
abstract class DbService {
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final Context appContext;

    protected DbService(Context context) {
        this.appContext = context.getApplicationContext();
    }

    protected Context context() {
        return appContext;
    }

    /** Only call from inside {@link #onDb}: opening the database can be slow the first time. */
    protected SQLiteDatabase db() {
        return SQLManager.getInstance(appContext).getWritableDatabase();
    }

    protected void onDb(Runnable task) {
        DB_EXECUTOR.execute(task);
    }

    protected static <T> void succeed(ServiceCallback<T> callback, T result) {
        MAIN.post(() -> callback.onSuccess(result));
    }

    protected static <T> void fail(ServiceCallback<T> callback, String message) {
        MAIN.post(() -> callback.onError(message));
    }
}
