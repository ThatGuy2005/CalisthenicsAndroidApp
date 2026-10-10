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
        cv.put("Username", object.getUsername());
        cv.put("Email", object.getEmail());
        cv.put("PasswordHash", object.getPasswordHash());
        cv.put("Height", object.getHeight());
        cv.put("Weight", object.getWeight());
        cv.put("Gender", object.getGender());
        cv.put("Mode", object.getMode());
        return Repository.insertDB(db,"Users",cv);
    };

    @Override
    public boolean delete(User object) {
        return Repository.deleteDB(db, "Users", "Username = ?", object.getUsername());
    };

    @Override
    public boolean update(User object) {
        ContentValues cv = new ContentValues();
        cv.put("Email", object.getEmail());
        cv.put("PasswordHash", object.getPasswordHash());
        cv.put("Height", object.getHeight());
        cv.put("Weight", object.getWeight());
        cv.put("Gender", object.getGender());
        cv.put("Mode", object.getMode());
        return Repository.updateDB(db, "Users", cv,
                "Username = ?", object.getUsername());
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<User>();
        User user;
        try(Cursor cursor = db.query("Users",null,null,
            null, null, null, null)) {
            while (cursor.moveToNext()) {
                user = new User(
                        cursor.getString(cursor.getColumnIndexOrThrow("Username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("PasswordHash")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("Height")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("Weight")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Gender")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Mode")));
                users.add(user);
            }
        }
        return users;
    };

    public User getOne(String username) {
        User user;
        try (Cursor cursor = db.query("Users", null, "Username = ?",
                new String[]{username}, null, null, null)) {
            if(cursor.moveToFirst()) {
                user = new User(
                        cursor.getString(cursor.getColumnIndexOrThrow("Username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("PasswordHash")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("Height")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("Weight")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Gender")),
                        cursor.getString(cursor.getColumnIndexOrThrow("Mode")));
                return user;
            } else
            {
                return null;
            }
        }
    }

    private SQLiteDatabase db;
}
