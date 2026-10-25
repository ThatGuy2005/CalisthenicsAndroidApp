package com.example.calisthenicsapp.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.database.SQLManager;
import com.example.calisthenicsapp.repository.CustomWorkoutRepository;
import com.example.calisthenicsapp.repository.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.repository.UserRepository;
import com.example.calisthenicsapp.repository.WorkoutLogRepository;
import com.example.calisthenicsapp.scheduling.ScheduleCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** What the sync writes to SQLite, without Firestore: the snapshots are made up here. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class LocalMirrorTest {
    private static final long START = 1792080000L;   // 2026-10-15 16:00:00 UTC

    private SQLiteDatabase db;
    private LocalMirror mirror;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        db = SQLManager.getInstance(context).getWritableDatabase();
        mirror = new LocalMirror("u1", db, new ScheduleCalendar(context, db));
        mirror.applyUser(user("alice"));
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    private static RemoteUser user(String username) {
        return new RemoteUser("u1", username, "a@b.c", 180.0, 75.0, null, 2000, 1L, 2L);
    }

    private static RemoteCustomWorkout customWorkout(String id) {
        return new RemoteCustomWorkout(id, "Mine", null, Collections.emptyList());
    }

    private static RemoteWorkoutLog log(String id) {
        return new RemoteWorkoutLog(id, "Legs", START, 600, 50.0, "Completed",
                Collections.emptyList());
    }

    private static RemoteSchedule schedule(String id, String workoutName, String customWorkoutId) {
        return new RemoteSchedule(id, workoutName, customWorkoutId, START, null,
                Collections.emptyList());
    }

    private static <T> List<T> none() {
        return Collections.emptyList();
    }

    @Test
    public void profileIsInsertedThenUpdated() {
        mirror.applyUser(user("alicia"));

        assertEquals("alicia", new UserRepository(db).getOne("u1").getUsername());
    }

    @Test
    public void scheduleWaitsForItsCustomWorkout() {
        ScheduledWorkoutRepository schedules = new ScheduledWorkoutRepository(db);

        assertTrue(mirror.applySchedules(Arrays.asList(schedule("s1", null, "cw1")), none(), null));
        assertNull(schedules.getOne("s1"));

        mirror.applyCustomWorkouts(Arrays.asList(customWorkout("cw1")), none(), null);
        assertNotNull(schedules.getOne("s1"));
    }

    @Test
    public void premadeScheduleIsMirroredAtOnce() {
        String premade = DatabaseUtils.stringForQuery(db, "SELECT Name FROM Workout LIMIT 1", null);

        mirror.applySchedules(Arrays.asList(schedule("s1", premade, null)), none(), null);

        assertNotNull(new ScheduledWorkoutRepository(db).getOne("s1"));
    }

    @Test
    public void serverListDeletesRowsItLacks() {
        WorkoutLogRepository logs = new WorkoutLogRepository(db);
        mirror.applyWorkoutLogs(Arrays.asList(log("a"), log("b")), none(), null);

        assertTrue(mirror.applyWorkoutLogs(none(), none(), new HashSet<>(Arrays.asList("a"))));

        assertNotNull(logs.getOne("a"));
        assertNull(logs.getOne("b"));
    }

    @Test
    public void cacheSnapshotDeletesNothing() {
        mirror.applyWorkoutLogs(Arrays.asList(log("a")), none(), null);

        assertFalse(mirror.applyWorkoutLogs(none(), none(), null));

        assertNotNull(new WorkoutLogRepository(db).getOne("a"));
    }

    @Test
    public void removedDocumentsGo() {
        CustomWorkoutRepository workouts = new CustomWorkoutRepository(db);
        mirror.applyCustomWorkouts(Arrays.asList(customWorkout("cw1")), none(), null);

        assertTrue(mirror.applyCustomWorkouts(none(), Arrays.asList("cw1"), null));

        assertNull(workouts.getOne("cw1"));
    }

    @Test
    public void pendingScheduleIsDroppedWhenTheServerLacksIt() {
        mirror.applySchedules(Arrays.asList(schedule("s1", null, "cw1")), none(), null);
        mirror.applySchedules(none(), none(), Collections.emptySet());

        mirror.applyCustomWorkouts(Arrays.asList(customWorkout("cw1")), none(), null);

        assertNull(new ScheduledWorkoutRepository(db).getOne("s1"));
    }

    @Test
    public void clearDeletesTheUsersRows() {
        mirror.applyWorkoutLogs(Arrays.asList(log("a")), none(), null);

        mirror.clear();

        assertNull(new UserRepository(db).getOne("u1"));
        assertTrue(new WorkoutLogRepository(db).getByUid("u1").isEmpty());
    }
}
