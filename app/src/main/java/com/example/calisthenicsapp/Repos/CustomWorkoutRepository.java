package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;

import java.util.ArrayList;
import java.util.List;

public class CustomWorkoutRepository implements Repository<CustomWorkout> {
    private final SQLiteDatabase db;

    public CustomWorkoutRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(CustomWorkout w) {            // Id is the Firestore document id
        ContentValues cv = new ContentValues();
        cv.put("Id", w.getId());
        cv.put("Name", w.getName());
        cv.put("Uid", w.getUid());
        try {
            return db.insertOrThrow("CustomWorkout", null, cv) != -1;
        } catch (SQLiteConstraintException e) {
            return false;                               // duplicate (Uid, Name) or unknown user
        }
    }

    @Override
    public boolean update(CustomWorkout w) {            // rename
        ContentValues cv = new ContentValues();
        cv.put("Name", w.getName());
        return Repository.updateDB(db, "CustomWorkout", cv,
                "Id = ?", w.getId());
    }

    @Override
    public boolean delete(CustomWorkout w) {            // its exercise rows cascade
        return Repository.deleteDB(db, "CustomWorkout",
                "Id = ?", w.getId());
    }

    @Override
    public List<CustomWorkout> getAll() {
        return query(null, null);
    }

    public List<CustomWorkout> getByUid(String uid) {
        return query("Uid = ?", new String[]{uid});
    }

    public CustomWorkout getOne(String id) {
        List<CustomWorkout> r = query("Id = ?", new String[]{id});
        return r.isEmpty() ? null : r.get(0);
    }

    private List<CustomWorkout> query(String where, String[] args) {
        List<CustomWorkout> list = new ArrayList<>();
        try (Cursor c = db.query("CustomWorkout", null, where, args,
                null, null, "Name ASC")) {
            while (c.moveToNext()) {
                list.add(new CustomWorkout(
                        c.getString(c.getColumnIndexOrThrow("Id")),
                        c.getString(c.getColumnIndexOrThrow("Name")),
                        c.getString(c.getColumnIndexOrThrow("Uid"))));
            }
        }
        return list;
    }
}
