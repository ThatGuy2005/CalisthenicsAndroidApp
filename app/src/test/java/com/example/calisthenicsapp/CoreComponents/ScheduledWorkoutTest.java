package com.example.calisthenicsapp.CoreComponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.TimeZone;

public class ScheduledWorkoutTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long START = 1792080000L;   // 2026-10-15 16:00:00 UTC
    private static final long DAY = 86_400L;

    private static ScheduledWorkout schedule(String rule) {
        return new ScheduledWorkout("s1", "u1", "Legs", null, START, rule, null);
    }

    @Test
    public void oneOffHasOnlyItsOwnTime() {
        ScheduledWorkout once = schedule(null);
        assertFalse(once.isRecurring());
        assertEquals(Long.valueOf(START), once.nextOccurrence(START - DAY, UTC));
        assertNull(once.nextOccurrence(START + 1, UTC));
    }

    @Test
    public void weeklyHasANextOccurrenceAfterTheFirst() {
        ScheduledWorkout weekly = schedule("FREQ=WEEKLY");
        assertTrue(weekly.isRecurring());
        assertEquals(Long.valueOf(START + 7 * DAY), weekly.nextOccurrence(START + 1, UTC));
    }

    @Test
    public void unreadableRuleCountsAsOnce() {
        ScheduledWorkout odd = schedule("FREQ=YEARLY;BYMONTH=1");
        assertFalse(odd.isRecurring());
        assertNull(odd.nextOccurrence(START + 1, UTC));
    }
}
