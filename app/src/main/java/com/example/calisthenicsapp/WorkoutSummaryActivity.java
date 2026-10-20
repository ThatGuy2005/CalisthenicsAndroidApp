package com.example.calisthenicsapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutLogService;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

/** Final workout totals and automatic log save (FR-5.3 to FR-5.6). */
public class WorkoutSummaryActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION = "com.example.calisthenicsapp.COMPLETED_SESSION";
    private static final String STATE_SESSION = "summary_workout_session";
    private WorkoutSession session;
    private TextView saved;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_summary);
        session = savedInstanceState == null ? null
                : (WorkoutSession) savedInstanceState.getSerializable(STATE_SESSION);
        if (session == null) session = (WorkoutSession) getIntent().getSerializableExtra(EXTRA_SESSION);
        if (session == null) { finish(); return; }
        saved = findViewById(R.id.summarySaved);
        bindSummary();
        findViewById(R.id.summaryHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.summaryWorkouts).setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        saveIfNeeded();
    }

    private void bindSummary() {
        boolean complete = WorkoutSession.STATUS_COMPLETED.equals(session.getStatus());
        ((TextView) findViewById(R.id.summaryTitle)).setText(complete ? R.string.summary_title_complete : R.string.summary_title_partial);
        ((TextView) findViewById(R.id.summaryWorkoutName)).setText(getString(complete
                ? R.string.summary_workout_completed : R.string.summary_workout_partial,
                session.getWorkoutName()));
        ((TextView) findViewById(R.id.summaryDuration)).setText(formatTime(session.getTotalSeconds()));
        ((TextView) findViewById(R.id.summaryCalories)).setText(String.format(Locale.getDefault(), "%.0f kcal", session.getTotalKcal()));
        int totalSets = 0;
        for (WorkoutSession.SessionExercise e : session.getExercises()) totalSets += e.getSets();
        ((TextView) findViewById(R.id.summarySets)).setText(getResources().getQuantityString(
                R.plurals.summary_sets, totalSets, session.getCompletedSets(), totalSets));
        LinearLayout list = findViewById(R.id.summaryExerciseList);
        list.removeAllViews();
        for (WorkoutSession.SessionExercise exercise : session.getExercises()) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(10);
            card.setLayoutParams(cp);
            card.setRadius(dp(17));
            card.setCardBackgroundColor(UiColors.SURFACE);
            card.setStrokeColor(UiColors.BORDER);
            card.setStrokeWidth(dp(1));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(15), dp(14), dp(15), dp(14));
            TextView title = new TextView(this);
            title.setText(exercise.getName());
            title.setTextColor(UiColors.GRAPHITE);
            title.setTextSize(15);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView sets = new TextView(this);
            sets.setText(getResources().getQuantityString(R.plurals.summary_exercise_sets,
                    exercise.getSets(), exercise.getCompletedSets(), exercise.getSets()));
            sets.setTextColor(exercise.getCompletedSets() == exercise.getSets() ? UiColors.SUCCESS : UiColors.MUTED);
            sets.setTextSize(13);
            sets.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(sets);
            card.addView(row);
            list.addView(card);
        }
    }

    private void saveIfNeeded() {
        if (!session.isLoggable()) {
            saved.setText(R.string.summary_nothing_to_save);
            return;
        }
        if (session.getLogId() != null) {
            saved.setText(R.string.summary_saved);
            return;
        }
        saved.setText(R.string.summary_saving);
        new WorkoutLogService(this).saveSession(session, new ServiceCallback<String>() {
            @Override public void onSuccess(String logId) {
                saved.setText(R.string.summary_saved);
            }
            @Override public void onError(String message) {
                saved.setText(R.string.summary_save_failed);
                saved.setOnClickListener(v -> saveIfNeeded());
            }
        });
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (session != null) outState.putSerializable(STATE_SESSION, session);
        super.onSaveInstanceState(outState);
    }
}
