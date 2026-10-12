package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;

import java.util.ArrayList;
import java.util.List;

public class ScheduledWorkoutRepository implements Repository<ScheduledWorkout> {
    private final SQLiteDatabase db;

    public ScheduledWorkoutRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(ScheduledWorkout s) {             // Id is the Firestore document id
        try {
            return db.insertOrThrow("ScheduledWorkout", null, toContentValues(s)) != -1;
        } catch (SQLiteConstraintException e) {
            return false;   // both or neither workout set, or unknown user/workout
        }
    }

    /** Changes the workout and/or the time. The owner (Uid) never changes. */
    @Override
    public boolean update(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        ContentValues cv = toContentValues(s);
        cv.remove("Id");
        cv.remove("Uid");
        return Repository.updateDB(db, "ScheduledWorkout", cv,
                "Id = ?", s.getId());
    }

    @Override
    public boolean delete(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        return Repository.deleteDB(db, "ScheduledWorkout",
                "Id = ?", s.getId());
    }

    @Override
    public List<ScheduledWorkout> getAll() {
        return query(null, null);
    }

    public ScheduledWorkout getOne(String id) {
        List<ScheduledWorkout> r = query("Id = ?", new String[]{id});
        return r.isEmpty() ? null : r.get(0);
    }

    /** All of a user's scheduled workouts, soonest first. */
    public List<ScheduledWorkout> getByUid(String uid) {
        return query("Uid = ?", new String[]{uid});
    }

    /** A user's workouts scheduled at or after fromEpochSeconds, soonest first. */
    public List<ScheduledWorkout> getUpcoming(String uid, long fromEpochSeconds) {
        return query("Uid = ? AND ScheduledAt >= ?",
                new String[]{uid, String.valueOf(fromEpochSeconds)});
    }

    private List<ScheduledWorkout> query(String where, String[] args) {
        List<ScheduledWorkout> list = new ArrayList<>();
        try (Cursor c = db.query("ScheduledWorkout", null, where, args,
                null, null, "ScheduledAt ASC")) {
            int idCol = c.getColumnIndexOrThrow("Id");
            int uidCol = c.getColumnIndexOrThrow("Uid");
            int nameCol = c.getColumnIndexOrThrow("WorkoutName");
            int customCol = c.getColumnIndexOrThrow("CustomWorkoutId");
            int atCol = c.getColumnIndexOrThrow("ScheduledAt");
            while (c.moveToNext()) {
                list.add(new ScheduledWorkout(
                        c.getString(idCol),
                        c.getString(uidCol),
                        c.getString(nameCol),
                        c.getString(customCol),
                        c.getLong(atCol)));
            }
        }
        return list;
    }

    private ContentValues toContentValues(ScheduledWorkout s) {
        ContentValues cv = new ContentValues();
        if (s.getId() != null) cv.put("Id", s.getId());
        cv.put("Uid", s.getUid());
        if (s.getWorkoutName() != null) cv.put("WorkoutName", s.getWorkoutName());
        else cv.putNull("WorkoutName");
        if (s.getCustomWorkoutId() != null) cv.put("CustomWorkoutId", s.getCustomWorkoutId());
        else cv.putNull("CustomWorkoutId");
        cv.put("ScheduledAt", s.getScheduledAt());
        return cv;
    }
}
