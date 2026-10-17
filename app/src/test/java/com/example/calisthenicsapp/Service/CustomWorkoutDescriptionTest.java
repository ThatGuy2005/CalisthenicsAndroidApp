package com.example.calisthenicsapp.Service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** The description rules of CustomWorkoutService; Robolectric because the class needs Android. */
@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class CustomWorkoutDescriptionTest {
    @Test
    public void blankMeansNoDescription() {
        assertNull(CustomWorkoutService.cleanDescription(null));
        assertNull(CustomWorkoutService.cleanDescription("   "));
        assertEquals("Push day.", CustomWorkoutService.cleanDescription("  Push day. "));
    }

    @Test
    public void atMost200Characters() {
        assertNull(CustomWorkoutService.validateDescription(null));
        assertNull(CustomWorkoutService.validateDescription("x".repeat(200)));
        assertEquals("The description can be at most 200 characters.",
                CustomWorkoutService.validateDescription("x".repeat(201)));
    }
}
