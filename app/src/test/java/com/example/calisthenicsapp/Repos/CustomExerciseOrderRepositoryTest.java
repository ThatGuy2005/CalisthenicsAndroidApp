package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.Database.SQLManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class CustomExerciseOrderRepositoryTest {
    private CustomExerciseOrderRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        SQLiteDatabase db = SQLManager.getInstance(context).getWritableDatabase();
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')");
        db.execSQL("INSERT INTO CustomWorkout (Id, Name, Uid) VALUES ('cw1', 'Mine', 'u1')");
        repository = new CustomExerciseOrderRepository(db);
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    @Test
    public void bothRestsAreStoredAndReadBack() {
        assertTrue(repository.replaceForCustomWorkout("cw1", Arrays.asList(
                new CustomExerciseOrderEntry("cw1", "Push-Up", 1, 3, 10, null, 60, 90),
                new CustomExerciseOrderEntry("cw1", "Plank", 2, 2, null, 30, 45))));
        List<CustomExerciseOrderEntry> entries = repository.getByCustomWorkout("cw1");
        assertEquals(2, entries.size());
        assertEquals(Integer.valueOf(60), entries.get(0).getRestSeconds());
        assertEquals(Integer.valueOf(90), entries.get(0).getRestAfterSeconds());
        assertEquals(Integer.valueOf(45), entries.get(1).getRestSeconds());
        assertNull(entries.get(1).getRestAfterSeconds());     // the same as the rest between sets
    }
}
