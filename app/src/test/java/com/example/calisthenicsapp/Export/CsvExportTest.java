package com.example.calisthenicsapp.Export;

import static org.junit.Assert.assertEquals;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.LoggedExercise;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CsvExportTest {
    private static final long T = 1792080000L;   // 2026-10-15T16:00:00Z

    private static List<String> lines(String csv) {
        return Arrays.asList(csv.split("\r\n"));
    }

    private static Map<String, String> sample() {
        User user = new User("u1", "alice", "a@b.c", 170, 0, null, T, T + 60);
        CustomWorkout workout = new CustomWorkout("cw1", "Legs, hard", "u1");
        List<CustomExerciseOrderEntry> entries = Arrays.asList(
                new CustomExerciseOrderEntry("cw1", "Squat", 1, 3, 12, null, 60),
                new CustomExerciseOrderEntry("cw1", "Plank", 2, 2, null, 30, 45));
        WorkoutLog log = new WorkoutLog("l1", "u1", "Legs, hard", T, 600, 42.456, "Completed",
                Collections.singletonList(new LoggedExercise("Squat", 5.0, false, 3, 3, 12, null, 90, 21.23)));
        ScheduledWorkout weekly = new ScheduledWorkout("s1", "u1", null, "cw1", T, "FREQ=WEEKLY",
                Arrays.asList(T + 7 * 86_400L, T), null);
        ScheduledWorkout once = new ScheduledWorkout("s2", "u1", "Beginner Full Body", null, T,
                null, null, null);
        return CsvExport.build(user, Collections.singletonList(workout),
                Collections.singletonMap("cw1", entries), Collections.singletonList(log),
                Arrays.asList(weekly, once));
    }

    @Test
    public void fiveFilesInAFixedOrder() {
        assertEquals(Arrays.asList(CsvExport.PROFILE, CsvExport.CUSTOM_WORKOUTS, CsvExport.WORKOUT_LOGS,
                CsvExport.WORKOUT_LOG_EXERCISES, CsvExport.SCHEDULES), new ArrayList<>(sample().keySet()));
    }

    @Test
    public void profileHasReadableTimesAndLeavesUnsetValuesEmpty() {
        assertEquals(Arrays.asList(
                "uid,username,email,height_cm,weight_kg,gender,created_at,updated_at",
                "u1,alice,a@b.c,170.0,,,2026-10-15T16:00:00Z,2026-10-15T16:01:00Z"),
                lines(sample().get(CsvExport.PROFILE)));
    }

    @Test
    public void customWorkoutsHaveOneRowPerExercise() {
        assertEquals(Arrays.asList(
                "workout_id,workout_name,position,exercise,sets,reps,seconds,rest_seconds",
                "cw1,\"Legs, hard\",1,Squat,3,12,,60",
                "cw1,\"Legs, hard\",2,Plank,2,,30,45"),
                lines(sample().get(CsvExport.CUSTOM_WORKOUTS)));
    }

    @Test
    public void logsAndTheirExercisesAreSeparateFilesLinkedByLogId() {
        assertEquals(Arrays.asList(
                "log_id,workout_name,started_at,duration_seconds,kcal,status",
                "l1,\"Legs, hard\",2026-10-15T16:00:00Z,600,42.5,Completed"),
                lines(sample().get(CsvExport.WORKOUT_LOGS)));
        assertEquals(Arrays.asList(
                "log_id,exercise,sets,completed_sets,reps,seconds,active_seconds,kcal,met",
                "l1,Squat,3,3,12,,90,21.2,5.0"),
                lines(sample().get(CsvExport.WORKOUT_LOG_EXERCISES)));
    }

    @Test
    public void schedulesNameTheirWorkoutAndListCompletedOccurrences() {
        assertEquals(Arrays.asList(
                "schedule_id,workout_name,custom_workout_id,first_occurrence,repeat_rule,completed_occurrences",
                "s1,\"Legs, hard\",cw1,2026-10-15T16:00:00Z,FREQ=WEEKLY,2026-10-15T16:00:00Z;2026-10-22T16:00:00Z",
                "s2,Beginner Full Body,,2026-10-15T16:00:00Z,,"),
                lines(sample().get(CsvExport.SCHEDULES)));
    }

    @Test
    public void withoutAProfileTheFileHasOnlyItsHeader() {
        Map<String, String> files = CsvExport.build(null, Collections.emptyList(),
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
        assertEquals(1, lines(files.get(CsvExport.PROFILE)).size());
        assertEquals(5, files.size());
    }
}
