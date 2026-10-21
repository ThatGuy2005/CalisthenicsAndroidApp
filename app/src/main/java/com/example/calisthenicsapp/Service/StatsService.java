package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.StatsPeriod;
import com.example.calisthenicsapp.CoreComponents.WorkoutStats;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.function.Supplier;

/**
 * Summary statistics over the signed-in user's workout logs (FR-5.7): number of workouts,
 * total training time and total calories for this week, this month or all time.
 * The matching history list comes from WorkoutLogService.getMyLogs(period, ...).
 *
 * Computed from SQLite, so it works offline and includes logs saved offline.
 */
public class StatsService extends UserDataService {

    public StatsService(Context context) {
        super(context);
    }

    /** With dependencies of your own, e.g. in a test; see {@link UserDataService}. */
    public StatsService(Context context, Supplier<SQLiteDatabase> database,
                        SessionManager sessionManager, Supplier<FirebaseFirestore> firestore) {
        super(context, database, sessionManager, firestore);
    }

    /** A null period means ALL_TIME. */
    public void getStats(StatsPeriod period, ServiceCallback<WorkoutStats> callback) {
        long from = (period == null ? StatsPeriod.ALL_TIME : period).startSeconds();
        readForUser(callback, uid -> new WorkoutLogRepository(db()).getStats(uid, from));
    }
}
