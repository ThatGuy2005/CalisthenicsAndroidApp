package com.example.calisthenicsapp.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Creates and opens the app's SQLite database.
 *
 * On first run it executes two files from app/src/main/assets/:
 *   schema.sql - tables, indexes, trigger (your AndroidAppProjectSQLiteDatabase.sql, renamed)
 *   seed.sql   - the premade exercises and workouts
 * Both run inside one transaction, so a mistake in either file makes creation fail loudly
 * instead of leaving a half-built database.
 *
 * Usage (always on a background thread, the first call creates and seeds the database):
 *   SQLiteDatabase db = SQLManager.getInstance(context).getWritableDatabase();
 *   UserRepository users = new UserRepository(db);
 */
public class SQLManager extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "CalisthenicsApp.db";

    /** Increase this when schema.sql changes (see onUpgrade). */
    public static final int DATABASE_VERSION = 1;

    private static final String SCHEMA_ASSET = "schema.sql";
    private static final String SEED_ASSET = "seed.sql";

    /** Children before parents, so foreign keys never block a DROP. */
    private static final String[] TABLES_CHILDREN_FIRST = {
            "ScheduledWorkout", "WorkoutLog", "CustomExerciseOrder", "CustomWorkout",
            "ExerciseOrder", "Workout", "Exercise", "Users"
    };

    private static SQLManager instance;
    private final Context appContext;

    /** One shared instance for the whole app. */
    public static synchronized SQLManager getInstance(Context context) {
        if (instance == null) {
            instance = new SQLManager(context.getApplicationContext());
        }
        return instance;
    }

    private SQLManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context;
    }

    /**
     * Foreign keys are off by default in SQLite and the setting is per connection.
     * "PRAGMA foreign_keys = ON" inside schema.sql does nothing during onCreate (it runs in a
     * transaction), so it is switched on here for every connection instead.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        runScript(db, SCHEMA_ASSET);
        runScript(db, SEED_ASSET);
    }

    /**
     * DEVELOPMENT ONLY: drops everything and recreates it, so all user data is lost.
     * Before real users exist, replace this with proper ALTER TABLE migrations.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String table : TABLES_CHILDREN_FIRST) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }

    private void runScript(SQLiteDatabase db, String assetName) {
        for (String statement : SqlScriptSplitter.split(readAsset(assetName))) {
            if (statement.regionMatches(true, 0, "PRAGMA", 0, 6)) continue;   // see onConfigure
            db.execSQL(statement);
        }
    }

    private String readAsset(String assetName) {
        try (InputStream in = appContext.getAssets().open(assetName);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                text.append(buffer, 0, read);
            }
            return text.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read asset " + assetName
                    + " (is it in app/src/main/assets/?)", e);
        }
    }
}
