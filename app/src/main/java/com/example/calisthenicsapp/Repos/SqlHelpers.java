package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

/** SQLite helpers shared by the repositories. */
final class SqlHelpers {
    private SqlHelpers() {
    }

    static Integer intOrNull(Cursor c, String column) {
        int idx = c.getColumnIndexOrThrow(column);
        return c.isNull(idx) ? null : c.getInt(idx);
    }

    static void putIntOrNull(ContentValues cv, String key, Integer value) {
        if (value == null) cv.putNull(key);
        else cv.put(key, value);
    }

    static boolean insertDB(SQLiteDatabase db, String table, ContentValues cv) {
        try {
            return db.insertOrThrow(table, null, cv) != -1;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    static boolean updateDB(SQLiteDatabase db, String table, ContentValues cv,
                            String where, String... args) {
        try {
            return db.update(table, cv, where, args) > 0;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    static boolean deleteDB(SQLiteDatabase db, String table, String where, String... args) {
        try {
            return db.delete(table, where, args) > 0;
        } catch (SQLiteConstraintException e) {
            return false; // e.g. a row another table still references (ON DELETE RESTRICT)
        }
    }
}
