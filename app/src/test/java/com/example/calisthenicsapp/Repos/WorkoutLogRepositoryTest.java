package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.CoreComponents.WorkoutStats;
import com.example.calisthenicsapp.Database.SQLManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class WorkoutLogRepositoryTest {
    private static final long WEEK_START = 1792080000L;
    private static final long DAY = 86_400L;

    private WorkoutLogRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        SQLiteDatabase db = SQLManager.getInstance(context).getWritableDatabase();
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')");
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u2', 'b@b.c', 'bob')");
        repository = new WorkoutLogRepository(db);

        log("old", "u1", WEEK_START - 1, "Completed");          // one second too early
        log("boundary", "u1", WEEK_START, "Partial");           // exactly at the start counts
        log("newest", "u1", WEEK_START + 3 * DAY, "Completed");
        log("middle", "u1", WEEK_START + DAY, "Completed");
        log("other user", "u2", WEEK_START + DAY, "Completed");
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    private void log(String id, String uid, long startedAt, String status) {
        assertTrue(repository.insert(new WorkoutLog(id, uid, "Legs", startedAt, 600, 50.0, status,
                Collections.emptyList())));
    }

    private static List<String> ids(List<WorkoutLog> logs) {
        List<String> ids = new ArrayList<>();
        for (WorkoutLog log : logs) ids.add(log.getId());
        return ids;
    }

    @Test
    public void sinceKeepsTheUsersLogsFromTheStartOnNewestFirst() {
        assertEquals(Arrays.asList("newest", "middle", "boundary"),
                ids(repository.getByUidSince("u1", WEEK_START)));
        assertEquals(Arrays.asList("newest", "middle", "boundary", "old"),
                ids(repository.getByUidSince("u1", 0)));
    }

    @Test
    public void listAndStatsCoverTheSameLogs() {
        for (long from : new long[]{0, WEEK_START, WEEK_START + 1, WEEK_START + 10 * DAY}) {
            List<WorkoutLog> logs = repository.getByUidSince("u1", from);
            WorkoutStats stats = repository.getStats("u1", from);
            assertEquals(stats.getWorkouts(), logs.size());
            long seconds = 0;
            for (WorkoutLog log : logs) seconds += log.getDurationSeconds();
            assertEquals(stats.getTotalSeconds(), seconds);
        }
    }
}
