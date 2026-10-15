package com.example.calisthenicsapp.Database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Creating and upgrading the real database file (DR-7). An old version is built from the
 * schema.sql of that version (test resources) and the seed, then opened through SQLManager.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class SQLManagerTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    @Test
    public void freshInstallHasTheSchemaAndThePremadeContent() {
        SQLiteDatabase db = open();
        assertEquals(SQLManager.DATABASE_VERSION, db.getVersion());
        assertTrue(count(db, "Exercise") > 0);
        assertTrue(count(db, "Workout") > 0);
        assertTrue(count(db, "ExerciseOrder") > 0);
        assertTrue(columns(db, "ScheduledWorkout").contains("RecurrenceRule"));
        assertTrue(columns(db, "Workout").contains("Category"));
        assertTrue(columns(db, "ExerciseOrder").contains("RestAfterSeconds"));
        assertTrue(columns(db, "CustomExerciseOrder").contains("RestAfterSeconds"));
        assertTrue(columns(db, "Exercise").contains("IsPerSide"));
        assertEquals(0, count(db, "CalendarEvent"));
    }

    @Test
    public void upgradeFromVersion3KeepsUserDataAndMatchesAFreshInstall() throws IOException {
        createVersion3(
                "INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')",
                "INSERT INTO CustomWorkout (Id, Name, Uid) VALUES ('cw1', 'Mine', 'u1')",
                "INSERT INTO CustomExerciseOrder (CustomWorkoutId, ExerciseName, Position, Sets)"
                        + " SELECT 'cw1', Name, 1, 4 FROM Exercise LIMIT 1",
                "INSERT INTO ScheduledWorkout (Id, Uid, WorkoutName, ScheduledAt)"
                        + " SELECT 's1', 'u1', Name, 1792080000 FROM Workout LIMIT 1",
                "INSERT INTO ScheduledWorkout (Id, Uid, CustomWorkoutId, ScheduledAt)"
                        + " VALUES ('s2', 'u1', 'cw1', 1792080000)",
                "INSERT INTO WorkoutLog (Id, Uid, WorkoutName, StartedAt, DurationSeconds, Kcal)"
                        + " VALUES ('l1', 'u1', 'Mine', 1, 60, 5)");

        SQLiteDatabase db = open();
        assertEquals(SQLManager.DATABASE_VERSION, db.getVersion());
        assertEquals(1, count(db, "Users"));
        assertEquals(1, count(db, "CustomWorkout"));
        assertEquals(1, count(db, "CustomExerciseOrder"));
        assertEquals(2, count(db, "ScheduledWorkout"));
        assertEquals(1, count(db, "WorkoutLog"));

        // The new columns and table work with the existing rows.
        ScheduledWorkoutRepository schedules = new ScheduledWorkoutRepository(db);
        ScheduledWorkout s1 = schedules.getOne("s1");
        assertNotNull(s1);
        assertEquals(null, s1.getRecurrenceRule());
        assertTrue(s1.getCompletedOccurrences().isEmpty());
        schedules.setCalendarEventId("s1", 42L);
        assertEquals(Long.valueOf(42L), schedules.getOne("s1").getCalendarEventId());

        String upgraded = describeSchema(db);
        String upgradedContent = premadeContent(db);
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        SQLiteDatabase fresh = open();
        assertEquals(describeSchema(fresh), upgraded);
        assertEquals(premadeContent(fresh), upgradedContent);
    }

    @Test
    public void upgradeRefreshesPremadeContentWithoutTouchingCustomWorkouts() throws IOException {
        SQLiteDatabase fresh = open();
        String expected = premadeContent(fresh);
        String exercise = DatabaseUtils.stringForQuery(fresh, "SELECT Name FROM Exercise LIMIT 1", null);
        String workout = DatabaseUtils.stringForQuery(fresh, "SELECT Name FROM Workout LIMIT 1", null);
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);

        // An old install whose premade content is out of date, with the user's own data on top.
        createVersion3(
                "UPDATE Exercise SET Instructions = 'old text', Met = 1.0 WHERE Name = '" + exercise + "'",
                "DELETE FROM ExerciseOrder WHERE WorkoutName = '" + workout + "' AND Position = 1",
                "DELETE FROM Workout WHERE Name <> '" + workout + "'",
                "INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')",
                "INSERT INTO CustomWorkout (Id, Name, Uid) VALUES ('cw1', 'Mine', 'u1')",
                "INSERT INTO CustomExerciseOrder (CustomWorkoutId, ExerciseName, Position, Sets, Reps)"
                        + " VALUES ('cw1', '" + exercise + "', 1, 7, 33)",
                "INSERT INTO ScheduledWorkout (Id, Uid, WorkoutName, ScheduledAt)"
                        + " VALUES ('s1', 'u1', '" + workout + "', 1792080000)");

        SQLiteDatabase db = open();
        assertEquals(expected, premadeContent(db));   // changed, missing and removed rows are back
        assertEquals("7|33", DatabaseUtils.stringForQuery(db,
                "SELECT Sets || '|' || Reps FROM CustomExerciseOrder WHERE CustomWorkoutId = 'cw1'", null));
        assertEquals(1, count(db, "ScheduledWorkout"));   // not cascaded away
    }

    @Test
    public void databasesOlderThanVersion3AreRecreated() throws IOException {
        createDatabase(2, readResource("schema_v3.sql"),
                "INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')");
        SQLiteDatabase db = open();
        assertEquals(SQLManager.DATABASE_VERSION, db.getVersion());
        assertEquals(0, count(db, "Users"));
        assertTrue(count(db, "Exercise") > 0);
        assertTrue(columns(db, "ScheduledWorkout").contains("Completed"));
    }

    // ---------- Helpers ----------

    private SQLiteDatabase open() {
        return SQLManager.getInstance(context).getWritableDatabase();
    }

    /**
     * A version 3 database with the old premade content (seed_v6.sql: the current seed.sql may
     * fill columns version 3 does not have), then the given statements.
     */
    private void createVersion3(String... statements) throws IOException {
        List<String> all = new ArrayList<>();
        for (String s : SqlScriptSplitter.split(readResource("seed_v6.sql"))) all.add(s);
        for (String s : statements) all.add(s);
        createDatabase(3, readResource("schema_v3.sql"), all.toArray(new String[0]));
    }

    /** The database file as an older app version would have left it. */
    private void createDatabase(int version, String schema, String... statements) {
        File file = context.getDatabasePath(SQLManager.DATABASE_NAME);
        assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
        try {
            db.setForeignKeyConstraintsEnabled(true);
            for (String s : SqlScriptSplitter.split(schema)) {
                if (!s.regionMatches(true, 0, "PRAGMA", 0, 6)) db.execSQL(s);
            }
            for (String s : statements) db.execSQL(s);
            db.setVersion(version);
        } finally {
            db.close();
        }
    }

    /** Every table's columns, and every index and trigger, in a stable order. */
    private static String describeSchema(SQLiteDatabase db) {
        Map<String, String> parts = new TreeMap<>();
        try (Cursor c = db.rawQuery("SELECT type, name FROM sqlite_master"
                + " WHERE name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'", null)) {
            while (c.moveToNext()) {
                String type = c.getString(0);
                String name = c.getString(1);
                parts.put(type + " " + name, type.equals("table") ? columnInfo(db, name) : "");
            }
        }
        return parts.toString();
    }

    private static String columnInfo(SQLiteDatabase db, String table) {
        StringBuilder info = new StringBuilder();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                info.append(c.getString(1)).append(':').append(c.getString(2))
                        .append(c.getInt(3) == 1 ? " notnull" : "")
                        .append(c.getInt(5) > 0 ? " pk" : "").append("; ");
            }
        }
        return info.toString();
    }

    private static String premadeContent(SQLiteDatabase db) {
        return dump(db, "SELECT * FROM Exercise ORDER BY Name")
                + dump(db, "SELECT * FROM Workout ORDER BY Name")
                + dump(db, "SELECT * FROM ExerciseOrder ORDER BY WorkoutName, Position");
    }

    private static String dump(SQLiteDatabase db, String sql) {
        StringBuilder out = new StringBuilder();
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                for (int i = 0; i < c.getColumnCount(); i++) out.append(c.getString(i)).append('|');
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static List<String> columns(SQLiteDatabase db, String table) {
        List<String> names = new ArrayList<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) names.add(c.getString(1));
        }
        return names;
    }

    private static long count(SQLiteDatabase db, String table) {
        return DatabaseUtils.queryNumEntries(db, table);
    }

    private String readResource(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull("Missing test resource " + name, in);
            return readAll(in);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
