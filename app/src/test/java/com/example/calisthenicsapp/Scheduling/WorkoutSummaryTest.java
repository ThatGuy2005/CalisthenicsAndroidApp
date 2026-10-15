package com.example.calisthenicsapp.Scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.calisthenicsapp.Session.WorkoutSession.SessionExercise;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

public class WorkoutSummaryTest {
    private static final long START = 1792080000L;   // 2026-10-15 16:00:00 UTC

    private static SessionExercise reps(String name, int sets, int reps, int rest) {
        return new SessionExercise(name, null, null, false, reps, null, sets, rest, 3.8);
    }

    private static SessionExercise timed(String name, int sets, int seconds, int rest) {
        return new SessionExercise(name, null, null, true, null, seconds, sets, rest, 3.8);
    }

    @Test
    public void estimateCountsSetsAndRestsButNotTheLastRest() {
        WorkoutSummary summary = new WorkoutSummary("s1", "Mixed", START, Arrays.asList(
                reps("Push-up", 3, 10, 60),       // 3 x (10 x 3 s + 60 s) = 270
                timed("Plank", 2, 30, 30)));      // 2 x (30 s + 30 s) = 120, minus the last 30
        assertEquals(360, summary.getEstimatedSeconds());
    }

    @Test
    public void shortWorkoutsLastAtLeast15Minutes() {
        WorkoutSummary summary = new WorkoutSummary("s1", "Short", START,
                Collections.singletonList(reps("Squat", 1, 10, 0)));
        assertEquals(START + WorkoutSummary.MIN_EVENT_SECONDS, summary.getEndSeconds());
    }

    @Test
    public void endIsRoundedUpToFiveMinutes() {
        WorkoutSummary summary = new WorkoutSummary("s1", "Long", START, Arrays.asList(
                timed("Plank", 10, 60, 60),       // 10 x 120 = 1200
                reps("Squat", 2, 20, 10)));       // 2 x 70 = 140, minus 10 -> total 1330
        assertEquals(1330, summary.getEstimatedSeconds());
        assertEquals(START + 1500, summary.getEndSeconds());   // 22:10 -> 25:00
    }

    @Test
    public void exerciseListHasOneNumberedLinePerExercise() {
        WorkoutSummary summary = new WorkoutSummary("s1", "Mixed", START, Arrays.asList(
                reps("Push-up", 3, 10, 60), timed("Plank", 2, 30, 0)));
        assertEquals("1. Push-up: 3 x 10 reps, 60 s rest\n2. Plank: 2 x 30 s",
                summary.getExerciseList());
    }

    @Test
    public void aDifferentRestBeforeTheNextExerciseIsListed() {
        WorkoutSummary summary = new WorkoutSummary("s1", "Mixed", START, Arrays.asList(
                new SessionExercise("Push-up", null, null, false, 10, null, 3, 60, 90, 3.8),
                new SessionExercise("Plank", null, null, true, null, 30, 2, 30, 90, 3.8)));
        assertEquals("1. Push-up: 3 x 10 reps, 60 s rest, 90 s before the next exercise\n"
                + "2. Plank: 2 x 30 s, 30 s rest", summary.getExerciseList());
    }

    @Test
    public void emailHasNameTimeAndExercises() {
        WorkoutSummary summary = new WorkoutSummary("s1", "Legs", START,
                Collections.singletonList(reps("Squat", 3, 15, 45)));
        TimeZone utc = TimeZone.getTimeZone("UTC");
        String subject = summary.getEmailSubject(utc, Locale.US);
        String body = summary.getEmailBody(utc, Locale.US);
        assertTrue(subject.contains("Legs"));
        assertTrue(subject.contains("2026"));
        assertTrue(body.contains("Workout: Legs"));
        assertTrue(body.contains("October 15, 2026"));
        assertTrue(body.contains("1. Squat: 3 x 15 reps, 45 s rest"));
    }

    @Test
    public void repeatingWorkoutSaysHowItRepeats() {
        WorkoutSummary summary = new WorkoutSummary("abc123", "Legs", START, Recurrence.weekly(),
                Collections.singletonList(reps("Squat", 3, 15, 45)));
        assertTrue(summary.getDescription().contains("Repeats: every week"));
        assertTrue(summary.getEmailBody(TimeZone.getTimeZone("UTC"), Locale.US)
                .contains("Repeats: every week"));
        assertTrue(summary.toIcs(START).contains("\r\nRRULE:FREQ=WEEKLY\r\n"));
    }

    @Test
    public void oneOffWorkoutDoesNotRepeat() {
        WorkoutSummary summary = new WorkoutSummary("abc123", "Legs", START,
                Collections.singletonList(reps("Squat", 3, 15, 45)));
        assertFalse(summary.getDescription().contains("Repeats"));
        assertFalse(summary.toIcs(START).contains("RRULE"));
    }

    @Test
    public void icsUsesTheScheduleIdAsUid() {
        WorkoutSummary summary = new WorkoutSummary("abc123", "Legs", START,
                Collections.singletonList(reps("Squat", 3, 15, 45)));
        String ics = summary.toIcs(START);
        assertTrue(ics.contains("\r\nUID:abc123@calisthenicsapp\r\n"));
        assertTrue(ics.contains("\r\nSUMMARY:Workout: Legs\r\n"));
    }
}
