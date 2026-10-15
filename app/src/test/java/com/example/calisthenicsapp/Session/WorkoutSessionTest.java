package com.example.calisthenicsapp.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.calisthenicsapp.Session.WorkoutSession.Phase;
import com.example.calisthenicsapp.Session.WorkoutSession.SessionExercise;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class WorkoutSessionTest {
    private static final double WEIGHT = 80;

    private long now;               // fake clock, milliseconds
    private WorkoutSession session;

    /** Push-ups: 2 sets of 10 reps, 30 s rest, MET 3.8. Plank: 1 set of 20 s, MET 3.0. */
    @Before
    public void setUp() {
        now = 1_000_000L;
        session = new WorkoutSession("Test Workout", Arrays.asList(
                new SessionExercise("Push-Up", null, null, false, 10, null, 2, 30, 3.8),
                new SessionExercise("Plank", null, null, true, null, 20, 1, 0, 3.0)),
                WEIGHT, () -> now, 1_700_000_000L);
    }

    private void advance(long seconds) {
        now += seconds * 1000L;
    }

    @Test
    public void startsOnFirstSetOfFirstExercise() {
        assertEquals(Phase.WORK, session.getPhase());
        assertEquals("Push-Up", session.getCurrentExercise().getName());
        assertEquals(1, session.getCurrentSet());
        assertEquals("Plank", session.getNextExercise().getName());
        assertNull(session.getRemainingSeconds());          // rep-based set has no countdown
    }

    @Test
    public void fullRunIsCompletedWithActiveTimeAndKcal() {
        advance(40);
        assertTrue(session.completeSet());                  // push-up set 1: 40 s active
        assertEquals(Phase.REST, session.getPhase());
        assertEquals(2, session.getCurrentSet());           // rest shows what comes next
        assertEquals(Integer.valueOf(30), session.getRemainingSeconds());

        advance(30);
        assertTrue(session.tick());                         // rest over
        assertEquals(Phase.WORK, session.getPhase());

        advance(50);
        session.completeSet();                              // push-up set 2: 50 s active
        // Rest after the last push-up set, then the plank.
        assertEquals(Phase.REST, session.getPhase());
        assertEquals("Plank", session.getCurrentExercise().getName());
        assertTrue(session.skipRest());

        advance(19);
        assertFalse(session.tick());
        assertEquals(Integer.valueOf(1), session.getRemainingSeconds());
        advance(5);
        assertTrue(session.tick());                         // timed set ends by itself
        assertEquals(Phase.FINISHED, session.getPhase());

        assertEquals(WorkoutSession.STATUS_COMPLETED, session.getStatus());
        assertTrue(session.isLoggable());
        assertEquals(40 + 30 + 50 + 24, session.getTotalSeconds());
        SessionExercise pushUp = session.getExercises().get(0);
        SessionExercise plank = session.getExercises().get(1);
        assertEquals(90, pushUp.getActiveSeconds());
        assertEquals(20, plank.getActiveSeconds());         // capped at the 20 s target
        double expected = 3.8 * WEIGHT * 90 / 3600.0 + 3.0 * WEIGHT * 20 / 3600.0;
        assertEquals(expected, session.getTotalKcal(), 1e-9);
    }

    @Test
    public void restBeforeTheNextExerciseFollowsTheLastSet() {
        session = new WorkoutSession("Rests", Arrays.asList(
                new SessionExercise("Push-Up", null, null, false, 10, null, 2, 30, 90, 3.8),
                new SessionExercise("Squat", null, null, false, 10, null, 1, 30, 0, 3.8),
                new SessionExercise("Plank", null, null, true, null, 20, 1, 0, 3.0)),
                WEIGHT, () -> now, 1_700_000_000L);
        session.completeSet();                              // push-up set 1
        assertEquals(Integer.valueOf(30), session.getRemainingSeconds());   // between sets
        session.skipRest();
        session.completeSet();                              // push-up set 2, the last
        assertEquals(Phase.REST, session.getPhase());
        assertEquals("Squat", session.getCurrentExercise().getName());
        assertEquals(Integer.valueOf(90), session.getRemainingSeconds());   // before the squat
        session.skipRest();
        session.completeSet();                              // squat: 0 s before the plank
        assertEquals(Phase.WORK, session.getPhase());
        assertEquals("Plank", session.getCurrentExercise().getName());
    }

    @Test
    public void aTimedPerSideSetRunsOncePerSide() {
        session = new WorkoutSession("Sides", Arrays.asList(
                new SessionExercise("Side Plank", null, null, true, null, 20, 1, 0, 0, true, 3.0)),
                WEIGHT, () -> now, 1_700_000_000L);
        assertEquals(Integer.valueOf(40), session.getRemainingSeconds());   // 20 s each side
        advance(30);
        assertFalse(session.tick());
        advance(10);
        assertTrue(session.tick());
        assertEquals(Phase.FINISHED, session.getPhase());
        assertEquals(40, session.getExercises().get(0).getActiveSeconds());
    }

    @Test
    public void kcalUseTheBmrRateWhenGiven() {
        double rate = 1780 / 24.0;                          // BMR 1780 kcal/day
        session = new WorkoutSession("Rate", Arrays.asList(
                new SessionExercise("Push-Up", null, null, false, 10, null, 1, 0, 3.8)),
                WEIGHT, rate, () -> now, 1_700_000_000L);
        assertEquals(rate, session.getKcalPerMetHour(), 1e-9);
        advance(60);
        session.completeSet();
        assertEquals(3.8 * rate * 60 / 3600.0, session.getTotalKcal(), 1e-9);
    }

    @Test
    public void withoutARateTheWeightIsUsed() {
        assertEquals(WEIGHT, session.getKcalPerMetHour(), 1e-9);
    }

    @Test
    public void pausedTimeIsNotCounted() {
        advance(10);
        session.pause();
        advance(300);
        assertTrue(session.isPaused());
        assertEquals(10, session.getStepElapsedSeconds());
        assertFalse(session.tick());
        session.resume();
        advance(5);
        session.completeSet();
        assertEquals(15, session.getExercises().get(0).getActiveSeconds());
        assertEquals(15, session.getTotalSeconds());
    }

    @Test
    public void restDoesNotEndWhilePaused() {
        session.completeSet();
        session.pause();
        advance(120);
        assertFalse(session.tick());
        assertEquals(Phase.REST, session.getPhase());
        assertEquals(Integer.valueOf(30), session.getRemainingSeconds());
    }

    @Test
    public void skippingAnExerciseMakesItPartial() {
        advance(30);
        session.completeSet();
        assertTrue(session.skipExercise());                 // skips push-up set 2 (shown in the rest)
        assertEquals(Phase.WORK, session.getPhase());
        assertEquals("Plank", session.getCurrentExercise().getName());
        session.completeSet();
        assertEquals(Phase.FINISHED, session.getPhase());
        assertEquals(WorkoutSession.STATUS_PARTIAL, session.getStatus());
        assertTrue(session.isLoggable());
    }

    @Test
    public void cancelWithoutCompletedSetsIsNotLoggable() {
        advance(30);
        session.cancel();
        assertEquals(Phase.CANCELLED, session.getPhase());
        assertFalse(session.isLoggable());
        assertNull(session.getCurrentExercise());
        assertEquals(0, session.getExercises().get(0).getActiveSeconds());  // unfinished set not counted
    }

    @Test
    public void cancelAfterASetIsPartial() {
        advance(30);
        session.completeSet();
        session.cancel();
        assertTrue(session.isLoggable());
        assertEquals(WorkoutSession.STATUS_PARTIAL, session.getStatus());
    }

    @Test
    public void actionsDoNothingOnceOver() {
        session.cancel();
        assertFalse(session.completeSet());
        assertFalse(session.skipRest());
        assertFalse(session.skipExercise());
        assertFalse(session.tick());
        assertEquals(Phase.CANCELLED, session.getPhase());
    }

    @Test
    public void survivesSerialization() throws Exception {
        advance(25);
        session.completeSet();
        advance(10);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(session);
        }
        WorkoutSession restored;
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (WorkoutSession) in.readObject();
        }
        restored.setClock(() -> now);

        assertEquals(Phase.REST, restored.getPhase());
        assertEquals(2, restored.getCurrentSet());
        assertEquals(Integer.valueOf(20), restored.getRemainingSeconds());
        assertEquals(1, restored.getCompletedSets());
        assertEquals(25, restored.getExercises().get(0).getActiveSeconds());
    }
}
