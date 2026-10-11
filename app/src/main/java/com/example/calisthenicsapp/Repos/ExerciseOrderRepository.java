package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;

import java.util.ArrayList;
import java.util.List;

public class ExerciseOrderRepository implements Repository<ExerciseOrderEntry> {
    private final SQLiteDatabase db;

    public ExerciseOrderRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(ExerciseOrderEntry e) {
        return Repository.insertDB(db, "ExerciseOrder", toContentValues(e));
    }

    /** The row is identified by (WorkoutName, Position); everything else is updated. */
    @Override
    public boolean update(ExerciseOrderEntry e) {
        ContentValues cv = toContentValues(e);
        cv.remove("WorkoutName");
        cv.remove("Position");
        return Repository.updateDB(db, "ExerciseOrder", cv,
                "WorkoutName = ? AND Position = ?",
                e.getWorkoutName(), String.valueOf(e.getPosition()));
    }

    @Override
    public boolean delete(ExerciseOrderEntry e) {
        return Repository.deleteDB(db, "ExerciseOrder",
                "WorkoutName = ? AND Position = ?",
                e.getWorkoutName(), String.valueOf(e.getPosition()));
    }

    @Override
    public List<ExerciseOrderEntry> getAll() {
        return query(null, null);
    }

    public List<ExerciseOrderEntry> getByWorkout(String workoutName) {
        return query("WorkoutName = ?", new String[]{workoutName});
    }

    /** Atomically replaces a workout's whole exercise list (use for reordering). */
    public boolean replaceForWorkout(String workoutName, List<ExerciseOrderEntry> entries) {
        db.beginTransaction();
        try {
            db.delete("ExerciseOrder", "WorkoutName = ?", new String[]{workoutName});
            for (ExerciseOrderEntry e : entries) {
                if (!Repository.insertDB(db, "ExerciseOrder", toContentValues(e)))
                    return false;                       // finally{} rolls back
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private List<ExerciseOrderEntry> query(String where, String[] args) {
        List<ExerciseOrderEntry> list = new ArrayList<>();
        try (Cursor c = db.query("ExerciseOrder", null, where, args,
                null, null, "WorkoutName ASC, Position ASC")) {
            while (c.moveToNext()) {
                list.add(new ExerciseOrderEntry(
                        c.getString(c.getColumnIndexOrThrow("WorkoutName")),
                        c.getString(c.getColumnIndexOrThrow("ExerciseName")),
                        c.getInt(c.getColumnIndexOrThrow("Position")),
                        c.getInt(c.getColumnIndexOrThrow("Sets")),
                        Repository.intOrNull(c, "Reps"),        // NULL = use the exercise default
                        Repository.intOrNull(c, "Seconds"),     // NULL = use the exercise default
                        c.getInt(c.getColumnIndexOrThrow("RestSeconds"))));
            }
        }
        return list;
    }

    private ContentValues toContentValues(ExerciseOrderEntry e) {
        ContentValues cv = new ContentValues();
        cv.put("WorkoutName", e.getWorkoutName());
        cv.put("ExerciseName", e.getExerciseName());
        cv.put("Position", e.getPosition());
        // Sets and RestSeconds are NOT NULL: leave them out when null so the DB default
        // (3 sets, 60 s) applies. An explicit NULL would be rejected.
        if (e.getSets() != null) cv.put("Sets", e.getSets());
        Repository.putIntOrNull(cv, "Reps", e.getReps());
        Repository.putIntOrNull(cv, "Seconds", e.getSeconds());
        if (e.getRestSeconds() != null) cv.put("RestSeconds", e.getRestSeconds());
        return cv;
    }
}