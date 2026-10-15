package com.example.calisthenicsapp.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Database.SQLManager;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Session.WorkoutSession.SessionExercise;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class WorkoutExercisesTest {
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        db = SQLManager.getInstance(context).getWritableDatabase();
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    @Test
    public void noExercisesTakeNoTime() {
        assertEquals(0, WorkoutExercises.estimateSeconds(Collections.emptyList()));
    }

    @Test
    public void estimateCountsBothRestsButNotTheRestAfterTheLastExercise() {
        assertEquals(390, WorkoutExercises.estimateSeconds(Arrays.asList(
                // 3 x 10 reps x 3 s + 2 x 60 s between sets + 90 s before the plank = 300
                new SessionExercise("Push-Up", null, null, false, 10, null, 3, 60, 90, 3.8),
                // 2 x 30 s + 1 x 30 s between sets = 90; its 45 s after rest is not counted
                new SessionExercise("Plank", null, null, true, null, 30, 2, 30, 45, 3.8))));
    }

    /** Fills in the exercise defaults from the seed: 15 reps of Crunches, 30 s of Plank, ... */
    @Test
    public void coreBasicsEstimateUsesTheExerciseDefaults() {
        // Crunches 3 x 15 x 3 s + 2 x 30 s between sets + 60 s before the next exercise = 255,
        // Plank 3 x 30 s + 60 + 60 = 210, Superman Hold 3 x 20 s + 60 + 60 = 180,
        // Glute Bridge 3 x 15 x 3 s + 60 = 195 (no rest after the last exercise)
        assertEquals(840, WorkoutExercises.estimateSeconds(
                WorkoutExercises.ofPremade(db, "Core Basics")));
    }

    @Test
    public void premadeRestsComeFromTheSeed() {
        SessionExercise pushUp = WorkoutExercises.ofPremade(db, "Upper Body Strength").get(0);
        assertEquals("Push-Up", pushUp.getName());
        assertEquals(60, pushUp.getRestSeconds());
        assertEquals(90, pushUp.getRestAfterSeconds());
    }

    @Test
    public void everyPremadeWorkoutHasALength() {
        List<Workout> all = new WorkoutRepository(db).getAll();
        assertFalse(all.isEmpty());
        for (Workout w : all) {
            int seconds = WorkoutExercises.estimateSeconds(WorkoutExercises.ofPremade(db, w.getName()));
            assertTrue(w.getName(), seconds > 0);
        }
    }
}
