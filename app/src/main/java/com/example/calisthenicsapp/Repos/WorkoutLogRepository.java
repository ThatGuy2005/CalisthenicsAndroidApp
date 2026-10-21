package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.calisthenicsapp.CoreComponents.LoggedExercise;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.CoreComponents.WorkoutStats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WorkoutLogRepository implements Repository<WorkoutLog> {
    private static final String TAG = "WorkoutLogRepository";
    private final SQLiteDatabase db;

    public WorkoutLogRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(WorkoutLog l) {                // Id is the Firestore document id
        ContentValues cv = new ContentValues();
        cv.put("Id", l.getId());
        cv.put("Uid", l.getUid());
        cv.put("WorkoutName", l.getWorkoutName());
        cv.put("StartedAt", l.getStartedAt());
        cv.put("DurationSeconds", l.getDurationSeconds());
        cv.put("Kcal", l.getKcal());
        cv.put("Status", l.getStatus());
        cv.put("Snapshot", toJson(l.getExercises()));
        return SqlHelpers.insertDB(db, "WorkoutLog", cv);
    }

    @Override
    public boolean update(WorkoutLog l) {
        ContentValues cv = new ContentValues();
        cv.put("WorkoutName", l.getWorkoutName());
        cv.put("StartedAt", l.getStartedAt());
        cv.put("DurationSeconds", l.getDurationSeconds());
        cv.put("Kcal", l.getKcal());
        cv.put("Status", l.getStatus());
        cv.put("Snapshot", toJson(l.getExercises()));
        return SqlHelpers.updateDB(db, "WorkoutLog", cv,
                "Id = ?", l.getId());
    }

    @Override
    public boolean delete(WorkoutLog l) {
        return SqlHelpers.deleteDB(db, "WorkoutLog", "Id = ?", l.getId());
    }

    @Override
    public List<WorkoutLog> getAll() {
        return query(null, null);
    }

    public List<WorkoutLog> getByUid(String uid) {
        return query("Uid = ?", new String[]{uid});   // newest first
    }

    /** The user's logs that started at or after fromSeconds, newest first: the ones getStats counts. */
    public List<WorkoutLog> getByUidSince(String uid, long fromSeconds) {
        return query("Uid = ? AND StartedAt >= ?", new String[]{uid, String.valueOf(fromSeconds)});
    }

    public WorkoutLog getOne(String id) {
        List<WorkoutLog> r = query("Id = ?", new String[]{id});
        return r.isEmpty() ? null : r.get(0);
    }

    /** Totals over the user's logs that started at or after fromSeconds (epoch seconds). */
    public WorkoutStats getStats(String uid, long fromSeconds) {
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*), "
                        + "COALESCE(SUM(Status = 'Completed'), 0), "
                        + "COALESCE(SUM(Status = 'Partial'), 0), "
                        + "COALESCE(SUM(DurationSeconds), 0), "
                        + "COALESCE(SUM(Kcal), 0) "
                        + "FROM WorkoutLog WHERE Uid = ? AND StartedAt >= ?",
                new String[]{uid, String.valueOf(fromSeconds)})) {
            c.moveToFirst();      // an aggregate without GROUP BY always returns one row
            return new WorkoutStats(c.getInt(0), c.getInt(1), c.getInt(2),
                    c.getLong(3), c.getDouble(4));
        }
    }

    private List<WorkoutLog> query(String where, String[] args) {
        List<WorkoutLog> list = new ArrayList<>();
        try (Cursor c = db.query("WorkoutLog", null, where, args,
                null, null, "StartedAt DESC")) {
            while (c.moveToNext()) {
                list.add(new WorkoutLog(
                        c.getString(c.getColumnIndexOrThrow("Id")),
                        c.getString(c.getColumnIndexOrThrow("Uid")),
                        c.getString(c.getColumnIndexOrThrow("WorkoutName")),
                        c.getLong(c.getColumnIndexOrThrow("StartedAt")),
                        c.getInt(c.getColumnIndexOrThrow("DurationSeconds")),
                        c.getDouble(c.getColumnIndexOrThrow("Kcal")),
                        c.getString(c.getColumnIndexOrThrow("Status")),
                        fromJson(c.getString(c.getColumnIndexOrThrow("Snapshot")))));
            }
        }
        return list;
    }

    /** The snapshot as JSON; also used by the data export. Fails only on NaN/infinite numbers. */
    public static JSONArray snapshotToJson(List<LoggedExercise> exercises) throws JSONException {
        JSONArray array = new JSONArray();
        for (LoggedExercise e : exercises) {
            JSONObject o = new JSONObject();
            o.put("exerciseName", e.getExerciseName());
            o.put("met", e.getMet());
            o.put("timed", e.isTimed());
            o.put("sets", e.getSets());
            o.put("completedSets", e.getCompletedSets());
            o.put("reps", e.getReps() == null ? JSONObject.NULL : e.getReps());
            o.put("seconds", e.getSeconds() == null ? JSONObject.NULL : e.getSeconds());
            o.put("perSide", e.isPerSide());
            o.put("activeSeconds", e.getActiveSeconds());
            o.put("kcal", e.getKcal());
            array.put(o);
        }
        return array;
    }

    // The snapshot is only ever read back whole, so it is one JSON column, not a child table.
    private static String toJson(List<LoggedExercise> exercises) {
        try {
            return snapshotToJson(exercises).toString();
        } catch (JSONException ex) {
            Log.w(TAG, "Could not store a workout log snapshot", ex);
            return "[]";
        }
    }

    private static List<LoggedExercise> fromJson(String json) {
        List<LoggedExercise> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                list.add(new LoggedExercise(
                        o.optString("exerciseName"),
                        o.optDouble("met", 0),
                        o.optBoolean("timed"),
                        o.optInt("sets"),
                        o.optInt("completedSets"),
                        o.isNull("reps") ? null : o.optInt("reps"),
                        o.isNull("seconds") ? null : o.optInt("seconds"),
                        o.optInt("activeSeconds"),
                        o.optDouble("kcal", 0),
                        o.optBoolean("perSide")));    // false in older snapshots
            }
        } catch (JSONException ex) {
            Log.w(TAG, "Unreadable workout log snapshot", ex);
        }
        return list;
    }
}
