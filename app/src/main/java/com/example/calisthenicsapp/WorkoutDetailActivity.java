package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.pill;
import static com.example.calisthenicsapp.Views.rounded;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.CoreComponents.ExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Service.ExerciseService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutService;
import com.example.calisthenicsapp.Service.WorkoutSessionService;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkoutDetailActivity extends AppCompatActivity {
    private TextView name, meta, description, count;
    private View startButton;
    private LinearLayout exerciseList;
    private WorkoutService workouts;
    private ExerciseService exercises;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);
        name = findViewById(R.id.workoutDetailName);
        meta = findViewById(R.id.workoutDetailMeta);
        description = findViewById(R.id.workoutDetailDescription);
        count = findViewById(R.id.workoutExerciseCount);
        startButton = findViewById(R.id.startWorkoutButton);
        exerciseList = findViewById(R.id.workoutExerciseList);
        workouts = new WorkoutService(this);
        exercises = new ExerciseService(this);
        findViewById(R.id.workoutBack).setOnClickListener(v -> finish());
        String workoutName = getIntent().getStringExtra(WorkoutListActivity.EXTRA_WORKOUT_NAME);
        startButton.setOnClickListener(v -> startWorkout(workoutName));
        load(workoutName);
    }

    private void startWorkout(String workoutName) {
        if (workoutName == null) { showError(getString(R.string.workout_detail_not_found)); return; }
        startButton.setEnabled(false);
        ((TextView) startButton).setText(R.string.workout_detail_preparing);
        new WorkoutSessionService(this).startPremade(workoutName, new ServiceCallback<WorkoutSession>() {
            @Override public void onSuccess(WorkoutSession session) {
                Intent intent = new Intent(WorkoutDetailActivity.this, WorkoutSessionActivity.class);
                intent.putExtra(WorkoutSessionActivity.EXTRA_SESSION, session);
                startActivity(intent);
                startButton.setEnabled(true);
                ((TextView) startButton).setText(R.string.workout_detail_start);
            }
            @Override public void onError(String message) {
                startButton.setEnabled(true);
                ((TextView) startButton).setText(R.string.workout_detail_start);
                showError(message);
            }
        });
    }

    private void load(String workoutName) {
        if (workoutName == null) { showError(getString(R.string.workout_detail_not_found)); return; }
        workouts.getWorkout(workoutName, new ServiceCallback<Workout>() {
            @Override public void onSuccess(Workout workout) {
                if (workout == null) { showError(getString(R.string.workout_detail_not_found)); return; }
                name.setText(workout.getName());
                String category = workout.getCategory() == null ? getString(R.string.workout_no_category)
                        : workout.getCategory().getLabel();
                String level = workout.getLevel() == null ? getString(R.string.workout_any_level)
                        : workout.getLevel().getLabel();
                meta.setText(getString(R.string.workout_detail_meta, category, level,
                        Durations.estimate(WorkoutDetailActivity.this, workout.getEstimatedSeconds(),
                                R.string.workout_time_varies)));
                description.setText(workout.getDescription() == null ? "" : workout.getDescription());
                loadEntries(workoutName);
            }
            @Override public void onError(String message) { showError(message); }
        });
    }

    private void loadEntries(String workoutName) {
        workouts.getExercises(workoutName, new ServiceCallback<List<ExerciseOrderEntry>>() {
            @Override public void onSuccess(List<ExerciseOrderEntry> entries) {
                count.setText(getString(R.string.workout_detail_exercise_count, entries.size()));
                exerciseList.removeAllViews();
                if (entries.isEmpty()) return;
                List<String> details = new ArrayList<>();
                for (int i = 0; i < entries.size(); i++) details.add(null);
                AtomicInteger waiting = new AtomicInteger(entries.size());
                for (int i = 0; i < entries.size(); i++) {
                    final int index = i;
                    ExerciseOrderEntry entry = entries.get(i);
                    exercises.getExercise(entry.getExerciseName(), new ServiceCallback<Exercise>() {
                        @Override public void onSuccess(Exercise exercise) {
                            details.set(index, targetText(entry, exercise));
                            if (waiting.decrementAndGet() == 0) renderEntries(entries, details);
                        }
                        @Override public void onError(String message) {
                            details.set(index, targetText(entry, null));
                            if (waiting.decrementAndGet() == 0) renderEntries(entries, details);
                        }
                    });
                }
            }
            @Override public void onError(String message) { showError(message); }
        });
    }

    private String targetText(ExerciseOrderEntry entry, Exercise exercise) {
        Integer seconds = entry.getSeconds();
        Integer reps = entry.getReps();
        boolean timed = seconds != null || (reps == null && exercise != null && exercise.isTimed());
        if (seconds == null && reps == null && exercise != null) {
            seconds = exercise.getDefaultSeconds(); reps = exercise.getDefaultReps();
        }
        String target = timed
                ? (seconds == null ? getString(R.string.target_timed) : getString(R.string.target_seconds, seconds))
                : (reps == null ? getString(R.string.target_reps_unknown)
                        : getResources().getQuantityString(R.plurals.target_reps, reps, reps));
        if (exercise != null && exercise.isPerSide()) target = getString(R.string.target_per_side_short, target);
        return target;
    }

    private void renderEntries(List<ExerciseOrderEntry> entries, List<String> details) {
        exerciseList.removeAllViews();
        for (int i = 0; i < entries.size(); i++) {
            ExerciseOrderEntry entry = entries.get(i);
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(this, 12);
            card.setLayoutParams(cp);
            card.setRadius(dp(this, 21));
            card.setCardBackgroundColor(UiColors.SURFACE);
            card.setStrokeColor(UiColors.BORDER);
            card.setStrokeWidth(dp(this, 1));
            card.setCardElevation(dp(this, 1));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout shell = new LinearLayout(this);
            shell.setOrientation(LinearLayout.HORIZONTAL);
            View accent = new View(this);
            accent.setBackgroundColor(UiColors.ORANGE_DARK);
            shell.addView(accent, new LinearLayout.LayoutParams(dp(this, 4), ViewGroup.LayoutParams.MATCH_PARENT));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(this, 14), dp(this, 14), dp(this, 14), dp(this, 13));
            shell.addView(row, new LinearLayout.LayoutParams(0, -2, 1f));

            LinearLayout heading = new LinearLayout(this);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(heading, new LinearLayout.LayoutParams(-1, -2));

            TextView number = new TextView(this);
            number.setText(String.format(java.util.Locale.ROOT, "%02d", i + 1));
            number.setTextColor(UiColors.ORANGE_DARK);
            number.setTextSize(12);
            number.setTypeface(null, android.graphics.Typeface.BOLD);
            number.setGravity(Gravity.CENTER);
            number.setBackground(rounded(UiColors.ORANGE_TINT, dp(this, 30)));
            heading.addView(number, new LinearLayout.LayoutParams(dp(this, 36), dp(this, 36)));

            TextView title = new TextView(this);
            title.setText(entry.getExerciseName());
            title.setTextColor(UiColors.GRAPHITE);
            title.setTextSize(17);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setMaxLines(2);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
            titleParams.leftMargin = dp(this, 10);
            heading.addView(title, titleParams);

            TextView arrow = new TextView(this);
            arrow.setText("›");
            arrow.setTextColor(UiColors.ORANGE_DARK);
            arrow.setTextSize(24);
            arrow.setGravity(Gravity.CENTER);
            arrow.setBackground(rounded(UiColors.ORANGE_TINT, dp(this, 30)));
            heading.addView(arrow, new LinearLayout.LayoutParams(dp(this, 30), dp(this, 30)));

            LinearLayout stats = new LinearLayout(this);
            stats.setGravity(Gravity.CENTER_VERTICAL);
            stats.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
            statsParams.topMargin = dp(this, 12);
            row.addView(stats, statsParams);
            if (entry.getSets() != null) {
                stats.addView(pill(this, getResources().getQuantityString(R.plurals.target_sets,
                        entry.getSets(), entry.getSets()), UiColors.SURFACE_ALT, UiColors.MUTED));
            }
            TextView target = pill(this, details.get(i), UiColors.ORANGE_TINT, UiColors.ORANGE_DARK);
            LinearLayout.LayoutParams targetParams = new LinearLayout.LayoutParams(-2, dp(this, 30));
            targetParams.leftMargin = dp(this, 7);
            stats.addView(target, targetParams);
            if (entry.getRestSeconds() != null) {
                TextView rest = pill(this, getString(R.string.workout_detail_rest, entry.getRestSeconds()),
                        UiColors.SURFACE_ALT, UiColors.MUTED);
                LinearLayout.LayoutParams restParams = new LinearLayout.LayoutParams(-2, dp(this, 30));
                restParams.leftMargin = dp(this, 7);
                stats.addView(rest, restParams);
            }

            TextView action = new TextView(this);
            action.setText(R.string.workout_detail_view_exercise);
            action.setTextColor(UiColors.ORANGE_DARK);
            action.setTextSize(12);
            action.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(this, 36));
            actionParams.topMargin = dp(this, 10);
            action.setPadding(dp(this, 10), 0, dp(this, 10), 0);
            action.setGravity(Gravity.CENTER_VERTICAL);
            action.setBackground(rounded(UiColors.ORANGE_TINT, dp(this, 10)));
            row.addView(action, actionParams);

            card.addView(shell);
            card.setContentDescription(getString(R.string.workout_detail_card_description,
                    i + 1, entry.getExerciseName()));
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, ExerciseDetailActivity.class);
                intent.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE_NAME, entry.getExerciseName());
                startActivity(intent);
            });
            exerciseList.addView(card);
        }
    }

    private void showError(String message) {
        Snackbar.make(findViewById(R.id.workoutDetailRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
