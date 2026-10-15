package com.example.calisthenicsapp.Repos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.User;
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
public class UserRepositoryTest {
    private UserRepository repository;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        repository = new UserRepository(SQLManager.getInstance(context).getWritableDatabase());
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    @Test
    public void birthYearIsStoredAndReadBack() {
        assertTrue(repository.insert(new User("u1", "alice", "a@b.c", 170, 60, "Female", 1995,
                1000L, 1000L)));
        assertEquals(Integer.valueOf(1995), repository.getOne("u1").getBirthYear());

        assertTrue(repository.update(new User("u1", "alice", "a@b.c", 170, 60, "Female", null,
                1000L, 2000L)));
        assertNull(repository.getOne("u1").getBirthYear());
    }
}
