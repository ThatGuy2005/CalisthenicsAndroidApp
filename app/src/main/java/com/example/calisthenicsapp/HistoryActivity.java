package com.example.calisthenicsapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.LoggedExercise;
import com.example.calisthenicsapp.CoreComponents.StatsPeriod;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.CoreComponents.WorkoutStats;
import com.example.calisthenicsapp.Service.DataChanges;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.StatsService;
import com.example.calisthenicsapp.Service.WorkoutLogService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Workout log and period statistics (FR-5.7). */
public class HistoryActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = HomeActivity.EXTRA_PREVIEW_MODE;

    private StatsPeriod period = StatsPeriod.WEEK;
    private boolean previewMode;
    private WorkoutLogService logService;
    private StatsService statsService;
    private TextView workoutsTotal, timeTotal, caloriesTotal, completionLine, logCount, loading;
    private LinearLayout logList;
    private TextView week, month, allTime;
    private List<WorkoutLog> logs = new ArrayList<>();

    private final DataChanges.Listener dataListener = kind -> {
        if (kind == DataChanges.Kind.WORKOUT_LOGS) loadData();
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        previewMode = getIntent().getBooleanExtra(EXTRA_PREVIEW_MODE, false);
        workoutsTotal = findViewById(R.id.historyWorkoutTotal);
        timeTotal = findViewById(R.id.historyTimeTotal);
        caloriesTotal = findViewById(R.id.historyCaloriesTotal);
        completionLine = findViewById(R.id.historyCompletionLine);
        logCount = findViewById(R.id.historyLogCount);
        loading = findViewById(R.id.historyLoading);
        logList = findViewById(R.id.historyLogList);
        week = findViewById(R.id.historyWeek);
        month = findViewById(R.id.historyMonth);
        allTime = findViewById(R.id.historyAllTime);
        if (!previewMode) {
            logService = new WorkoutLogService(this);
            statsService = new StatsService(this);
        }
        week.setOnClickListener(v -> selectPeriod(StatsPeriod.WEEK));
        month.setOnClickListener(v -> selectPeriod(StatsPeriod.MONTH));
        allTime.setOnClickListener(v -> selectPeriod(StatsPeriod.ALL_TIME));
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_history);
        nav.setOnItemSelectedListener(this::onNavigationSelected);
        renderPeriodPicker();
        if (previewMode) renderPreview();
        else DataChanges.observe(this, dataListener, DataChanges.Kind.WORKOUT_LOGS);
    }

    private void selectPeriod(StatsPeriod selected) {
        if (period == selected) return;
        period = selected;
        renderPeriodPicker();
        if (previewMode) renderPreview(); else loadData();
    }

    private void renderPeriodPicker() {
        TextView[] options = {week, month, allTime};
        StatsPeriod[] periods = {StatsPeriod.WEEK, StatsPeriod.MONTH, StatsPeriod.ALL_TIME};
        String[] labels = {"This week", "This month", "All time"};
        for (int i = 0; i < options.length; i++) {
            boolean selected = period == periods[i];
            options[i].setText(labels[i]);
            options[i].setTextColor(selected ? UiColors.GRAPHITE : UiColors.MUTED);
            options[i].setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            options[i].setBackground(selected ? rounded(UiColors.ORANGE, dp(24)) : null);
        }
    }

    private void loadData() {
        loading.setVisibility(View.VISIBLE);
        loading.setText("Loading your workouts…");
        statsService.getStats(period, new ServiceCallback<WorkoutStats>() {
            @Override public void onSuccess(WorkoutStats stats) { showStats(stats); }
            @Override public void onError(String message) {
                loading.setText("Could not load statistics. Pull down or choose a period to retry.");
                showMessage(message);
            }
        });
        logService.getMyLogs(period, new ServiceCallback<List<WorkoutLog>>() {
            @Override public void onSuccess(List<WorkoutLog> result) {
                logs = result;
                renderLogs();
            }
            @Override public void onError(String message) {
                loading.setVisibility(View.VISIBLE);
                loading.setText("Workout history could not be loaded. Choose a period to retry.");
                logList.removeAllViews();
                showMessage(message);
            }
        });
    }

    private void showStats(WorkoutStats stats) {
        workoutsTotal.setText(String.valueOf(stats.getWorkouts()));
        timeTotal.setText(formatDuration(stats.getTotalSeconds()));
        caloriesTotal.setText(String.format(Locale.getDefault(), "%.0f kcal", stats.getTotalKcal()));
        completionLine.setText(stats.getCompletedWorkouts() + " completed  ·  "
                + stats.getPartialWorkouts() + " partial");
    }

    private void renderLogs() {
        logList.removeAllViews();
        loading.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        if (logs.isEmpty()) {
            loading.setText("No workouts in this period yet. Complete a workout and it will appear here.");
        }
        logCount.setText(logs.size() + (logs.size() == 1 ? " workout" : " workouts"));
        for (WorkoutLog log : logs) logList.addView(createLogCard(log));
    }

    private View createLogCard(WorkoutLog log) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(11);
        card.setLayoutParams(cp);
        card.setRadius(dp(20));
        card.setCardBackgroundColor(Color.WHITE);
        card.setCardElevation(0);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.addView(body);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        TextView name = new TextView(this);
        name.setText(log.getWorkoutName());
        name.setTextColor(UiColors.GRAPHITE);
        name.setTextSize(16);
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(2);
        heading.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        boolean complete = WorkoutLog.STATUS_COMPLETED.equals(log.getStatus());
        TextView status = new TextView(this);
        status.setText(complete ? "COMPLETED" : "PARTIAL");
        status.setTextColor(complete ? UiColors.SUCCESS : 0xFF8C6631);
        status.setTextSize(9);
        status.setTypeface(null, Typeface.BOLD);
        status.setPadding(dp(9), dp(6), dp(9), dp(6));
        status.setBackground(rounded(complete ? UiColors.SUCCESS_TINT : 0xFFFFF1DB, dp(18)));
        heading.addView(status);

        TextView date = new TextView(this);
        date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(log.getStartedAt() * 1000L)));
        date.setTextColor(UiColors.MUTED);
        date.setTextSize(12);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(7);
        body.addView(date, dateParams);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(-1, -2);
        metricParams.topMargin = dp(12);
        body.addView(metrics, metricParams);
        addMetric(metrics, formatDuration(log.getDurationSeconds()));
        addSeparator(metrics);
        addMetric(metrics, String.format(Locale.getDefault(), "%.0f kcal", log.getKcal()));
        addSeparator(metrics);
        addMetric(metrics, log.getExercises().size() + " exercises");

        TextView hint = new TextView(this);
        hint.setText("Tap for workout details  ·  Hold to delete");
        hint.setTextColor(UiColors.ORANGE_DARK);
        hint.setTextSize(11);
        hint.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(11);
        body.addView(hint, hintParams);

        card.setContentDescription(log.getWorkoutName() + ", " + log.getStatus() + ". Tap to see details.");
        card.setOnClickListener(v -> showLogDetails(log));
        card.setOnLongClickListener(v -> {
            if (previewMode) showMessage("Preview workout logs cannot be deleted.");
            else confirmDelete(log);
            return true;
        });
        return card;
    }

    private void addMetric(LinearLayout row, String text) {
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(UiColors.MUTED);
        item.setTextSize(11);
        item.setTypeface(null, Typeface.BOLD);
        row.addView(item);
    }

    private void addSeparator(LinearLayout row) {
        TextView dot = new TextView(this);
        dot.setText("·");
        dot.setTextColor(0xFF9AA69B);
        dot.setTextSize(14);
        dot.setPadding(dp(8), 0, dp(8), 0);
        row.addView(dot);
    }

    private void showLogDetails(WorkoutLog log) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(6), dp(20), dp(8));
        TextView summary = new TextView(this);
        summary.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(log.getStartedAt() * 1000L)) + "\n" + formatDuration(log.getDurationSeconds())
                + " · " + String.format(Locale.getDefault(), "%.0f kcal", log.getKcal())
                + " · " + log.getStatus());
        summary.setTextColor(UiColors.MUTED);
        summary.setTextSize(13);
        content.addView(summary);
        TextView exercisesHeader = new TextView(this);
        exercisesHeader.setText("EXERCISES");
        exercisesHeader.setTextColor(UiColors.ORANGE_DARK);
        exercisesHeader.setTextSize(11);
        exercisesHeader.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.topMargin = dp(18);
        content.addView(exercisesHeader, headerParams);
        for (LoggedExercise exercise : log.getExercises()) {
            TextView line = new TextView(this);
            line.setText(exercise.getExerciseName() + "\n" + exerciseDescription(exercise));
            line.setTextColor(UiColors.GRAPHITE);
            line.setTextSize(14);
            line.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, -2);
            lineParams.topMargin = dp(12);
            content.addView(line, lineParams);
            line.setTypeface(null, Typeface.NORMAL);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(content);
        new MaterialAlertDialogBuilder(this).setTitle(log.getWorkoutName()).setView(scroll)
                .setPositiveButton("Done", null).show();
    }

    private String exerciseDescription(LoggedExercise exercise) {
        String target;
        if (exercise.isTimed()) target = (exercise.getSeconds() == null ? "Timed" : exercise.getSeconds() + " sec");
        else target = (exercise.getReps() == null ? "Reps" : exercise.getReps() + " reps");
        if (exercise.isPerSide()) target += " per side";
        return exercise.getCompletedSets() + " of " + exercise.getSets() + " sets  ·  " + target;
    }

    private void confirmDelete(WorkoutLog log) {
        new MaterialAlertDialogBuilder(this).setTitle("Delete workout log?")
                .setMessage("“" + log.getWorkoutName() + "” will be removed from your history.")
                .setNegativeButton("Keep", null)
                .setPositiveButton("Delete", (dialog, which) -> logService.delete(log.getId(), new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void ignored) { showMessage("Workout removed from history."); loadData(); }
                    @Override public void onError(String message) { showMessage(message); }
                })).show();
    }

    private void renderPreview() {
        long now = System.currentTimeMillis() / 1000L;
        List<LoggedExercise> firstExercises = new ArrayList<>();
        firstExercises.add(new LoggedExercise("Push-Up", 3.8, false, 3, 3, 12, null, 270, 38, false));
        firstExercises.add(new LoggedExercise("Plank", 3.0, true, 3, 3, null, 30, 180, 25, false));
        List<LoggedExercise> secondExercises = new ArrayList<>();
        secondExercises.add(new LoggedExercise("Bodyweight Squat", 5.0, false, 3, 2, 15, null, 125, 20, false));
        logs = new ArrayList<>();
        if (period == StatsPeriod.WEEK || period == StatsPeriod.MONTH || period == StatsPeriod.ALL_TIME) {
            logs.add(new WorkoutLog("preview-1", "preview", "Upper Body Focus", now - 3600,
                    1680, 63, WorkoutLog.STATUS_COMPLETED, firstExercises));
        }
        if (period != StatsPeriod.WEEK) {
            logs.add(new WorkoutLog("preview-2", "preview", "Leg Day Basics", now - 172800,
                    900, 20, WorkoutLog.STATUS_PARTIAL, secondExercises));
        }
        showStats(new WorkoutStats(logs.size(), logs.size() > 0 ? 1 : 0,
                logs.size() > 1 ? 1 : 0, logs.stream().mapToLong(WorkoutLog::getDurationSeconds).sum(),
                logs.stream().mapToDouble(WorkoutLog::getKcal).sum()));
        renderLogs();
    }

    private boolean onNavigationSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_history) return true;
        Intent intent;
        if (item.getItemId() == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
        else if (item.getItemId() == R.id.nav_workouts) intent = new Intent(this, WorkoutListActivity.class);
        else if (item.getItemId() == R.id.nav_exercises) intent = new Intent(this, ExerciseListActivity.class);
        else intent = new Intent(this, ScheduleActivity.class);
        intent.putExtra(HomeActivity.EXTRA_PREVIEW_MODE, previewMode);
        startActivity(intent);
        finish();
        return true;
    }

    private String formatDuration(long seconds) {
        long minutes = Math.max(0, seconds) / 60;
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours == 0) return minutes + " min";
        return remainingMinutes == 0 ? hours + " h" : hours + " h " + remainingMinutes + " min";
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.historyRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
