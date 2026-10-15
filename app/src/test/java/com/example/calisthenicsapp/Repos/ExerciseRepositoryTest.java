package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Exercise;
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
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class ExerciseRepositoryTest {
    private ExerciseRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        repository = new ExerciseRepository(SQLManager.getInstance(context).getWritableDatabase());
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    @Test
    public void everyPremadeExerciseHasADifficulty() {
        List<Exercise> all = repository.getAll();
        assertFalse(all.isEmpty());
        for (Exercise e : all) assertNotNull(e.getName(), e.getDifficulty());
    }

    @Test
    public void searchByDifficultyReturnsOnlyThatDifficulty() {
        int total = 0;
        for (Difficulty difficulty : Difficulty.values()) {
            List<Exercise> found = repository.search(null, null, difficulty);
            for (Exercise e : found) assertEquals(difficulty, e.getDifficulty());
            total += found.size();
        }
        assertEquals(repository.getAll().size(), total);   // the three filters cover the library
    }

    @Test
    public void perSideExercisesComeFromTheSeed() {
        List<String> perSide = new ArrayList<>();
        for (Exercise e : repository.getAll()) if (e.isPerSide()) perSide.add(e.getName());
        assertEquals(Arrays.asList("Archer Push-Up", "Lunges", "Pistol Squat"), perSide);
    }

    @Test
    public void perSideIsStoredAndReadBack() {
        assertTrue(repository.insert(new Exercise("Side Plank", "Hold.", true, 20, null, 3.0,
                null, "Core", Difficulty.INTERMEDIATE, true)));
        assertTrue(repository.getOne("Side Plank").isPerSide());
        assertTrue(repository.insert(new Exercise("Hollow Hold", "Hold.", true, 20, null, 3.0,
                null, "Core", Difficulty.INTERMEDIATE)));
        assertFalse(repository.getOne("Hollow Hold").isPerSide());
    }
}
