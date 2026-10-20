package com.example.calisthenicsapp.Service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;

import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.CoreComponents.StatsPeriod;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.CoreComponents.WorkoutStats;
import com.example.calisthenicsapp.Database.SQLManager;
import com.example.calisthenicsapp.Repos.ExerciseRepository;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Services built from dependencies handed in: this test's database, a made-up signed-in user
 * and no Firestore at all, so neither Firebase nor a device is needed.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class ServiceDependenciesTest {
    /** Reads come from SQLite only, so asking for Firestore is a bug. */
    private static final Supplier<FirebaseFirestore> NO_FIRESTORE = () -> {
        throw new AssertionError("A read asked for Firestore");
    };

    private Context context;
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        SQLManager.closeInstance();
        context.deleteDatabase(SQLManager.DATABASE_NAME);
        db = SQLManager.getInstance(context).getWritableDatabase();
    }

    @After
    public void tearDown() {
        SQLManager.closeInstance();
    }

    private static SessionManager signedInAs(String uid) {
        return new SessionManager(null) {
            @Override
            public String getCurrentUid() {
                return uid;
            }
        };
    }

    @Test
    public void libraryServiceReadsTheGivenDatabase() throws InterruptedException {
        Result<List<Exercise>> result = await(new ExerciseService(context, () -> db)::getAllExercises);

        assertNull(result.error);
        assertFalse(result.value.isEmpty());
        assertEquals(new ExerciseRepository(db).getAll().size(), result.value.size());
    }

    @Test
    public void userServiceReadsOnlyTheGivenUsersData() throws InterruptedException {
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u1', 'a@b.c', 'alice')");
        db.execSQL("INSERT INTO Users (Uid, Email, Username) VALUES ('u2', 'b@b.c', 'bob')");
        WorkoutLogRepository logs = new WorkoutLogRepository(db);
        assertTrue(logs.insert(new WorkoutLog("a", "u1", "Legs", 1000, 600, 50.0, "Completed",
                Collections.emptyList())));
        assertTrue(logs.insert(new WorkoutLog("b", "u1", "Legs", 2000, 300, 20.0, "Partial",
                Collections.emptyList())));
        assertTrue(logs.insert(new WorkoutLog("c", "u2", "Legs", 2000, 900, 70.0, "Completed",
                Collections.emptyList())));
        StatsService stats = new StatsService(context, () -> db, signedInAs("u1"), NO_FIRESTORE);

        Result<WorkoutStats> result = await(callback -> stats.getStats(StatsPeriod.ALL_TIME, callback));

        assertNull(result.error);
        assertEquals(2, result.value.getWorkouts());
        assertEquals(900, result.value.getTotalSeconds());
    }

    @Test
    public void signedOutUserGetsAnError() throws InterruptedException {
        StatsService stats = new StatsService(context, () -> db, signedInAs(null), NO_FIRESTORE);

        Result<WorkoutStats> result = await(callback -> stats.getStats(StatsPeriod.ALL_TIME, callback));

        assertEquals(UserDataService.NOT_SIGNED_IN, result.error);
    }

    private static final class Result<T> implements ServiceCallback<T> {
        volatile boolean done;
        T value;
        String error;

        @Override
        public void onSuccess(T result) {
            value = result;
            done = true;
        }

        @Override
        public void onError(String message) {
            error = message;
            done = true;
        }
    }

    /** The service reads on its own thread and then posts the callback to the main looper. */
    private static <T> Result<T> await(Consumer<ServiceCallback<T>> call) throws InterruptedException {
        Result<T> result = new Result<>();
        call.accept(result);
        for (int i = 0; i < 500 && !result.done; i++) {
            shadowOf(Looper.getMainLooper()).idle();
            if (!result.done) Thread.sleep(10);
        }
        assertTrue("The service never called back", result.done);
        return result;
    }
}
