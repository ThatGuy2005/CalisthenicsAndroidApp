package com.example.calisthenicsapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.StatsPeriod;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.CoreComponents.WorkoutStats;
import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Service.CustomWorkoutService;
import com.example.calisthenicsapp.Service.DataChanges;
import com.example.calisthenicsapp.Service.ScheduleService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.StatsService;
import com.example.calisthenicsapp.Service.WorkoutLogService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = "com.example.calisthenicsapp.PREVIEW_MODE";
    private TextView nextWorkoutName;
    private TextView nextWorkoutDate;
    private TextView weeklyWorkoutCount;
    private TextView weeklyTrainingTime;
    private TextView weeklyCalories;
    private TextView lastWorkoutName;
    private TextView lastWorkoutDetails;
    private TextView lastWorkoutStatus;

    private ScheduleService schedules;
    private StatsService stats;
    private WorkoutLogService workoutLogs;
    private CustomWorkoutService customWorkouts;
    private boolean previewMode;

    private final DataChanges.Listener dataListener = kind -> {
        if (kind == DataChanges.Kind.SCHEDULES || kind == DataChanges.Kind.CUSTOM_WORKOUTS) {
            loadUpcomingWorkout();
        }
        if (kind == DataChanges.Kind.WORKOUT_LOGS) {
            loadWeeklyStats();
            loadLastWorkout();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        previewMode = getIntent().getBooleanExtra(EXTRA_PREVIEW_MODE, false);
        if (!previewMode) {
            schedules = new ScheduleService(this);
            stats = new StatsService(this);
            workoutLogs = new WorkoutLogService(this);
            customWorkouts = new CustomWorkoutService(this);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        nextWorkoutName = findViewById(R.id.nextWorkoutName);
        nextWorkoutDate = findViewById(R.id.nextWorkoutDate);
        weeklyWorkoutCount = findViewById(R.id.weeklyWorkoutCount);
        weeklyTrainingTime = findViewById(R.id.weeklyTrainingTime);
        weeklyCalories = findViewById(R.id.weeklyCalories);
        lastWorkoutName = findViewById(R.id.lastWorkoutName);
        lastWorkoutDetails = findViewById(R.id.lastWorkoutDetails);
        lastWorkoutStatus = findViewById(R.id.lastWorkoutStatus);

        Button signOut = findViewById(R.id.homeSignOut);
        findViewById(R.id.homeSettings).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));
        signOut.setOnClickListener(view -> {
            if (previewMode) {
                finish();
                return;
            }
            new AuthService(this).signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.homeScheduleAction).setOnClickListener(view -> openSchedule());
        findViewById(R.id.homeWorkoutsAction).setOnClickListener(view -> openWorkouts());
        findViewById(R.id.homeExercisesAction).setOnClickListener(view -> showComingSoon());

        BottomNavigationView navigation = findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(R.id.nav_home);
        navigation.setOnItemSelectedListener(this::onNavigationSelected);

        if (previewMode) {
            showPreviewData(signOut);
        } else {
            DataChanges.observe(this, dataListener,
                    DataChanges.Kind.SCHEDULES,
                    DataChanges.Kind.CUSTOM_WORKOUTS,
                    DataChanges.Kind.WORKOUT_LOGS);
        }
    }

    private void showPreviewData(Button signOut) {
        signOut.setText("Exit preview");
        ((TextView) findViewById(R.id.homeSubtitle))
                .setText("UI preview · sample data only, no account needed");
        nextWorkoutName.setText("Upper body strength");
        nextWorkoutDate.setText("Today · 6:00 PM");
        weeklyWorkoutCount.setText("3");
        weeklyTrainingTime.setText("1 h 45 min");
        weeklyCalories.setText("420");
        lastWorkoutName.setText("Push & Core");
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
        lastWorkoutDetails.setText(date + " · 42 min · 185 kcal");
        lastWorkoutStatus.setText("COMPLETED");
    }

    private boolean onNavigationSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_home) return true;
        if (item.getItemId() == R.id.nav_exercises) {
            Intent intent = new Intent(this, ExerciseListActivity.class);
            intent.putExtra(ExerciseListActivity.EXTRA_PREVIEW_MODE, previewMode);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.nav_workouts) {
            openWorkouts();
            return true;
        }
        if (item.getItemId() == R.id.nav_schedule) {
            openSchedule();
            return true;
        }
        if (item.getItemId() == R.id.nav_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.putExtra(HistoryActivity.EXTRA_PREVIEW_MODE, previewMode);
            startActivity(intent);
            return true;
        }
        showComingSoon();
        return false;
    }

    private void loadUpcomingWorkout() {
        schedules.getUpcoming(new ServiceCallback<List<ScheduledWorkout>>() {
            @Override
            public void onSuccess(List<ScheduledWorkout> upcoming) {
                if (upcoming.isEmpty()) {
                    nextWorkoutName.setText("Nothing scheduled yet");
                    nextWorkoutDate.setText("Choose a workout and plan your next session.");
                    return;
                }

                ScheduledWorkout next = upcoming.get(0);
                Long nextAt = next.nextOccurrence(System.currentTimeMillis() / 1000L,
                        TimeZone.getDefault());
                nextWorkoutDate.setText(nextAt == null ? "Upcoming workout"
                        : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(new Date(nextAt * 1000L)));

                if (!next.isCustom()) {
                    nextWorkoutName.setText(next.getWorkoutName());
                    return;
                }

                nextWorkoutName.setText("Your custom workout");
                customWorkouts.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
                    @Override
                    public void onSuccess(List<CustomWorkout> workouts) {
                        for (CustomWorkout workout : workouts) {
                            if (workout.getId().equals(next.getCustomWorkoutId())) {
                                nextWorkoutName.setText(workout.getName());
                                break;
                            }
                        }
                    }

                    @Override
                    public void onError(String message) {
                        // Keep the useful generic custom-workout label when this lookup fails.
                    }
                });
            }

            @Override
            public void onError(String message) {
                nextWorkoutName.setText("Upcoming workout unavailable");
                nextWorkoutDate.setText(message);
            }
        });
    }

    private void loadWeeklyStats() {
        stats.getStats(StatsPeriod.WEEK, new ServiceCallback<WorkoutStats>() {
            @Override
            public void onSuccess(WorkoutStats result) {
                weeklyWorkoutCount.setText(String.valueOf(result.getWorkouts()));
                weeklyTrainingTime.setText(formatDuration(result.getTotalSeconds()));
                weeklyCalories.setText(String.format(java.util.Locale.getDefault(),
                        "%.0f", result.getTotalKcal()));
            }

            @Override
            public void onError(String message) {
                weeklyWorkoutCount.setText("—");
                weeklyTrainingTime.setText("—");
                weeklyCalories.setText("—");
                showMessage(message);
            }
        });
    }

    private void loadLastWorkout() {
        workoutLogs.getMyLogs(StatsPeriod.ALL_TIME, new ServiceCallback<List<WorkoutLog>>() {
            @Override
            public void onSuccess(List<WorkoutLog> logs) {
                if (logs.isEmpty()) {
                    lastWorkoutName.setText("No workouts logged yet");
                    lastWorkoutDetails.setText("Complete your first workout to see it here.");
                    lastWorkoutStatus.setText("");
                    return;
                }

                WorkoutLog latest = logs.get(0);
                lastWorkoutName.setText(latest.getWorkoutName());
                String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                        DateFormat.SHORT).format(new Date(latest.getStartedAt() * 1000L));
                lastWorkoutDetails.setText(date + " · " + formatDuration(latest.getDurationSeconds())
                        + " · " + String.format(java.util.Locale.getDefault(), "%.0f kcal",
                        latest.getKcal()));
                lastWorkoutStatus.setText(latest.getStatus());
                lastWorkoutStatus.setTextColor(WorkoutLog.STATUS_COMPLETED.equals(latest.getStatus())
                        ? UiColors.SUCCESS : UiColors.ORANGE_DARK);
            }

            @Override
            public void onError(String message) {
                lastWorkoutName.setText("Workout history unavailable");
                lastWorkoutDetails.setText(message);
                lastWorkoutStatus.setText("");
            }
        });
    }

    private String formatDuration(long seconds) {
        long minutes = Math.max(0, seconds) / 60;
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours == 0) return minutes + " min";
        if (remainingMinutes == 0) return hours + " h";
        return hours + " h " + remainingMinutes + " min";
    }

    private void showComingSoon() {
        showMessage("This section is coming next.");
    }

    private void openWorkouts() {
        Intent intent = new Intent(this, WorkoutListActivity.class);
        intent.putExtra(WorkoutListActivity.EXTRA_PREVIEW_MODE, previewMode);
        startActivity(intent);
    }

    private void openSchedule() {
        Intent intent = new Intent(this, ScheduleActivity.class);
        intent.putExtra(ScheduleActivity.EXTRA_PREVIEW_MODE, previewMode);
        startActivity(intent);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.homeRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
