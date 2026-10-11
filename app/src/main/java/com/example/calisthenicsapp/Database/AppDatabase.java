package com.example.calisthenicsapp.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Local database setup for the account screens. */
public class AppDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "calisthenics.db";
    private static final int DATABASE_VERSION = 1;

    public AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Users ("
                + "Username TEXT NOT NULL PRIMARY KEY, "
                + "Email TEXT NOT NULL UNIQUE, "
                + "PasswordHash TEXT NOT NULL, "
                + "Height REAL, "
                + "Weight REAL, "
                + "Gender TEXT, "
                + "Mode TEXT NOT NULL DEFAULT 'User')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep existing accounts. Add future schema migrations here.
    }
}
