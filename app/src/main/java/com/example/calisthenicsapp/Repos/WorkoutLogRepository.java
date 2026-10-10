package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;

import java.util.ArrayList;
import java.util.List;

public class WorkoutLogRepository implements Repository<WorkoutLog> {
    private final SQLiteDatabase db;

    public WorkoutLogRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(WorkoutLog l) {               // Id is autoincremented
        ContentValues cv = new ContentValues();
        cv.put("Username", l.getUsername());
        cv.put("WorkoutName", l.getWorkoutName());
        cv.put("StartedAt", l.getStartedAt());
        cv.put("DurationSeconds", l.getDurationSeconds());
        cv.put("Kcal", l.getKcal());
        return Repository.insertDB(db, "WorkoutLog", cv);
    }

    @Override
    public boolean update(WorkoutLog l) {
        ContentValues cv = new ContentValues();
        cv.put("WorkoutName", l.getWorkoutName());
        cv.put("StartedAt", l.getStartedAt());
        cv.put("DurationSeconds", l.getDurationSeconds());
        cv.put("Kcal", l.getKcal());
        return Repository.updateDB(db, "WorkoutLog", cv,
                "Id = ?", String.valueOf(l.getId()));
    }

    @Override
    public boolean delete(WorkoutLog l) {
        return Repository.deleteDB(db, "WorkoutLog", "Id = ?", String.valueOf(l.getId()));
    }

    @Override
    public List<WorkoutLog> getAll() {
        return query(null, null);
    }

    public List<WorkoutLog> getByUsername(String username) {
        return query("Username = ?", new String[]{username});   // newest first
    }

    private List<WorkoutLog> query(String where, String[] args) {
        List<WorkoutLog> list = new ArrayList<>();
        try (Cursor c = db.query("WorkoutLog", null, where, args,
                null, null, "StartedAt DESC")) {
            while (c.moveToNext()) {
                list.add(new WorkoutLog(
                        c.getLong(c.getColumnIndexOrThrow("Id")),
                        c.getString(c.getColumnIndexOrThrow("Username")),
                        c.getString(c.getColumnIndexOrThrow("WorkoutName")),
                        c.getLong(c.getColumnIndexOrThrow("StartedAt")),
                        c.getInt(c.getColumnIndexOrThrow("DurationSeconds")),
                        c.getDouble(c.getColumnIndexOrThrow("Kcal"))));
            }
        }
        return list;
    }
}
