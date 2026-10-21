package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Equipment;
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
    private SQLiteDatabase db;
    private ExerciseRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        db = SQLManager.getInstance(context).getWritableDatabase();
        repository = new ExerciseRepository(db);
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
            List<Exercise> found = repository.search(null, null, difficulty, null);
            for (Exercise e : found) assertEquals(difficulty, e.getDifficulty());
            total += found.size();
        }
        assertEquals(repository.getAll().size(), total);   // the three filters cover the library
    }

    @Test
    public void searchByEquipmentReturnsOnlyThatEquipment() {
        int total = 0;
        for (Equipment equipment : Equipment.values()) {
            List<Exercise> found = repository.search(null, null, null, equipment);
            for (Exercise e : found) assertEquals(equipment, e.getEquipment());
            total += found.size();
        }
        assertEquals(repository.getAll().size(), total);   // every exercise has one value
        assertEquals(Arrays.asList("Pull-Up"),
                names(repository.search(null, null, null, Equipment.PULL_UP_BAR)));
        assertEquals(Arrays.asList("Burpees"),                              // filters combine
                names(repository.search("bur", "Full Body", Difficulty.INTERMEDIATE, Equipment.NONE)));
    }

    private static List<String> names(List<Exercise> exercises) {
        List<String> names = new ArrayList<>();
        for (Exercise e : exercises) names.add(e.getName());
        return names;
    }

    @Test
    public void perSideExercisesComeFromTheSeed() {
        List<String> perSide = new ArrayList<>();
        for (Exercise e : repository.getAll()) if (e.isPerSide()) perSide.add(e.getName());
        assertEquals(Arrays.asList("Archer Push-Up", "Lunges", "Pistol Squat"), perSide);
    }

    @Test
    public void equipmentAndMuscleGroupsComeFromTheSeed() {
        assertEquals(Equipment.PULL_UP_BAR, repository.getOne("Pull-Up").getEquipment());
        assertEquals(Equipment.PARALLEL_BARS, repository.getOne("Dips").getEquipment());
        assertEquals(Equipment.BENCH, repository.getOne("Bench Dips").getEquipment());
        assertEquals(Equipment.WALL, repository.getOne("Wall Sit").getEquipment());
        assertEquals(Equipment.NONE, repository.getOne("Push-Up").getEquipment());
        assertTrue(repository.getOne("Burpees").isMultiMuscle());
        assertFalse(repository.getOne("Crunches").isMultiMuscle());
    }

    @Test
    public void equipmentAndMultiMuscleAreReadBack() {
        insertExercise("Ring Row", Equipment.PULL_UP_BAR, false, true);
        Exercise row = repository.getOne("Ring Row");
        assertEquals(Equipment.PULL_UP_BAR, row.getEquipment());
        assertTrue(row.isMultiMuscle());
        insertExercise("Calf Raise", null, false, false);
        Exercise raise = repository.getOne("Calf Raise");
        assertEquals(Equipment.NONE, raise.getEquipment());   // the column's default
        assertFalse(raise.isMultiMuscle());
    }

    @Test
    public void perSideIsReadBack() {
        insertExercise("Side Plank", null, true, false);
        assertTrue(repository.getOne("Side Plank").isPerSide());
        insertExercise("Hollow Hold", null, false, false);
        assertFalse(repository.getOne("Hollow Hold").isPerSide());
    }

    /** The app never writes exercises; seed.sql does, so the test adds rows the same way. */
    private void insertExercise(String name, Equipment equipment, boolean perSide,
                                boolean multiMuscle) {
        ContentValues cv = new ContentValues();
        cv.put("Name", name);
        cv.put("Instructions", "Test.");
        cv.put("DefaultReps", 10);
        cv.put("MuscleGroup", "Core");
        cv.put("Difficulty", Difficulty.INTERMEDIATE.getLabel());
        cv.put("IsPerSide", perSide ? 1 : 0);
        if (equipment != null) cv.put("Equipment", equipment.getLabel());
        cv.put("IsMultiMuscle", multiMuscle ? 1 : 0);
        assertTrue(db.insert("Exercise", null, cv) != -1);
    }
}
