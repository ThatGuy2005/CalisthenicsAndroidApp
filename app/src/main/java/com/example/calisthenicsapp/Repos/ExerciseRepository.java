package com.example.calisthenicsapp.Repos;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Equipment;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

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

    /**
     * Exercises matching every filter that is not null, sorted by name. nameQuery matches
     * anywhere in the name; muscleGroup and difficulty must match whole. All ignore case.
     */
    public List<Exercise> search(String nameQuery, String muscleGroup, Difficulty difficulty,
                                 Equipment equipment) {
        List<String> conditions = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if (nameQuery != null) {
            // Escape LIKE's wildcards so a search for "50%" or "t_" matches them literally.
            conditions.add("Name LIKE ? ESCAPE '\\'");
            args.add("%" + nameQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (muscleGroup != null) {
            conditions.add("MuscleGroup = ? COLLATE NOCASE");
            args.add(muscleGroup);
        }
        if (difficulty != null) {
            conditions.add("Difficulty = ? COLLATE NOCASE");
            args.add(difficulty.getLabel());
        }
        if (equipment != null) {
            conditions.add("Equipment = ?");
            args.add(equipment.getLabel());
        }
        String where = conditions.isEmpty() ? null : TextUtils.join(" AND ", conditions);

        List<Exercise> list = new ArrayList<>();
        try (Cursor c = db.query("Exercise", null, where, args.toArray(new String[0]),
                null, null, "Name ASC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    /** The distinct muscle groups in the library, sorted, for building the filter options. */
    public List<String> getMuscleGroups() {
        List<String> list = new ArrayList<>();
        try (Cursor c = db.query(true, "Exercise", new String[]{"MuscleGroup"},
                "MuscleGroup IS NOT NULL", null, null, null, "MuscleGroup ASC", null)) {
            while (c.moveToNext()) list.add(c.getString(0));
        }
        return list;
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
        if (e.getDifficulty() != null) cv.put("Difficulty", e.getDifficulty().getLabel());
        else cv.putNull("Difficulty");
        cv.put("IsPerSide", e.isPerSide() ? 1 : 0);
        cv.put("Equipment", e.getEquipment().getLabel());
        cv.put("IsMultiMuscle", e.isMultiMuscle() ? 1 : 0);
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
                Difficulty.fromLabel(c.getString(c.getColumnIndexOrThrow("Difficulty"))),
                c.getInt(c.getColumnIndexOrThrow("IsPerSide")) == 1,
                Equipment.fromLabel(c.getString(c.getColumnIndexOrThrow("Equipment"))),
                c.getInt(c.getColumnIndexOrThrow("IsMultiMuscle")) == 1);
    }
}