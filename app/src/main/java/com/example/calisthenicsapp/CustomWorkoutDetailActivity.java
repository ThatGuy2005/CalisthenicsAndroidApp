package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.text;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.Service.CustomWorkoutService;
import com.example.calisthenicsapp.Service.ExerciseService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutSessionService;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read-only overview of a custom workout, used from both its library and the schedule. */
public class CustomWorkoutDetailActivity extends AppCompatActivity {
    public static final String EXTRA_WORKOUT_ID = "custom_workout_id";
    private String workoutId;
    private TextView name, meta, description, count, start;
    private LinearLayout exercisesView;
    private List<CustomExerciseOrderEntry> entries = new ArrayList<>();
    private final Map<String, Exercise> exerciseByName = new HashMap<>();
    private boolean workoutLoaded, entriesLoaded, libraryLoaded;
    private CustomWorkout workout;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_custom_workout_detail);
        workoutId = getIntent().getStringExtra(EXTRA_WORKOUT_ID);
        name = findViewById(R.id.customWorkoutDetailName);
        meta = findViewById(R.id.customWorkoutDetailMeta);
        description = findViewById(R.id.customWorkoutDetailDescription);
        count = findViewById(R.id.customWorkoutDetailCount);
        exercisesView = findViewById(R.id.customWorkoutDetailExercises);
        start = findViewById(R.id.customWorkoutStart);
        findViewById(R.id.customWorkoutBack).setOnClickListener(v -> finish());
        start.setOnClickListener(v -> startWorkout());
        if (workoutId == null) { show(getString(R.string.workout_detail_not_found)); finish(); return; }

        CustomWorkoutService service = new CustomWorkoutService(this);
        service.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
            @Override public void onSuccess(List<CustomWorkout> result) {
                for (CustomWorkout item : result) if (workoutId.equals(item.getId())) { workout = item; break; }
                workoutLoaded = true; renderWhenReady();
            }
            @Override public void onError(String message) { show(message); finish(); }
        });
        service.getExercises(workoutId, new ServiceCallback<List<CustomExerciseOrderEntry>>() {
            @Override public void onSuccess(List<CustomExerciseOrderEntry> result) {
                entries = result; entriesLoaded = true; renderWhenReady();
            }
            @Override public void onError(String message) { show(message); }
        });
        new ExerciseService(this).getAllExercises(new ServiceCallback<List<Exercise>>() {
            @Override public void onSuccess(List<Exercise> result) {
                for (Exercise item : result) exerciseByName.put(item.getName(), item);
                libraryLoaded = true; renderWhenReady();
            }
            @Override public void onError(String message) { libraryLoaded = true; renderWhenReady(); }
        });
    }

    private void renderWhenReady() {
        if (!workoutLoaded || !entriesLoaded || !libraryLoaded) return;
        if (workout == null) { show(getString(R.string.custom_detail_gone)); finish(); return; }
        name.setText(workout.getName());
        meta.setText(getResources().getQuantityString(R.plurals.custom_detail_meta, entries.size(),
                entries.size(), Durations.estimate(this, workout.getEstimatedSeconds(),
                        R.string.custom_workouts_no_length)));
        description.setText(workout.getDescription() == null || workout.getDescription().trim().isEmpty()
                ? getString(R.string.custom_detail_no_description) : workout.getDescription());
        count.setText(getString(R.string.workout_detail_exercise_count, entries.size()));
        exercisesView.removeAllViews();
        for (int i = 0; i < entries.size(); i++) addExercise(i, entries.get(i));
        start.setEnabled(!entries.isEmpty());
    }

    private void addExercise(int index, CustomExerciseOrderEntry entry) {
        Exercise exercise = exerciseByName.get(entry.getExerciseName());
        boolean timed = exercise != null && exercise.isTimed();
        int sets = entry.getSets() == null ? 3 : entry.getSets();
        Integer target = timed
                ? entry.getSeconds() == null ? exercise.getDefaultSeconds() : entry.getSeconds()
                : entry.getReps() == null ? exercise == null ? null : exercise.getDefaultReps() : entry.getReps();
        int rest = entry.getRestSeconds() == null ? 60 : entry.getRestSeconds();
        String targetText = target == null ? getString(timed ? R.string.target_timed : R.string.target_reps_unknown)
                : timed ? getString(R.string.target_seconds, target)
                : getResources().getQuantityString(R.plurals.target_reps, target, target);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(this, 16)); card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER); card.setStrokeWidth(dp(this, 1)); card.setCardElevation(0);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(this, 9); card.setLayoutParams(cardParams);
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(this, 14), dp(this, 13), dp(this, 14), dp(this, 13)); card.addView(row);
        TextView number = text(this, String.format(java.util.Locale.ROOT, "%02d", index + 1), 13,
                UiColors.ORANGE_DARK, true);
        number.setGravity(Gravity.CENTER); row.addView(number, new LinearLayout.LayoutParams(dp(this, 34), dp(this, 34)));
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.leftMargin = dp(this, 10); row.addView(labels, lp);
        labels.addView(text(this, entry.getExerciseName(), 15, UiColors.GRAPHITE, true));
        TextView detail = text(this, getString(R.string.target_sets_target_rest,
                getResources().getQuantityString(R.plurals.target_sets, sets, sets), targetText,
                getString(R.string.target_rest, rest)), 12, UiColors.MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(this, 4); labels.addView(detail, detailParams);
        card.setClickable(true); card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExerciseDetailActivity.class);
            intent.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE_NAME, entry.getExerciseName());
            startActivity(intent);
        });
        exercisesView.addView(card);
    }

    private void startWorkout() {
        start.setEnabled(false); start.setText(R.string.custom_detail_preparing);
        new WorkoutSessionService(this).startCustom(workoutId, new ServiceCallback<WorkoutSession>() {
            @Override public void onSuccess(WorkoutSession session) {
                Intent intent = new Intent(CustomWorkoutDetailActivity.this, WorkoutSessionActivity.class);
                intent.putExtra(WorkoutSessionActivity.EXTRA_SESSION, session); startActivity(intent);
                start.setEnabled(true); start.setText(R.string.workout_detail_start);
            }
            @Override public void onError(String message) {
                start.setEnabled(true); start.setText(R.string.workout_detail_start); show(message);
            }
        });
    }

    private void show(String message) { Snackbar.make(findViewById(R.id.customWorkoutDetailRoot), message, Snackbar.LENGTH_LONG).show(); }
}
