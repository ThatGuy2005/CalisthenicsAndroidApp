package com.example.calisthenicsapp.Export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.LoggedExercise;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

/** Robolectric because org.json is part of Android. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class JsonExportTest {
    private static final long T = 1792080000L;   // 2026-10-15T16:00:00Z

    private static ExportData sample(User user) {
        CustomWorkout workout = new CustomWorkout("cw1", "Legs", "u1", null, null);
        WorkoutLog log = new WorkoutLog("l1", "u1", "Legs", T, 600, 42.5, "Completed",
                Collections.singletonList(new LoggedExercise("Squat", 5.0, false, 3, 3, 12, null, 90, 21.2)));
        ScheduledWorkout weekly = new ScheduledWorkout("s1", "u1", null, "cw1", T, "FREQ=WEEKLY",
                Arrays.asList(T + 7 * 86_400L, T), null);
        return new ExportData("u1", user, Collections.singletonList(workout),
                Collections.singletonMap("cw1", Arrays.asList(
                        new CustomExerciseOrderEntry("cw1", "Squat", 1, 3, 12, null, 60, 90),
                        new CustomExerciseOrderEntry("cw1", "Plank", 2, 2, null, 30, 45))),
                Collections.singletonList(log), Collections.singletonList(weekly));
    }

    @Test
    public void holdsEveryKindOfRecordWithUnsetValuesAsNull() throws JSONException {
        JSONObject root = JsonExport.build(
                sample(new User("u1", "alice", "a@b.c", 170, 0, null, T, T + 60)), T + 120);

        assertEquals(T + 120, root.getLong("exportedAt"));
        JSONObject profile = root.getJSONObject("profile");
        assertEquals("alice", profile.getString("username"));
        assertTrue(profile.isNull("gender"));

        JSONObject workout = root.getJSONArray("customWorkouts").getJSONObject(0);
        assertTrue(workout.isNull("description"));
        assertEquals(2, workout.getJSONArray("exercises").length());
        JSONObject plank = workout.getJSONArray("exercises").getJSONObject(1);
        assertEquals(30, plank.getInt("seconds"));
        assertTrue(plank.isNull("reps"));

        JSONObject log = root.getJSONArray("workoutLogs").getJSONObject(0);
        assertEquals("Squat", log.getJSONArray("exercises").getJSONObject(0).getString("exerciseName"));

        JSONObject schedule = root.getJSONArray("schedules").getJSONObject(0);
        assertTrue(schedule.isNull("workoutName"));
        assertEquals("FREQ=WEEKLY", schedule.getString("recurrenceRule"));
        assertEquals(2, schedule.getJSONArray("completedOccurrences").length());
    }

    @Test
    public void profileNotSyncedYetHasOnlyTheUid() throws JSONException {
        JSONObject profile = JsonExport.build(sample(null), T).getJSONObject("profile");

        assertEquals("u1", profile.getString("uid"));
        assertFalse(profile.has("username"));
    }
}
