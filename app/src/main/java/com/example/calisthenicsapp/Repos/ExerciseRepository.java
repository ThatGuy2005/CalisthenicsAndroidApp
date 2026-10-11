package com.example.calisthenicsapp.Repos;

import com.example.calisthenicsapp.CoreComponents.Exercise;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ExerciseRepository implements Repository<Exercise> {
    private final SQLiteDatabase db;

    public ExerciseRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(Exercise e) {
        return Repository.insertDB(db, "Exercise", toContentValues(e));
    }

    @Override
    public boolean update(Exercise e) {
        return Repository.updateDB(db, "Exercise", toContentValues(e),
                "Name = ?", e.getName());
    }

    @Override
    public boolean delete(Exercise e) {
        // false if a workout still uses it (ON DELETE RESTRICT)
        return Repository.deleteDB(db, "Exercise", "Name = ?", e.getName());
    }

    @Override
    public List<Exercise> getAll() {
        List<Exercise> list = new ArrayList<>();
        try (Cursor c = db.query("Exercise", null, null, null, null, null, "Name ASC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public Exercise getOne(String name) {
        try (Cursor c = db.query("Exercise", null, "Name = ?",
                new String[]{name}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    private ContentValues toContentValues(Exercise e) {
        ContentValues cv = new ContentValues();
        cv.put("Name", e.getName());
        cv.put("Instructions", e.getInstructions());
        cv.put("IsTimed", e.isTimed() ? 1 : 0);
        // NULL, not 0: the schema has CHECK (DefaultSeconds > 0) and CHECK (DefaultReps > 0)
        Repository.putIntOrNull(cv, "DefaultSeconds", e.getDefaultSeconds());
        Repository.putIntOrNull(cv, "DefaultReps", e.getDefaultReps());
        cv.put("Met", e.getMet());
        cv.put("AnimationFile", e.getAnimationFile());
        cv.put("MuscleGroup", e.getMuscleGroup());
        cv.put("Difficulty", e.getDifficulty());
        return cv;
    }

    private Exercise fromCursor(Cursor c) {
        return new Exercise(
                c.getString(c.getColumnIndexOrThrow("Name")),
                c.getString(c.getColumnIndexOrThrow("Instructions")),
                c.getInt(c.getColumnIndexOrThrow("IsTimed")) == 1,
                Repository.intOrNull(c, "DefaultSeconds"),
                Repository.intOrNull(c, "DefaultReps"),
                c.getDouble(c.getColumnIndexOrThrow("Met")),
                c.getString(c.getColumnIndexOrThrow("AnimationFile")),
                c.getString(c.getColumnIndexOrThrow("MuscleGroup")),
                c.getString(c.getColumnIndexOrThrow("Difficulty")));
    }
}