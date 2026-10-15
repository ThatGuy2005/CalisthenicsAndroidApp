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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

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

    /** Fills in the exercise defaults from the seed: 15 reps of Crunches, 30 s of Plank, ... */
    @Test
    public void coreBasicsEstimateUsesTheExerciseDefaults() {
        // Crunches 3 x (15 x 3 s + 30 s rest) = 225, Plank 3 x (30 s + 30 s) = 180,
        // Superman Hold 3 x (20 s + 30 s) = 150, Glute Bridge 3 x (15 x 3 s + 30 s) = 225,
        // minus the rest after the last set: 780 - 30
        assertEquals(750, WorkoutExercises.estimateSeconds(
                WorkoutExercises.ofPremade(db, "Core Basics")));
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
