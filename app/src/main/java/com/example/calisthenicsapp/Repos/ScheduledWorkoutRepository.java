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
    public boolean insert(ScheduledWorkout s) {             // Id is autoincremented
        try {
            long id = db.insertOrThrow("ScheduledWorkout", null, toContentValues(s));
            if (id == -1) return false;
            s.setId(id);                                    // caller now has the id
            return true;
        } catch (SQLiteConstraintException e) {
            return false;   // both or neither workout set, or unknown user/workout
        }
    }

    /** Changes the workout and/or the time. The owner (Username) never changes. */
    @Override
    public boolean update(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        ContentValues cv = toContentValues(s);
        cv.remove("Id");
        cv.remove("Username");
        return Repository.updateDB(db, "ScheduledWorkout", cv,
                "Id = ?", String.valueOf(s.getId()));
    }

    @Override
    public boolean delete(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        return Repository.deleteDB(db, "ScheduledWorkout",
                "Id = ?", String.valueOf(s.getId()));
    }

    @Override
    public List<ScheduledWorkout> getAll() {
        return query(null, null);
    }

    public ScheduledWorkout getOne(long id) {
        List<ScheduledWorkout> r = query("Id = ?", new String[]{String.valueOf(id)});
        return r.isEmpty() ? null : r.get(0);
    }

    /** All of a user's scheduled workouts, soonest first. */
    public List<ScheduledWorkout> getByUsername(String username) {
        return query("Username = ?", new String[]{username});
    }

    /** A user's workouts scheduled at or after fromEpochSeconds, soonest first. */
    public List<ScheduledWorkout> getUpcoming(String username, long fromEpochSeconds) {
        return query("Username = ? AND ScheduledAt >= ?",
                new String[]{username, String.valueOf(fromEpochSeconds)});
    }

    private List<ScheduledWorkout> query(String where, String[] args) {
        List<ScheduledWorkout> list = new ArrayList<>();
        try (Cursor c = db.query("ScheduledWorkout", null, where, args,
                null, null, "ScheduledAt ASC")) {
            int idCol = c.getColumnIndexOrThrow("Id");
            int userCol = c.getColumnIndexOrThrow("Username");
            int nameCol = c.getColumnIndexOrThrow("WorkoutName");
            int customCol = c.getColumnIndexOrThrow("CustomWorkoutId");
            int atCol = c.getColumnIndexOrThrow("ScheduledAt");
            while (c.moveToNext()) {
                // getString returns null for NULL, but getInt/getLong return 0,
                // so the nullable CustomWorkoutId needs an explicit isNull check.
                list.add(new ScheduledWorkout(
                        c.getLong(idCol),
                        c.getString(userCol),
                        c.getString(nameCol),
                        c.isNull(customCol) ? null : c.getLong(customCol),
                        c.getLong(atCol)));
            }
        }
        return list;
    }

    private ContentValues toContentValues(ScheduledWorkout s) {
        ContentValues cv = new ContentValues();
        if (s.getId() != null) cv.put("Id", s.getId());
        cv.put("Username", s.getUsername());
        if (s.getWorkoutName() != null) cv.put("WorkoutName", s.getWorkoutName());
        else cv.putNull("WorkoutName");
        if (s.getCustomWorkoutId() != null) cv.put("CustomWorkoutId", s.getCustomWorkoutId());
        else cv.putNull("CustomWorkoutId");
        cv.put("ScheduledAt", s.getScheduledAt());
        return cv;
    }
}