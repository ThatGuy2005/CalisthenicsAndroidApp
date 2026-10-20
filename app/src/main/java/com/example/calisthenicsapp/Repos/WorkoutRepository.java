package com.example.calisthenicsapp.Repos;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.CoreComponents.WorkoutCategory;

import java.util.ArrayList;
import java.util.List;

public class WorkoutRepository implements ReadRepository<Workout> {
    private final SQLiteDatabase db;

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
