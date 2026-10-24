package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.rounded;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        BottomNav.attach(this, R.id.nav_history, previewMode);
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
        int[] labels = {R.string.history_period_week, R.string.history_period_month, R.string.history_period_all};
        for (int i = 0; i < options.length; i++) {
            boolean selected = period == periods[i];
            options[i].setText(labels[i]);
            options[i].setTextColor(selected ? UiColors.INK : UiColors.MUTED);
            options[i].setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            options[i].setBackground(selected ? rounded(UiColors.ORANGE, dp(this, 24)) : null);
        }
    }

    private void loadData() {
        loading.setVisibility(View.VISIBLE);
        loading.setText(R.string.history_loading);
        statsService.getStats(period, new ServiceCallback<WorkoutStats>() {
            @Override public void onSuccess(WorkoutStats stats) { showStats(stats); }
            @Override public void onError(String message) {
                loading.setText(R.string.history_stats_failed);
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
                loading.setText(R.string.history_logs_failed);
                logList.removeAllViews();
                showMessage(message);
            }
        });
    }

    private void showStats(WorkoutStats stats) {
        workoutsTotal.setText(String.valueOf(stats.getWorkouts()));
        timeTotal.setText(Durations.spent(this, stats.getTotalSeconds()));
        caloriesTotal.setText(getString(R.string.energy_kcal, stats.getTotalKcal()));
        completionLine.setText(getString(R.string.history_completion,
                stats.getCompletedWorkouts(), stats.getPartialWorkouts()));
    }

    private void renderLogs() {
        logList.removeAllViews();
        loading.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        if (logs.isEmpty()) {
            loading.setText(R.string.history_empty);
        }
        logCount.setText(getResources().getQuantityString(R.plurals.history_log_count,
                logs.size(), logs.size()));
        for (WorkoutLog log : logs) logList.addView(createLogCard(log));
    }

    private View createLogCard(WorkoutLog log) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(this, 11);
        card.setLayoutParams(cp);
        card.setRadius(dp(this, 20));
        card.setCardBackgroundColor(UiColors.SURFACE);
        card.setCardElevation(0);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(this, 1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(this, 15), dp(this, 14), dp(this, 15), dp(this, 14));
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
        status.setText(complete ? R.string.history_status_completed : R.string.history_status_partial);
        status.setTextColor(complete ? UiColors.SUCCESS : UiColors.WARNING);
        status.setTextSize(9);
        status.setTypeface(null, Typeface.BOLD);
        status.setPadding(dp(this, 9), dp(this, 6), dp(this, 9), dp(this, 6));
        status.setBackground(rounded(complete ? UiColors.SUCCESS_TINT : UiColors.WARNING_TINT, dp(this, 18)));
        heading.addView(status);

        TextView date = new TextView(this);
        date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(log.getStartedAt() * 1000L)));
        date.setTextColor(UiColors.MUTED);
        date.setTextSize(12);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(this, 7);
        body.addView(date, dateParams);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(-1, -2);
        metricParams.topMargin = dp(this, 12);
        body.addView(metrics, metricParams);
        addMetric(metrics, Durations.spent(this, log.getDurationSeconds()));
        addSeparator(metrics);
        addMetric(metrics, getString(R.string.energy_kcal, log.getKcal()));
        addSeparator(metrics);
        addMetric(metrics, getResources().getQuantityString(R.plurals.history_exercise_count,
                log.getExercises().size(), log.getExercises().size()));

        TextView hint = new TextView(this);
        hint.setText(R.string.history_card_hint);
        hint.setTextColor(UiColors.ORANGE_DARK);
        hint.setTextSize(11);
        hint.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(this, 11);
        body.addView(hint, hintParams);

        card.setContentDescription(getString(R.string.history_card_description,
                log.getWorkoutName(), log.getStatus()));
        card.setOnClickListener(v -> showLogDetails(log));
        card.setOnLongClickListener(v -> {
            if (previewMode) showMessage(getString(R.string.history_preview_no_delete));
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
        dot.setPadding(dp(this, 8), 0, dp(this, 8), 0);
        row.addView(dot);
    }

    private void showLogDetails(WorkoutLog log) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(this, 20), dp(this, 6), dp(this, 20), dp(this, 8));
        TextView summary = new TextView(this);
        summary.setText(getString(R.string.history_log_summary,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(new Date(log.getStartedAt() * 1000L)),
                Durations.spent(this, log.getDurationSeconds()), log.getKcal(), log.getStatus()));
        summary.setTextColor(UiColors.MUTED);
        summary.setTextSize(13);
        content.addView(summary);
        TextView exercisesHeader = new TextView(this);
        exercisesHeader.setText(R.string.history_exercises_header);
        exercisesHeader.setTextColor(UiColors.ORANGE_DARK);
        exercisesHeader.setTextSize(11);
        exercisesHeader.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.topMargin = dp(this, 18);
        content.addView(exercisesHeader, headerParams);
        for (LoggedExercise exercise : log.getExercises()) {
            TextView line = new TextView(this);
            line.setText(getString(R.string.history_exercise_line,
                    exercise.getExerciseName(), exerciseDescription(exercise)));
            line.setTextColor(UiColors.GRAPHITE);
            line.setTextSize(14);
            line.setLineSpacing(dp(this, 3), 1f);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, -2);
            lineParams.topMargin = dp(this, 12);
            content.addView(line, lineParams);
            line.setTypeface(null, Typeface.NORMAL);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(content);
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(log.getWorkoutName()).setView(scroll)
                .setPositiveButton(R.string.common_done, null).show();
    }

    private String exerciseDescription(LoggedExercise exercise) {
        String target;
        if (exercise.isTimed()) target = exercise.getSeconds() == null ? getString(R.string.target_timed)
                : getString(R.string.target_seconds, exercise.getSeconds());
        else target = exercise.getReps() == null ? getString(R.string.target_reps_unknown)
                : getResources().getQuantityString(R.plurals.target_reps, exercise.getReps(), exercise.getReps());
        if (exercise.isPerSide()) target = getString(R.string.target_per_side, target);
        return getResources().getQuantityString(R.plurals.history_sets_done, exercise.getSets(),
                exercise.getCompletedSets(), exercise.getSets(), target);
    }

    private void confirmDelete(WorkoutLog log) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.history_delete_title)
                .setMessage(getString(R.string.history_delete_message, log.getWorkoutName()))
                .setNegativeButton(R.string.history_delete_keep, null)
                .setPositiveButton(R.string.common_delete, (dialog, which) -> logService.delete(log.getId(), new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void ignored) { showMessage(getString(R.string.history_deleted)); loadData(); }
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

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.historyRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
