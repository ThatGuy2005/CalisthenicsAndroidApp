package com.example.calisthenicsapp.Export;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.LoggedExercise;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The user's data as CSV files (NFR-5), one per kind of record, since CSV cannot nest:
 * profile, custom workout exercises, workout logs, the exercises of each log, and schedules.
 * Times are ISO 8601 in UTC ("2026-10-15T16:00:00Z"), which spreadsheets and people can read.
 * Pure Java, so it is unit-tested on the JVM.
 */
public final class CsvExport {
    public static final String PROFILE = "profile.csv";
    public static final String CUSTOM_WORKOUTS = "custom_workouts.csv";
    public static final String WORKOUT_LOGS = "workout_logs.csv";
    public static final String WORKOUT_LOG_EXERCISES = "workout_log_exercises.csv";
    public static final String SCHEDULES = "schedules.csv";

    private CsvExport() {
    }

    /**
     * File name to CSV text, in a fixed order.
     *
     * @param user                null if the profile is not synced yet (the file then has only its header)
     * @param exercisesByWorkout  custom workout id to its exercises, in order
     */
    public static Map<String, String> build(User user, List<CustomWorkout> customWorkouts,
                                            Map<String, List<CustomExerciseOrderEntry>> exercisesByWorkout,
                                            List<WorkoutLog> logs, List<ScheduledWorkout> schedules) {
        Map<String, String> files = new LinkedHashMap<>();

        CsvWriter profile = new CsvWriter("uid", "username", "email", "height_cm", "weight_kg",
                "gender", "created_at", "updated_at");
        if (user != null) {
            profile.row(user.getUid(), user.getUsername(), user.getEmail(), positive(user.getHeight()),
                    positive(user.getWeight()), user.getGender(), iso(user.getCreatedAt()),
                    iso(user.getUpdatedAt()));
        }
        files.put(PROFILE, profile.toString());

        CsvWriter workouts = new CsvWriter("workout_id", "workout_name", "position", "exercise",
                "sets", "reps", "seconds", "rest_seconds", "rest_after_seconds");
        Map<String, String> workoutNames = new HashMap<>();
        for (CustomWorkout w : customWorkouts) {
            workoutNames.put(w.getId(), w.getName());
            List<CustomExerciseOrderEntry> entries = exercisesByWorkout.get(w.getId());
            for (CustomExerciseOrderEntry e : entries == null
                    ? Collections.<CustomExerciseOrderEntry>emptyList() : entries) {
                workouts.row(w.getId(), w.getName(), e.getPosition(), e.getExerciseName(),
                        e.getSets(), e.getReps(), e.getSeconds(), e.getRestSeconds(),
                        e.getRestAfterSeconds());
            }
        }
        files.put(CUSTOM_WORKOUTS, workouts.toString());

        CsvWriter logRows = new CsvWriter("log_id", "workout_name", "started_at", "duration_seconds",
                "kcal", "status");
        CsvWriter logExercises = new CsvWriter("log_id", "exercise", "sets", "completed_sets", "reps",
                "seconds", "active_seconds", "kcal", "met");
        for (WorkoutLog log : logs) {
            logRows.row(log.getId(), log.getWorkoutName(), iso(log.getStartedAt()),
                    log.getDurationSeconds(), oneDecimal(log.getKcal()), log.getStatus());
            for (LoggedExercise e : log.getExercises()) {
                logExercises.row(log.getId(), e.getExerciseName(), e.getSets(), e.getCompletedSets(),
                        e.getReps(), e.getSeconds(), e.getActiveSeconds(), oneDecimal(e.getKcal()),
                        e.getMet());
            }
        }
        files.put(WORKOUT_LOGS, logRows.toString());
        files.put(WORKOUT_LOG_EXERCISES, logExercises.toString());

        CsvWriter scheduleRows = new CsvWriter("schedule_id", "workout_name", "custom_workout_id",
                "first_occurrence", "repeat_rule", "completed_occurrences");
        for (ScheduledWorkout s : schedules) {
            StringJoiner completed = new StringJoiner(";");
            for (long at : s.getCompletedOccurrences()) completed.add(iso(at));
            String name = s.isCustom() ? workoutNames.get(s.getCustomWorkoutId()) : s.getWorkoutName();
            scheduleRows.row(s.getId(), name, s.getCustomWorkoutId(), iso(s.getScheduledAt()),
                    s.getRecurrenceRule(), completed.toString());
        }
        files.put(SCHEDULES, scheduleRows.toString());
        return files;
    }

    /**
     * Writes the files into one ZIP. Each starts with a UTF-8 byte order mark, without which
     * Excel reads UTF-8 CSV in the old ANSI code page and garbles accented letters.
     * Does not close out.
     */
    public static void writeZip(Map<String, String> files, OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        for (Map.Entry<String, String> file : files.entrySet()) {
            zip.putNextEntry(new ZipEntry(file.getKey()));
            zip.write(("\uFEFF" + file.getValue()).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        zip.finish();
    }

    static String iso(long epochSeconds) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(epochSeconds * 1000L));
    }

    /** The local cache stores a missing height or weight as 0. */
    private static Double positive(Double value) {
        return value == null || value <= 0 ? null : value;
    }

    /** A number, not text, so the CSV writer treats it as one. */
    private static double oneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
