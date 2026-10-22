package com.example.calisthenicsapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(
                getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

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

        findViewById(R.id.homeScheduleAction).setOnClickListener(view ->
                BottomNav.open(this, R.id.nav_schedule, previewMode));
        findViewById(R.id.homeWorkoutsAction).setOnClickListener(view ->
                BottomNav.open(this, R.id.nav_workouts, previewMode));
        findViewById(R.id.homeExercisesAction).setOnClickListener(view ->
                BottomNav.open(this, R.id.nav_exercises, previewMode));

        BottomNav.attach(this, R.id.nav_home, previewMode);

        if (previewMode) {
            showPreviewData(signOut);
        } else {
            DataChanges.observe(this, dataListener,
                    DataChanges.Kind.SCHEDULES,
                    DataChanges.Kind.CUSTOM_WORKOUTS,
                    DataChanges.Kind.WORKOUT_LOGS);
        }
    }

    // Made-up sample data for the preview mode, so not worth string resources.
    @SuppressLint("SetTextI18n")
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
        lastWorkoutStatus.setVisibility(View.VISIBLE);
    }

    private void loadUpcomingWorkout() {
        schedules.getUpcoming(new ServiceCallback<List<ScheduledWorkout>>() {
            @Override
            public void onSuccess(List<ScheduledWorkout> upcoming) {
                if (upcoming.isEmpty()) {
                    nextWorkoutName.setText(R.string.home_nothing_scheduled);
                    nextWorkoutDate.setText(R.string.home_nothing_scheduled_hint);
                    return;
                }

                ScheduledWorkout next = upcoming.get(0);
                Long nextAt = next.nextOccurrence(System.currentTimeMillis() / 1000L,
                        TimeZone.getDefault());
                nextWorkoutDate.setText(nextAt == null ? getString(R.string.home_upcoming_workout)
                        : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(new Date(nextAt * 1000L)));

                if (!next.isCustom()) {
                    nextWorkoutName.setText(next.getWorkoutName());
                    return;
                }

                nextWorkoutName.setText(R.string.home_custom_workout);
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
                nextWorkoutName.setText(R.string.home_upcoming_unavailable);
                nextWorkoutDate.setText(message);
            }
        });
    }

    private void loadWeeklyStats() {
        stats.getStats(StatsPeriod.WEEK, new ServiceCallback<WorkoutStats>() {
            @Override
            public void onSuccess(WorkoutStats result) {
                weeklyWorkoutCount.setText(String.valueOf(result.getWorkouts()));
                weeklyTrainingTime.setText(Durations.spent(HomeActivity.this, result.getTotalSeconds()));
                weeklyCalories.setText(String.format(java.util.Locale.getDefault(),
                        "%.0f", result.getTotalKcal()));
            }

            @Override
            public void onError(String message) {
                weeklyWorkoutCount.setText(R.string.home_stat_placeholder);
                weeklyTrainingTime.setText(R.string.home_stat_placeholder);
                weeklyCalories.setText(R.string.home_stat_placeholder);
                showMessage(message);
            }
        });
    }

    private void loadLastWorkout() {
        workoutLogs.getMyLogs(StatsPeriod.ALL_TIME, new ServiceCallback<List<WorkoutLog>>() {
            @Override
            public void onSuccess(List<WorkoutLog> logs) {
                if (logs.isEmpty()) {
                    lastWorkoutName.setText(R.string.home_no_workouts);
                    lastWorkoutDetails.setText(R.string.home_no_workouts_hint);
                    lastWorkoutStatus.setVisibility(View.GONE);
                    return;
                }

                WorkoutLog latest = logs.get(0);
                lastWorkoutName.setText(latest.getWorkoutName());
                String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                        DateFormat.SHORT).format(new Date(latest.getStartedAt() * 1000L));
                lastWorkoutDetails.setText(getString(R.string.home_last_workout_details, date,
                        Durations.spent(HomeActivity.this, latest.getDurationSeconds()), latest.getKcal()));
                boolean completed = WorkoutLog.STATUS_COMPLETED.equals(latest.getStatus());
                lastWorkoutStatus.setText(completed ? R.string.home_status_completed : R.string.home_status_partial);
                lastWorkoutStatus.setTextColor(completed ? UiColors.SUCCESS : UiColors.WARNING);
                lastWorkoutStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                lastWorkoutName.setText(R.string.home_history_unavailable);
                lastWorkoutDetails.setText(message);
                lastWorkoutStatus.setVisibility(View.GONE);
            }
        });
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.homeRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
