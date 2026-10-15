package com.example.calisthenicsapp.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates and opens the app's SQLite database.
 *
 * On first run it executes two files from app/src/main/assets/:
 *   schema.sql - tables, indexes, trigger (your AndroidAppProjectSQLiteDatabase.sql, renamed)
 *   seed.sql   - the premade exercises and workouts
 * Both run inside one transaction, so a mistake in either file makes creation fail loudly
 * instead of leaving a half-built database. Later versions upgrade an existing database step
 * by step (see onUpgrade), also inside one transaction.
 *
 * Usage (always on a background thread, the first call creates and seeds the database):
 *   SQLiteDatabase db = SQLManager.getInstance(context).getWritableDatabase();
 *   UserRepository users = new UserRepository(db);
 */
public class SQLManager extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "CalisthenicsApp.db";

    /** Increase this when schema.sql or seed.sql changes, and add the step to onUpgrade. */
    public static final int DATABASE_VERSION = 7;

    /** Older databases are dropped and recreated instead of upgraded, see onUpgrade. */
    private static final int OLDEST_UPGRADABLE_VERSION = 3;

    /** The premade tables, in the order seed.sql fills them. */
    private static final String[] SEED_TABLES = {"Exercise", "Workout", "ExerciseOrder"};
    private static final Pattern SEED_INSERT = Pattern.compile(
            "^INSERT\\s+INTO\\s+(Exercise|Workout|ExerciseOrder)\\b", Pattern.CASE_INSENSITIVE);

    private static final String SCHEMA_ASSET = "schema.sql";
    private static final String SEED_ASSET = "seed.sql";

    /** Children before parents, so foreign keys never block a DROP. */
    private static final String[] TABLES_CHILDREN_FIRST = {
            "CalendarEvent", "ScheduledWorkout", "WorkoutLog", "CustomExerciseOrder", "CustomWorkout",
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

    /** For tests: closes the shared instance, so the next getInstance opens the file again. */
    @VisibleForTesting
    public static synchronized void closeInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
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
     * Upgrades the database step by step, keeping the user's data (DR-7). The per-user tables
     * are a cache of Firestore, but CalendarEvent exists only on this device.
     *
     * To change the schema: edit schema.sql (used by new installs), raise DATABASE_VERSION to N,
     * and add an "if (oldVersion < N)" step below that turns version N-1 into N. Every upgrade
     * also brings the premade content up to date with seed.sql (refreshSeed), so a seed-only
     * change just needs the version raised.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < OLDEST_UPGRADABLE_VERSION) {
            // Development builds from before workout logs had a status; nothing worth keeping.
            for (String table : TABLES_CHILDREN_FIRST) {
                db.execSQL("DROP TABLE IF EXISTS " + table);
            }
            onCreate(db);
            return;
        }
        if (oldVersion < 4) {   // calendar events of schedules (FR-6.2)
            db.execSQL("CREATE TABLE IF NOT EXISTS CalendarEvent ("
                    + "ScheduleId TEXT PRIMARY KEY NOT NULL, EventId INTEGER NOT NULL)");
        }
        if (oldVersion < 5) {   // repeating schedules (FR-6.1)
            db.execSQL("ALTER TABLE ScheduledWorkout ADD COLUMN RecurrenceRule TEXT");
        }
        if (oldVersion < 6) {   // completed occurrences (FR-6.1)
            db.execSQL("ALTER TABLE ScheduledWorkout ADD COLUMN Completed TEXT");
        }
        if (oldVersion < 7) {   // workout categories (FR-4.1); refreshSeed fills them in
            db.execSQL("ALTER TABLE Workout ADD COLUMN Category TEXT");
        }
        refreshSeed(db);
    }

    /**
     * Brings the premade exercises and workouts up to date with seed.sql without touching the
     * user's own data (DR-7). seed.sql is loaded into empty temp copies of the premade tables
     * and merged from there: changed rows are updated in place and new rows added. Premade rows
     * are never deleted or replaced, because custom workouts and schedules point at them (and
     * deleting a workout would cascade to its schedules). Each premade workout in seed.sql gets
     * exactly the exercises listed there.
     */
    private void refreshSeed(SQLiteDatabase db) {
        for (String table : SEED_TABLES) {
            db.execSQL("DROP TABLE IF EXISTS temp.Seed" + table);
            db.execSQL("CREATE TEMP TABLE Seed" + table + " AS SELECT * FROM main." + table + " WHERE 0");
        }
        for (String statement : SqlScriptSplitter.split(readAsset(SEED_ASSET))) {
            if (statement.regionMatches(true, 0, "PRAGMA", 0, 6)) continue;
            Matcher insert = SEED_INSERT.matcher(statement);
            if (!insert.find()) {
                throw new IllegalStateException("seed.sql may only insert into the premade tables, not: "
                        + statement.substring(0, Math.min(60, statement.length())));
            }
            db.execSQL(insert.replaceFirst("INSERT INTO temp.Seed$1"));
        }
        mergeByKey(db, "Exercise", "Name");
        mergeByKey(db, "Workout", "Name");
        db.execSQL("DELETE FROM ExerciseOrder WHERE WorkoutName IN (SELECT Name FROM temp.SeedWorkout)");
        db.execSQL("INSERT INTO ExerciseOrder SELECT * FROM temp.SeedExerciseOrder");
        for (String table : SEED_TABLES) {
            db.execSQL("DROP TABLE temp.Seed" + table);
        }
    }

    /** Sets the rows that temp.Seed{table} also has to its values, and adds the rows it adds. */
    private static void mergeByKey(SQLiteDatabase db, String table, String key) {
        StringBuilder set = new StringBuilder();
        for (String column : columnsOf(db, table)) {
            if (column.equals(key)) continue;
            if (set.length() > 0) set.append(", ");
            set.append(column).append(" = (SELECT s.").append(column)
                    .append(" FROM temp.Seed").append(table).append(" s WHERE s.").append(key)
                    .append(" = ").append(table).append('.').append(key).append(')');
        }
        if (set.length() > 0) {
            db.execSQL("UPDATE " + table + " SET " + set
                    + " WHERE " + key + " IN (SELECT " + key + " FROM temp.Seed" + table + ")");
        }
        db.execSQL("INSERT OR IGNORE INTO " + table + " SELECT * FROM temp.Seed" + table);
    }

    private static List<String> columnsOf(SQLiteDatabase db, String table) {
        List<String> columns = new ArrayList<>();
        try (Cursor c = db.rawQuery("PRAGMA main.table_info(" + table + ")", null)) {
            int nameCol = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) columns.add(c.getString(nameCol));
        }
        return columns;
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
