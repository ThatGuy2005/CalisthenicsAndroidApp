package com.example.calisthenicsapp;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import android.database.sqlite.SQLiteConstraintException;

public class SQLManager {
    public static void enableForeignKeys(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    public static boolean addUser(SQLiteDatabase db, String username, String gmail,
                                  String passwordHash, Double height, Double weight,
                                  String gender, String mode) {
        ContentValues cv = new ContentValues();
        cv.put("Username", username);
        cv.put("Gmail", gmail);
        cv.put("PasswordHash", passwordHash);
        cv.put("Height", height);
        cv.put("Weight", weight);
        cv.put("Gender", gender);
        cv.put("Mode", mode == null ? "User" : mode);
        // CreatedAt / UpdatedAt come from the column defaults.
        return insert(db, "Users", cv);
    }

    /** Updates everything except the username and password hash.*/
    /** If you want to update certain attributes (and if there's no
     * function for that), then, just pass the current state of the user
     * but with the necessary values changed.*/
    public static boolean updateUser(SQLiteDatabase db, String username, String gmail,
                                     Double height, Double weight, String gender, String mode) {
        ContentValues cv = new ContentValues();
        cv.put("Gmail", gmail);
        cv.put("Height", height);
        cv.put("Weight", weight);
        cv.put("Gender", gender);
        cv.put("Mode", mode);
        return update(db, "Users", cv, "Username = ?", username);
    }

    public static boolean updateUserPassword(SQLiteDatabase db, String username, String newPasswordHash) {
        ContentValues cv = new ContentValues();
        cv.put("PasswordHash", newPasswordHash);
        return update(db, "Users", cv, "Username = ?", username);
    }

    /** Renames a user; ON UPDATE CASCADE fixes CustomWorkout.Username automatically. */
    public static boolean changeUsername(SQLiteDatabase db, String oldUsername, String newUsername) {
        ContentValues cv = new ContentValues();
        cv.put("Username", newUsername);
        return update(db, "Users", cv, "Username = ?", oldUsername);
    }

    /** Also deletes the user's custom workouts (ON DELETE CASCADE). */
    public static boolean deleteUser(SQLiteDatabase db, String username) {
        return delete(db, "Users", "Username = ?", username);
    }

    /** Helper functions to select the right tables, add the condition
     * and the keys to the query*/
    private static boolean insert(SQLiteDatabase db, String table, ContentValues cv) {
        try {
            return db.insertOrThrow(table, null, cv) != -1;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    private static boolean update(SQLiteDatabase db, String table, ContentValues cv,
                                  String where, String key) {
        try {
            return db.update(table, cv, where, new String[]{key}) > 0;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    private static boolean delete(SQLiteDatabase db, String table, String where, String key) {
        try {
            return db.delete(table, where, new String[]{key}) > 0;
        } catch (SQLiteConstraintException e) {
            return false; // e.g. exercise still referenced (RESTRICT)
        }
    }
}
