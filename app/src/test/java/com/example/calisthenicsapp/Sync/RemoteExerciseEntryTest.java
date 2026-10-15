package com.example.calisthenicsapp.Sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RemoteExerciseEntryTest {
    /** Firestore hands numbers over as Long. */
    private static Map<String, Object> entry() {
        Map<String, Object> map = new HashMap<>();
        map.put("exerciseName", "Push-up");
        map.put("position", 2L);
        map.put("sets", 3L);
        map.put("reps", null);
        map.put("seconds", null);
        map.put("restSeconds", 60L);
        return map;
    }

    @Test
    public void readsBothRests() {
        Map<String, Object> map = entry();
        map.put("restAfterSeconds", 90L);
        RemoteExerciseEntry e = RemoteExerciseEntry.fromMap(map);
        assertEquals("Push-up", e.exerciseName);
        assertEquals(2, e.position);
        assertEquals(Integer.valueOf(3), e.sets);
        assertNull(e.reps);
        assertEquals(Integer.valueOf(60), e.restSeconds);
        assertEquals(Integer.valueOf(90), e.restAfterSeconds);
    }

    /** Workouts saved before the field existed have no restAfterSeconds at all. */
    @Test
    public void missingRestAfterIsNull() {
        assertNull(RemoteExerciseEntry.fromMap(entry()).restAfterSeconds);
    }
}
