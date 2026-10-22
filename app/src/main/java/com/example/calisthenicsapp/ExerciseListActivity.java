package com.example.calisthenicsapp;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.Gravity;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.Difficulty;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.Service.ExerciseService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ExerciseListActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = HomeActivity.EXTRA_PREVIEW_MODE;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearch = this::loadExercises;
    private ExerciseService exerciseService;
    private EditText searchInput;
    private Spinner muscleGroupFilter;
    private Spinner difficultyFilter;
    private LinearLayout exerciseList;
    private TextView exerciseCount;
    private boolean filtersReady;
    private boolean previewMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_list);

        previewMode = getIntent().getBooleanExtra(EXTRA_PREVIEW_MODE, false);
        exerciseService = new ExerciseService(this);
        searchInput = findViewById(R.id.exerciseSearch);
        muscleGroupFilter = findViewById(R.id.muscleGroupFilter);
        difficultyFilter = findViewById(R.id.difficultyFilter);
        exerciseList = findViewById(R.id.exerciseList);
        exerciseCount = findViewById(R.id.exerciseCount);

        setupBottomNavigation();
        setupDifficultyFilter();
        setupSearch();
        loadMuscleGroups();
    }

    private void setupBottomNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(R.id.nav_exercises);
        navigation.setOnItemSelectedListener(this::onNavigationSelected);
    }

    private boolean onNavigationSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_exercises) return true;
        if (item.getItemId() == R.id.nav_home) {
            Intent home = new Intent(this, HomeActivity.class);
            home.putExtra(HomeActivity.EXTRA_PREVIEW_MODE, previewMode);
            startActivity(home);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.nav_workouts) {
            Intent workouts = new Intent(this, WorkoutListActivity.class);
            workouts.putExtra(WorkoutListActivity.EXTRA_PREVIEW_MODE, previewMode);
            startActivity(workouts);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.nav_schedule) {
            Intent schedule = new Intent(this, ScheduleActivity.class);
            schedule.putExtra(ScheduleActivity.EXTRA_PREVIEW_MODE, previewMode);
            startActivity(schedule);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.nav_history) {
            Intent history = new Intent(this, HistoryActivity.class);
            history.putExtra(HistoryActivity.EXTRA_PREVIEW_MODE, previewMode);
            startActivity(history);
            finish();
            return true;
        }
        Snackbar.make(findViewById(R.id.exerciseListRoot),
                R.string.nav_coming_next, Snackbar.LENGTH_SHORT).show();
        return false;
    }

    private void setupDifficultyFilter() {
        List<String> levels = new ArrayList<>();
        levels.add(getString(R.string.exercise_list_all_levels));
        for (Difficulty difficulty : Difficulty.values()) levels.add(difficulty.getLabel());
        difficultyFilter.setAdapter(new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, levels) {{
            setDropDownViewResource(R.layout.spinner_item_dropdown);
        }});
        difficultyFilter.setOnItemSelectedListener(new SimpleSelectionListener(() -> scheduleSearch()));
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadMuscleGroups() {
        exerciseService.getMuscleGroups(new ServiceCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> groups) {
                List<String> options = new ArrayList<>();
                options.add(getString(R.string.exercise_list_all_groups));
                options.addAll(groups);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ExerciseListActivity.this,
                        R.layout.spinner_item_selected, options);
                adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
                muscleGroupFilter.setAdapter(adapter);
                muscleGroupFilter.setOnItemSelectedListener(
                        new SimpleSelectionListener(ExerciseListActivity.this::scheduleSearch));
                filtersReady = true;
                loadExercises();
            }

            @Override
            public void onError(String message) {
                exerciseCount.setText(R.string.exercise_list_filters_failed);
                Snackbar.make(findViewById(R.id.exerciseListRoot), message,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void scheduleSearch() {
        if (!filtersReady) return;
        searchHandler.removeCallbacks(pendingSearch);
        searchHandler.postDelayed(pendingSearch, 180);
    }

    private void loadExercises() {
        if (!filtersReady) return;
        // The first option of each filter is "All ...", which means no filter.
        String group = muscleGroupFilter.getSelectedItemPosition() <= 0
                ? null : String.valueOf(muscleGroupFilter.getSelectedItem());
        Difficulty difficulty = difficultyFilter.getSelectedItemPosition() <= 0
                ? null : Difficulty.fromLabel(String.valueOf(difficultyFilter.getSelectedItem()));

        exerciseService.searchExercises(searchInput.getText().toString(), group, difficulty,
                null, new ServiceCallback<List<Exercise>>() {
                    @Override
                    public void onSuccess(List<Exercise> exercises) {
                        renderExercises(exercises);
                    }

                    @Override
                    public void onError(String message) {
                        exerciseCount.setText(R.string.exercise_list_failed);
                        exerciseList.removeAllViews();
                        Snackbar.make(findViewById(R.id.exerciseListRoot), message,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void renderExercises(List<Exercise> exercises) {
        exerciseList.removeAllViews();
        exerciseCount.setText(getResources().getQuantityString(R.plurals.exercise_list_count,
                exercises.size(), exercises.size()));
        if (exercises.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.exercise_list_no_match);
            empty.setTextColor(UiColors.MUTED);
            empty.setTextSize(15);
            empty.setPadding(4, 20, 4, 20);
            exerciseList.addView(empty);
            return;
        }
        for (Exercise exercise : exercises) exerciseList.addView(createExerciseCard(exercise));
    }

    private MaterialCardView createExerciseCard(Exercise exercise) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(22));
        card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(1));
        card.setCardElevation(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        View accent = new View(this);
        accent.setBackgroundColor(difficultyAccent(exercise.getDifficulty()));
        shell.addView(accent, new LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(15), dp(14));
        shell.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = new TextView(this);
        badge.setText(muscleMark(exercise.getMuscleGroup()));
        badge.setTextColor(muscleAccent(exercise.getMuscleGroup()));
        badge.setTextSize(12);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(muscleTint(exercise.getMuscleGroup()), dp(13)));
        topRow.addView(badge, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView group = new TextView(this);
        group.setText(exercise.getMuscleGroup().toUpperCase(java.util.Locale.ROOT));
        group.setTextColor(UiColors.MUTED);
        group.setTextSize(10);
        group.setLetterSpacing(0.08f);
        group.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(0, -2, 1f);
        groupParams.leftMargin = dp(10);
        topRow.addView(group, groupParams);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(UiColors.ORANGE_DARK);
        arrow.setTextSize(25);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(rounded(UiColors.ORANGE_TINT, dp(30)));
        topRow.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(32)));
        content.addView(topRow);

        TextView name = new TextView(this);
        name.setText(exercise.getName());
        name.setTextColor(UiColors.GRAPHITE);
        name.setTextSize(18);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(10);
        content.addView(name, nameParams);

        Integer amount = exercise.isTimed() ? exercise.getDefaultSeconds() : exercise.getDefaultReps();
        String target = amount == null
                ? getString(exercise.isTimed() ? R.string.target_timed : R.string.target_reps_unknown)
                : exercise.isTimed() ? getString(R.string.target_seconds, amount)
                : getResources().getQuantityString(R.plurals.target_reps, amount, amount);
        if (exercise.isPerSide()) target = getString(R.string.target_per_side_short, target);
        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(12);
        content.addView(footer, footerParams);

        TextView level = pill(exercise.getDifficulty().getLabel(), UiColors.SURFACE_ALT, UiColors.MUTED);
        footer.addView(level);
        TextView targetPill = pill(target, UiColors.ORANGE_TINT, UiColors.ORANGE_DARK);
        LinearLayout.LayoutParams targetParams = new LinearLayout.LayoutParams(-2, dp(30));
        targetParams.leftMargin = dp(7);
        footer.addView(targetPill, targetParams);
        TextView action = new TextView(this);
        action.setText(R.string.exercise_list_form);
        action.setTextColor(UiColors.ORANGE_DARK);
        action.setTextSize(12);
        action.setTypeface(null, android.graphics.Typeface.BOLD);
        action.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        footer.addView(action, new LinearLayout.LayoutParams(0, -2, 1f));

        card.addView(shell);
        card.setOnClickListener(view -> {
            Intent detail = new Intent(this, ExerciseDetailActivity.class);
            detail.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE_NAME, exercise.getName());
            startActivity(detail);
        });
        return card;
    }

    private TextView pill(String text, int backgroundColor, int textColor) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(textColor);
        view.setTextSize(11);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), 0, dp(10), 0);
        view.setBackground(rounded(backgroundColor, dp(20)));
        view.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(30)));
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int muscleAccent(String group) {
        return UiColors.ORANGE_DARK;
    }

    private int difficultyAccent(Difficulty difficulty) {
        if (difficulty == null) return UiColors.ORANGE_DARK;
        switch (difficulty) {
            case BEGINNER:
                return UiColors.DIFFICULTY_BEGINNER;
            case INTERMEDIATE:
                return UiColors.DIFFICULTY_INTERMEDIATE;
            case ADVANCED:
                return UiColors.DIFFICULTY_ADVANCED;
            default:
                return UiColors.ORANGE_DARK;
        }
    }

    private int muscleTint(String group) {
        return UiColors.ORANGE_TINT;
    }

    private String muscleMark(String group) {
        if (group == null || group.trim().isEmpty()) return getString(R.string.exercise_list_mark_none);
        String[] words = group.trim().split("\\s+");
        if (words.length > 1) return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase(java.util.Locale.ROOT);
        return group.length() < 2 ? group.toUpperCase(java.util.Locale.ROOT)
                : group.substring(0, 2).toUpperCase(java.util.Locale.ROOT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private static class SimpleSelectionListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable onSelected;

        SimpleSelectionListener(Runnable onSelected) { this.onSelected = onSelected; }

        @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                             android.view.View view, int position, long id) {
            onSelected.run();
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }
}
