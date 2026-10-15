package com.example.calisthenicsapp.Sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.calisthenicsapp.CoreComponents.LoggedExercise;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteWorkoutLogTest {
    /** Firestore hands numbers over as Long or Double. */
    private static Map<String, Object> lunges() {
        Map<String, Object> map = new HashMap<>();
        map.put("exerciseName", "Lunges");
        map.put("met", 3.8);
        map.put("timed", false);
        map.put("sets", 3L);
        map.put("completedSets", 2L);
        map.put("reps", 12L);
        map.put("seconds", null);
        map.put("activeSeconds", 80L);
        map.put("kcal", 6.5);
        return map;
    }

    @Test
    public void readsPerSideAndTheOtherFields() {
        Map<String, Object> map = lunges();
        map.put("perSide", true);
        LoggedExercise e = RemoteWorkoutLog.exercisesFrom(Arrays.asList(map)).get(0);
        assertEquals("Lunges", e.getExerciseName());
        assertEquals(2, e.getCompletedSets());
        assertEquals(Integer.valueOf(12), e.getReps());
        assertTrue(e.isPerSide());
    }

    /** Logs saved before the flag existed have no perSide at all. */
    @Test
    public void missingPerSideIsFalse() {
        List<LoggedExercise> list = RemoteWorkoutLog.exercisesFrom(Arrays.asList(lunges()));
        assertFalse(list.get(0).isPerSide());
    }
}
