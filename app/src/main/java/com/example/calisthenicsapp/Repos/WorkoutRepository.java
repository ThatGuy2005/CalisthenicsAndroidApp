package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Workout;

import java.util.ArrayList;
import java.util.List;

public class WorkoutRepository implements Repository<Workout> {
    private final SQLiteDatabase db;

    public WorkoutRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(Workout w) {
        ContentValues cv = new ContentValues();
        cv.put("Name", w.getName());
        putLevel(cv, w);
        return Repository.insertDB(db, "Workout", cv);
    }

    @Override
    public boolean update(Workout w) {
        ContentValues cv = new ContentValues();
        putLevel(cv, w);
        return Repository.updateDB(db, "Workout", cv, "Name = ?", w.getName());
    }

    /** Stored as the Difficulty label ("Beginner"), like Exercise.Difficulty. */
    private static void putLevel(ContentValues cv, Workout w) {
        if (w.getLevel() != null) cv.put("Level", w.getLevel().getLabel());
        else cv.putNull("Level");
    }

    public boolean rename(String oldName, String newName) {
        ContentValues cv = new ContentValues();
        cv.put("Name", newName);
        return Repository.updateDB(db, "Workout", cv, "Name = ?", oldName);
    }

    @Override
    public boolean delete(Workout w) {
        // ExerciseOrder rows go with it (ON DELETE CASCADE)
        return Repository.deleteDB(db, "Workout", "Name = ?", w.getName());
    }

    @Override
    public List<Workout> getAll() {
        List<Workout> list = new ArrayList<>();
        try (Cursor c = db.query("Workout", null, null, null,
                null, null, "Name ASC")) {
            while (c.moveToNext())
                list.add(new Workout(c.getString(c.getColumnIndexOrThrow("Name")),
                        Difficulty.fromLabel(c.getString(c.getColumnIndexOrThrow("Level")))));
        }
        return list;
    }

    public Workout getOne(String name) {
        try (Cursor c = db.query("Workout", null, "Name = ?",
                new String[]{name}, null, null, null)) {
            return c.moveToFirst()
                    ? new Workout(c.getString(c.getColumnIndexOrThrow("Name")),
                    Difficulty.fromLabel(c.getString(c.getColumnIndexOrThrow("Level")))) : null;
        }
    }
}
