package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;

import java.util.ArrayList;
import java.util.List;

public class CustomExerciseOrderRepository implements Repository<CustomExerciseOrderEntry> {
    private final SQLiteDatabase db;

    public CustomExerciseOrderRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(CustomExerciseOrderEntry e) {
        return SqlHelpers.insertDB(db, "CustomExerciseOrder", toContentValues(e));
    }

    /** The row is identified by (CustomWorkoutId, Position); everything else is updated. */
    @Override
    public boolean update(CustomExerciseOrderEntry e) {
        ContentValues cv = toContentValues(e);
        cv.remove("CustomWorkoutId");
        cv.remove("Position");
        return SqlHelpers.updateDB(db, "CustomExerciseOrder", cv,
                "CustomWorkoutId = ? AND Position = ?",
                e.getCustomWorkoutId(), String.valueOf(e.getPosition()));
    }

    @Override
    public boolean delete(CustomExerciseOrderEntry e) {
        return SqlHelpers.deleteDB(db, "CustomExerciseOrder",
                "CustomWorkoutId = ? AND Position = ?",
                e.getCustomWorkoutId(), String.valueOf(e.getPosition()));
    }

    @Override
    public List<CustomExerciseOrderEntry> getAll() {
        return query(null, null);
    }

    public List<CustomExerciseOrderEntry> getByCustomWorkout(String customWorkoutId) {
        return query("CustomWorkoutId = ?", new String[]{customWorkoutId});
    }

    public boolean replaceForCustomWorkout(String customWorkoutId,
                                           List<CustomExerciseOrderEntry> entries) {
        db.beginTransaction();
        try {
            db.delete("CustomExerciseOrder", "CustomWorkoutId = ?",
                    new String[]{customWorkoutId});
            for (CustomExerciseOrderEntry e : entries) {
                if (!SqlHelpers.insertDB(db, "CustomExerciseOrder", toContentValues(e)))
                    return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private List<CustomExerciseOrderEntry> query(String where, String[] args) {
        List<CustomExerciseOrderEntry> list = new ArrayList<>();
        try (Cursor c = db.query("CustomExerciseOrder", null, where, args,
                null, null, "CustomWorkoutId ASC, Position ASC")) {
            while (c.moveToNext()) {
                list.add(new CustomExerciseOrderEntry(
                        c.getString(c.getColumnIndexOrThrow("CustomWorkoutId")),
                        c.getString(c.getColumnIndexOrThrow("ExerciseName")),
                        c.getInt(c.getColumnIndexOrThrow("Position")),
                        c.getInt(c.getColumnIndexOrThrow("Sets")),
                        SqlHelpers.intOrNull(c, "Reps"),        // NULL = use the exercise default
                        SqlHelpers.intOrNull(c, "Seconds"),     // NULL = use the exercise default
                        c.getInt(c.getColumnIndexOrThrow("RestSeconds")),
                        SqlHelpers.intOrNull(c, "RestAfterSeconds")));   // NULL = RestSeconds
            }
        }
        return list;
    }

    private ContentValues toContentValues(CustomExerciseOrderEntry e) {
        ContentValues cv = new ContentValues();
        cv.put("CustomWorkoutId", e.getCustomWorkoutId());
        cv.put("ExerciseName", e.getExerciseName());
        cv.put("Position", e.getPosition());
        // Sets and RestSeconds are NOT NULL: leave them out when null so the DB default
        // (3 sets, 60 s) applies. An explicit NULL would be rejected.
        if (e.getSets() != null) cv.put("Sets", e.getSets());
        SqlHelpers.putIntOrNull(cv, "Reps", e.getReps());
        SqlHelpers.putIntOrNull(cv, "Seconds", e.getSeconds());
        if (e.getRestSeconds() != null) cv.put("RestSeconds", e.getRestSeconds());
        SqlHelpers.putIntOrNull(cv, "RestAfterSeconds", e.getRestAfterSeconds());
        return cv;
    }
}
