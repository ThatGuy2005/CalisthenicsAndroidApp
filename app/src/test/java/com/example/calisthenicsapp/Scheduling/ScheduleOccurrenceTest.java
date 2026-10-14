package com.example.calisthenicsapp.Scheduling;

import static org.junit.Assert.assertEquals;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class ScheduleOccurrenceTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long START = 1792080000L;   // Thu 2026-10-15 16:00:00 UTC
    private static final long DAY = 86_400L;
    private static final long HOUR = 3_600L;

    @Test
    public void statusFollowsCompletionAndTime() {
        assertEquals(OccurrenceStatus.COMPLETED, OccurrenceStatus.of(true, START, START + 30 * DAY));
        assertEquals(OccurrenceStatus.PENDING, OccurrenceStatus.of(false, START, START - DAY));
        assertEquals(OccurrenceStatus.PENDING, OccurrenceStatus.of(false, START, START + 12 * HOUR - 1));
        assertEquals(OccurrenceStatus.MISSED, OccurrenceStatus.of(false, START, START + 12 * HOUR));
    }

    @Test
    public void listsEveryOccurrenceInTheWindowWithItsStatus() {
        ScheduledWorkout weekly = new ScheduledWorkout("w", "u", "Legs", null, START,
                "FREQ=WEEKLY", Collections.singletonList(START), null);
        ScheduledWorkout once = new ScheduledWorkout("o", "u", "Arms", null, START + 2 * DAY,
                null, null, null);
        long now = START + 9 * DAY;   // Saturday of the second week

        List<ScheduleOccurrence> occurrences = ScheduleOccurrence.between(
                Arrays.asList(weekly, once), START, START + 15 * DAY, now, UTC);

        assertEquals(4, occurrences.size());
        assertOccurrence(occurrences.get(0), "w", START, OccurrenceStatus.COMPLETED);
        assertOccurrence(occurrences.get(1), "o", START + 2 * DAY, OccurrenceStatus.MISSED);
        assertOccurrence(occurrences.get(2), "w", START + 7 * DAY, OccurrenceStatus.MISSED);
        assertOccurrence(occurrences.get(3), "w", START + 14 * DAY, OccurrenceStatus.PENDING);
    }

    @Test
    public void leavesOutOccurrencesOutsideTheWindow() {
        ScheduledWorkout once = new ScheduledWorkout("o", "u", "Arms", null, START, null, null, null);
        assertEquals(0, ScheduleOccurrence.between(Collections.singletonList(once),
                START + 1, START + DAY, START, UTC).size());
    }

    private static void assertOccurrence(ScheduleOccurrence o, String scheduleId, long start,
                                         OccurrenceStatus status) {
        assertEquals(scheduleId, o.getSchedule().getId());
        assertEquals(start, o.getStartSeconds());
        assertEquals(status, o.getStatus());
    }
}
