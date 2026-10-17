package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
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
public class ScheduledWorkoutRepositoryTest {
    private static final long START = 1792080000L;   // 2026-10-15 16:00:00 UTC
    private static final long DAY = 86_400L;

    private Context context;
    private SQLiteDatabase db;
    private ScheduledWorkoutRepository repository;
    private String premade;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        db = SQLManager.getInstance(context).getWritableDatabase();
        repository = new ScheduledWorkoutRepository(db);
        premade = DatabaseUtils.stringForQuery(db, "SELECT Name FROM Workout LIMIT 1", null);
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')");
        db.execSQL("INSERT INTO CustomWorkout (Id, Name, Uid) VALUES ('cw1', 'Mine', 'u1')");
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    private ScheduledWorkout premade(String id, long at, String rule) {
        return new ScheduledWorkout(id, "u1", premade, null, at, rule, null, null);
    }

    private static List<String> ids(List<ScheduledWorkout> schedules) {
        List<String> ids = new ArrayList<>();
        for (ScheduledWorkout s : schedules) ids.add(s.getId());
        return ids;
    }

    @Test
    public void roundTripsTheRepeatRuleAndCompletedOccurrences() {
        assertTrue(repository.insert(new ScheduledWorkout("s1", "u1", premade, null, START,
                "FREQ=WEEKLY", Arrays.asList(START + 7 * DAY, START), null)));
        ScheduledWorkout read = repository.getOne("s1");
        assertEquals("FREQ=WEEKLY", read.getRecurrenceRule());
        assertEquals(Arrays.asList(START, START + 7 * DAY), new ArrayList<>(read.getCompletedOccurrences()));
        assertNull(read.getCalendarEventId());

        assertTrue(repository.update(premade("s1", START + 3600, null)));
        read = repository.getOne("s1");
        assertEquals(START + 3600, read.getScheduledAt());
        assertNull(read.getRecurrenceRule());
        assertTrue(read.getCompletedOccurrences().isEmpty());
    }

    @Test
    public void calendarEventCanBeLinkedBeforeTheRowExistsAndSurvivesUpdates() {
        repository.setCalendarEventId("s1", 42L);   // as ScheduleService does right after creating it
        assertTrue(repository.insert(premade("s1", START, null)));
        assertEquals(Long.valueOf(42L), repository.getOne("s1").getCalendarEventId());

        repository.update(premade("s1", START + DAY, "FREQ=DAILY"));   // a sync upsert
        assertEquals(Long.valueOf(42L), repository.getOne("s1").getCalendarEventId());

        repository.clearCalendarEventId("s1");
        assertNull(repository.getOne("s1").getCalendarEventId());
    }

    @Test
    public void exactlyOneWorkoutIsRequired() {
        assertFalse(repository.insert(new ScheduledWorkout("both", "u1", premade, "cw1", START)));
        assertFalse(repository.insert(new ScheduledWorkout("none", "u1", null, null, START)));
        assertFalse(repository.insert(new ScheduledWorkout("ghost", "u1", null, "missing", START)));
        assertTrue(repository.insert(new ScheduledWorkout("custom", "u1", null, "cw1", START)));
    }

    @Test
    public void upcomingIncludesRepeatingSchedulesThatStartedEarlier() {
        repository.insert(premade("pastOnce", START - DAY, null));
        repository.insert(premade("pastWeekly", START - 30 * DAY, "FREQ=WEEKLY"));
        repository.insert(premade("future", START + DAY, null));
        assertEquals(Arrays.asList("pastWeekly", "future"), ids(repository.getUpcoming("u1", START)));
    }

    @Test
    public void betweenFindsOneOffsInsideAndRepeatingOnesStartedBeforeTheEnd() {
        repository.insert(premade("before", START - DAY, null));
        repository.insert(premade("inside", START + DAY, null));
        repository.insert(premade("after", START + 10 * DAY, null));
        repository.insert(premade("repeating", START - 30 * DAY, "FREQ=DAILY"));
        repository.insert(premade("repeatingLater", START + 10 * DAY, "FREQ=DAILY"));
        assertEquals(Arrays.asList("repeating", "inside"),
                ids(repository.getBetween("u1", START, START + 7 * DAY)));
    }

    @Test
    public void deletingACustomWorkoutDeletesItsSchedulesButKeepsTheEventLink() {
        repository.insert(new ScheduledWorkout("s1", "u1", null, "cw1", START));
        repository.setCalendarEventId("s1", 7L);
        new CustomWorkoutRepository(db).delete(new CustomWorkout("cw1", "Mine", "u1"));
        assertNull(repository.getOne("s1"));
        // SyncManager reads the link by id when the schedule's REMOVED arrives, to delete the event.
        assertEquals(Long.valueOf(7L), repository.getCalendarEventId("s1"));
        assertEquals(Collections.emptyList(), repository.getByUid("u1"));
    }
}
