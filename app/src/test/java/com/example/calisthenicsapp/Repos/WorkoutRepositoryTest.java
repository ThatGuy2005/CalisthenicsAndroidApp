package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Equipment;
import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.CoreComponents.WorkoutCategory;
import com.example.calisthenicsapp.Database.SQLManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class WorkoutRepositoryTest {
    private SQLiteDatabase db;
    private WorkoutRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        db = SQLManager.getInstance(context).getWritableDatabase();
        repository = new WorkoutRepository(db);
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    /**
     * The Workout table has no CHECK on Level or Category, so this guards seed.sql instead
     * (an unknown label would already fail in fromLabel).
     */
    @Test
    public void everyPremadeWorkoutHasALevelAndACategory() {
        List<Workout> all = repository.getAll();
        assertFalse(all.isEmpty());
        for (Workout w : all) {
            assertNotNull(w.getName(), w.getLevel());
            assertNotNull(w.getName(), w.getCategory());
        }
        assertEquals(WorkoutCategory.CORE, repository.getOne("Core Basics").getCategory());
    }

    /** The seed's descriptions say whether equipment is needed; this keeps them honest. */
    @Test
    public void everyPremadeWorkoutHasADescriptionThatMatchesItsEquipment() {
        ExerciseRepository exercises = new ExerciseRepository(db);
        for (Workout w : repository.getAll()) {
            assertNotNull(w.getName(), w.getDescription());
            boolean needsEquipment = false;
            for (ExerciseOrderEntry e : new ExerciseOrderRepository(db).getByWorkout(w.getName())) {
                if (exercises.getOne(e.getExerciseName()).getEquipment() != Equipment.NONE) {
                    needsEquipment = true;
                }
            }
            assertEquals(w.getName(), !needsEquipment,
                    w.getDescription().contains("No equipment needed"));
        }
    }

    @Test
    public void descriptionIsReadBack() {
        insertWorkout("Test Desc", Difficulty.BEGINNER, WorkoutCategory.CORE, "Short and easy.");
        assertEquals("Short and easy.", repository.getOne("Test Desc").getDescription());
        insertWorkout("Test No Desc", Difficulty.BEGINNER, WorkoutCategory.CORE, null);
        assertNull(repository.getOne("Test No Desc").getDescription());
    }

    @Test
    public void categoryIsReadFromTheLabel() {
        insertWorkout("Test Legs", Difficulty.BEGINNER, WorkoutCategory.LOWER_BODY, null);
        assertEquals(WorkoutCategory.LOWER_BODY, repository.getOne("Test Legs").getCategory());
        insertWorkout("Test Cardio", Difficulty.BEGINNER, WorkoutCategory.CARDIO, null);
        assertEquals(WorkoutCategory.CARDIO, repository.getOne("Test Cardio").getCategory());
        insertWorkout("Test No Category", Difficulty.BEGINNER, null, null);
        assertNull(repository.getOne("Test No Category").getCategory());
    }

    @Test
    public void levelIsReadFromTheLabel() {
        insertWorkout("Test Hard", Difficulty.ADVANCED, null, null);
        assertEquals(Difficulty.ADVANCED, repository.getOne("Test Hard").getLevel());
        insertWorkout("Test Medium", Difficulty.INTERMEDIATE, null, null);
        assertEquals(Difficulty.INTERMEDIATE, repository.getOne("Test Medium").getLevel());
        insertWorkout("Test None", null, null, null);
        assertNull(repository.getOne("Test None").getLevel());
    }

    /** The app never writes workouts; seed.sql does, with the labels, so the test does too. */
    private void insertWorkout(String name, Difficulty level, WorkoutCategory category,
                               String description) {
        ContentValues cv = new ContentValues();
        cv.put("Name", name);
        cv.put("Level", level == null ? null : level.getLabel());
        cv.put("Category", category == null ? null : category.getLabel());
        cv.put("Description", description);
        assertTrue(db.insert("Workout", null, cv) != -1);
    }
}
