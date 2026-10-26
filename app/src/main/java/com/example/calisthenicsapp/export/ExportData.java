package com.example.calisthenicsapp.export;

import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.model.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.model.CustomWorkout;
import com.example.calisthenicsapp.model.ScheduledWorkout;
import com.example.calisthenicsapp.model.User;
import com.example.calisthenicsapp.model.WorkoutLog;
import com.example.calisthenicsapp.repository.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.repository.CustomWorkoutRepository;
import com.example.calisthenicsapp.repository.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.repository.UserRepository;
import com.example.calisthenicsapp.repository.WorkoutLogRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything of one user that the exports hold (NFR-5): the profile, the custom workouts with
 * their exercises, the workout logs and the schedules. Read in one place, so the JSON
 * ({@link JsonExport}) and the CSV ({@link CsvExport}) export cover the same records.
 */
public final class ExportData {
    private final String uid;
    private final User user;
    private final List<CustomWorkout> customWorkouts;
    private final Map<String, List<CustomExerciseOrderEntry>> exercisesByWorkout;
    private final List<WorkoutLog> logs;
    private final List<ScheduledWorkout> schedules;

    /**
     * @param user                null if the profile is not synced yet
     * @param exercisesByWorkout  custom workout id to its exercises, in order
     */
    public ExportData(String uid, User user, List<CustomWorkout> customWorkouts,
                      Map<String, List<CustomExerciseOrderEntry>> exercisesByWorkout,
                      List<WorkoutLog> logs, List<ScheduledWorkout> schedules) {
        this.uid = uid;
        this.user = user;
        this.customWorkouts = customWorkouts;
        this.exercisesByWorkout = exercisesByWorkout;
        this.logs = logs;
        this.schedules = schedules;
    }

    /** The user's rows in SQLite. Call it on a background thread. */
    public static ExportData read(SQLiteDatabase db, String uid) {
        CustomExerciseOrderRepository orderRepository = new CustomExerciseOrderRepository(db);
        List<CustomWorkout> workouts = new CustomWorkoutRepository(db).getByUid(uid);
        Map<String, List<CustomExerciseOrderEntry>> exercises = new HashMap<>();
        for (CustomWorkout w : workouts) {
            exercises.put(w.getId(), orderRepository.getByCustomWorkout(w.getId()));
        }
        return new ExportData(uid, new UserRepository(db).getOne(uid), workouts, exercises,
                new WorkoutLogRepository(db).getByUid(uid),
                new ScheduledWorkoutRepository(db).getByUid(uid));
    }

    /** The user's uid; always set, even without a profile. */
    public String getUid() {
        return uid;
    }

    /** The profile, or null if it is not synced yet. */
    public User getUser() {
        return user;
    }

    /** The custom workouts, by name. */
    public List<CustomWorkout> getCustomWorkouts() {
        return customWorkouts;
    }

    /** Custom workout id to its exercises, in order. */
    public Map<String, List<CustomExerciseOrderEntry>> getExercisesByWorkout() {
        return exercisesByWorkout;
    }

    /** The workout logs, newest first. */
    public List<WorkoutLog> getLogs() {
        return logs;
    }

    /** The schedules, soonest first. */
    public List<ScheduledWorkout> getSchedules() {
        return schedules;
    }
}
