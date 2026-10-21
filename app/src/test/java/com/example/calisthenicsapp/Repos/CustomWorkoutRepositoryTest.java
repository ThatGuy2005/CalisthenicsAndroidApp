package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.Database.SQLManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class CustomWorkoutRepositoryTest {
    private CustomWorkoutRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        SQLiteDatabase db = SQLManager.getInstance(context).getWritableDatabase();
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')");
        repository = new CustomWorkoutRepository(db);
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    @Test
    public void descriptionIsStoredAndReadBack() {
        assertTrue(repository.insert(new CustomWorkout("cw1", "Mine", "u1", "Push day.", null)));
        assertEquals("Push day.", repository.getOne("cw1").getDescription());

        assertTrue(repository.update(new CustomWorkout("cw1", "Mine", "u1", "Pull day.", null)));
        assertEquals("Pull day.", repository.getOne("cw1").getDescription());

        assertTrue(repository.update(new CustomWorkout("cw1", "Renamed", "u1")));
        assertEquals("Renamed", repository.getOne("cw1").getName());
        assertNull(repository.getOne("cw1").getDescription());
    }
}
