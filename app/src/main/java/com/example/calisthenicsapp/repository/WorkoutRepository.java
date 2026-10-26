package com.example.calisthenicsapp.repository;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.model.Difficulty;
import com.example.calisthenicsapp.model.Workout;
import com.example.calisthenicsapp.model.WorkoutCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * The premade workouts (the Workout table from seed.sql), keyed by name. Read only; their exercises
 * are in {@link ExerciseOrderRepository}. The estimated length is not stored: WorkoutService adds
 * it.
 */
public class WorkoutRepository implements ReadRepository<Workout> {
    private final SQLiteDatabase db;

    /** Works on an open database; it does not close it. */
    public WorkoutRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public List<Workout> getAll() {
        List<Workout> list = new ArrayList<>();
        try (Cursor c = db.query("Workout", null, null, null,
                null, null, "Name ASC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    /** The premade workout with exactly this name, or null. */
    public Workout getOne(String name) {
        try (Cursor c = db.query("Workout", null, "Name = ?",
                new String[]{name}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    /**
     * Level and category are stored as their labels: the Difficulty label ("Beginner"), like
     * Exercise.Difficulty, and the WorkoutCategory label ("Full body").
     */
    private static Workout fromCursor(Cursor c) {
        return new Workout(c.getString(c.getColumnIndexOrThrow("Name")),
                Difficulty.fromLabel(c.getString(c.getColumnIndexOrThrow("Level"))),
                WorkoutCategory.fromLabel(c.getString(c.getColumnIndexOrThrow("Category"))),
                c.getString(c.getColumnIndexOrThrow("Description")));
    }
}
