package com.example.calisthenicsapp.Repos;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;

import java.util.ArrayList;
import java.util.List;

public class ExerciseOrderRepository implements ReadRepository<ExerciseOrderEntry> {
    private final SQLiteDatabase db;

    public ExerciseOrderRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public List<ExerciseOrderEntry> getAll() {
        return query(null, null);
    }

    public List<ExerciseOrderEntry> getByWorkout(String workoutName) {
        return query("WorkoutName = ?", new String[]{workoutName});
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
                        SqlHelpers.intOrNull(c, "Reps"),        // NULL = use the exercise default
                        SqlHelpers.intOrNull(c, "Seconds"),     // NULL = use the exercise default
                        c.getInt(c.getColumnIndexOrThrow("RestSeconds")),
                        SqlHelpers.intOrNull(c, "RestAfterSeconds")));   // NULL = RestSeconds
            }
        }
        return list;
    }
}