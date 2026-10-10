package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;

import java.util.ArrayList;
import java.util.List;

public class CustomExerciseOrderRepository implements Repository<CustomExerciseOrderEntry> {
    private final SQLiteDatabase db;

    public CustomExerciseOrderRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(CustomExerciseOrderEntry e) {
        return Repository.insertDB(db, "CustomExerciseOrder", toContentValues(e));
    }

    @Override
    public boolean update(CustomExerciseOrderEntry e) {
        ContentValues cv = new ContentValues();
        cv.put("ExerciseName", e.getExerciseName());
        return Repository.updateDB(db, "CustomExerciseOrder", cv,
                "CustomWorkoutId = ? AND Position = ?",
                String.valueOf(e.getCustomWorkoutId()), String.valueOf(e.getPosition()));
    }

    @Override
    public boolean delete(CustomExerciseOrderEntry e) {
        return Repository.deleteDB(db, "CustomExerciseOrder",
                "CustomWorkoutId = ? AND Position = ?",
                String.valueOf(e.getCustomWorkoutId()), String.valueOf(e.getPosition()));
    }

    @Override
    public List<CustomExerciseOrderEntry> getAll() {
        return query(null, null);
    }

    public List<CustomExerciseOrderEntry> getByCustomWorkout(long customWorkoutId) {
        return query("CustomWorkoutId = ?", new String[]{String.valueOf(customWorkoutId)});
    }

    public boolean replaceForCustomWorkout(long customWorkoutId,
                                           List<CustomExerciseOrderEntry> entries) {
        db.beginTransaction();
        try {
            db.delete("CustomExerciseOrder", "CustomWorkoutId = ?",
                    new String[]{String.valueOf(customWorkoutId)});
            for (CustomExerciseOrderEntry e : entries) {
                if (!Repository.insertDB(db, "CustomExerciseOrder", toContentValues(e)))
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
                        c.getLong(c.getColumnIndexOrThrow("CustomWorkoutId")),
                        c.getString(c.getColumnIndexOrThrow("ExerciseName")),
                        c.getInt(c.getColumnIndexOrThrow("Position"))));
            }
        }
        return list;
    }

    private ContentValues toContentValues(CustomExerciseOrderEntry e) {
        ContentValues cv = new ContentValues();
        cv.put("CustomWorkoutId", e.getCustomWorkoutId());
        cv.put("ExerciseName", e.getExerciseName());
        cv.put("Position", e.getPosition());
        return cv;
    }
}
