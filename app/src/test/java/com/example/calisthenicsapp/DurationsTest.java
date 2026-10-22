package com.example.calisthenicsapp;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class DurationsTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Test
    public void estimatesRoundUpToWholeMinutes() {
        assertEquals("1 min", Durations.estimate(context, 1, R.string.workout_time_varies));
        assertEquals("25 min", Durations.estimate(context, 1500, R.string.workout_time_varies));
        assertEquals("26 min", Durations.estimate(context, 1501, R.string.workout_time_varies));
    }

    @Test
    public void withoutAnEstimateTheGivenTextIsShown() {
        assertEquals("time varies", Durations.estimate(context, null, R.string.workout_time_varies));
        assertEquals("Ready to train", Durations.estimate(context, 0, R.string.custom_workouts_no_length));
    }

    @Test
    public void timeSpentInMinutesAndHours() {
        assertEquals("0 min", Durations.spent(context, 0));
        assertEquals("<1 min", Durations.spent(context, 59));
        assertEquals("42 min", Durations.spent(context, 42 * 60 + 59));
        assertEquals("1 h", Durations.spent(context, 3600));
        assertEquals("1 h 5 min", Durations.spent(context, 3900));
    }
}
