package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.User;

import java.util.ArrayList;
import java.util.List;

public class UserRepository implements Repository<User>{
    public UserRepository(SQLiteDatabase db) {
        this.db = db;
    }
    @Override
    public boolean insert(User object) {
        ContentValues cv = new ContentValues();
        cv.put("Uid", object.getUid());
        cv.put("Username", object.getUsername());
        cv.put("Email", object.getEmail());
        cv.put("Height", object.getHeight());
        cv.put("Weight", object.getWeight());
        cv.put("Gender", object.getGender());
        Repository.putIntOrNull(cv, "BirthYear", object.getBirthYear());
        cv.put("CreatedAt", object.getCreatedAt());
        cv.put("UpdatedAt", object.getUpdatedAt());
        return Repository.insertDB(db,"Users",cv);
    };

    @Override
    public boolean delete(User object) {
        return Repository.deleteDB(db, "Users", "Uid = ?", object.getUid());
    };

    @Override
    public boolean update(User object) {
        ContentValues cv = new ContentValues();
        cv.put("Username", object.getUsername());
        cv.put("Email", object.getEmail());
        cv.put("Height", object.getHeight());
        cv.put("Weight", object.getWeight());
        cv.put("Gender", object.getGender());
        Repository.putIntOrNull(cv, "BirthYear", object.getBirthYear());
        // Keep Firestore's updatedAt. If it is unchanged, the trigger bumps it to now instead.
        cv.put("UpdatedAt", object.getUpdatedAt());
        return Repository.updateDB(db, "Users", cv,
                "Uid = ?", object.getUid());
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<User>();
        User user;
        try(Cursor cursor = db.query("Users",null,null,
            null, null, null, null)) {
            while (cursor.moveToNext()) {
                user = fromCursor(cursor);
                users.add(user);
            }
        }
        return users;
    };

    public User getOne(String uid) {
        try (Cursor cursor = db.query("Users", null, "Uid = ?",
                new String[]{uid}, null, null, null)) {
            if(cursor.moveToFirst()) {
                return fromCursor(cursor);
            } else
            {
                return null;
            }
        }
    }

    private User fromCursor(Cursor c) {
        return new User(
                c.getString(c.getColumnIndexOrThrow("Uid")),
                c.getString(c.getColumnIndexOrThrow("Username")),
                c.getString(c.getColumnIndexOrThrow("Email")),
                c.getDouble(c.getColumnIndexOrThrow("Height")),
                c.getDouble(c.getColumnIndexOrThrow("Weight")),
                c.getString(c.getColumnIndexOrThrow("Gender")),
                Repository.intOrNull(c, "BirthYear"),
                c.getLong(c.getColumnIndexOrThrow("CreatedAt")),
                c.getLong(c.getColumnIndexOrThrow("UpdatedAt")));
    }

    private SQLiteDatabase db;
}
