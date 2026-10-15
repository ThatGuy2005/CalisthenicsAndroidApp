package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
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
    private WorkoutRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        repository = new WorkoutRepository(SQLManager.getInstance(context).getWritableDatabase());
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

    @Test
    public void categoryIsStoredAsTheLabelAndReadBack() {
        assertTrue(repository.insert(new Workout("Test Legs", Difficulty.BEGINNER,
                WorkoutCategory.LOWER_BODY)));
        assertEquals(WorkoutCategory.LOWER_BODY, repository.getOne("Test Legs").getCategory());

        assertTrue(repository.update(new Workout("Test Legs", Difficulty.BEGINNER,
                WorkoutCategory.CARDIO)));
        assertEquals(WorkoutCategory.CARDIO, repository.getOne("Test Legs").getCategory());

        assertTrue(repository.update(new Workout("Test Legs", Difficulty.BEGINNER)));
        assertNull(repository.getOne("Test Legs").getCategory());
    }

    @Test
    public void levelIsStoredAsTheLabelAndReadBack() {
        assertTrue(repository.insert(new Workout("Test Hard", Difficulty.ADVANCED)));
        assertEquals(Difficulty.ADVANCED, repository.getOne("Test Hard").getLevel());

        assertTrue(repository.update(new Workout("Test Hard", Difficulty.INTERMEDIATE)));
        assertEquals(Difficulty.INTERMEDIATE, repository.getOne("Test Hard").getLevel());

        assertTrue(repository.insert(new Workout("Test None", (Difficulty) null)));
        assertNull(repository.getOne("Test None").getLevel());
    }
}
