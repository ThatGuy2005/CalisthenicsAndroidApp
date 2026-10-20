package com.example.calisthenicsapp;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.CoreComponents.WorkoutCategory;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class WorkoutListActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = HomeActivity.EXTRA_PREVIEW_MODE;
    public static final String EXTRA_WORKOUT_NAME = "com.example.calisthenicsapp.WORKOUT_NAME";
    private final List<Workout> allWorkouts = new ArrayList<>();
    private LinearLayout list;
    private TextView count;
    private Spinner categoryFilter;
    private boolean previewMode;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);
        previewMode = getIntent().getBooleanExtra(EXTRA_PREVIEW_MODE, false);
        list = findViewById(R.id.workoutList);
        count = findViewById(R.id.workoutCount);
        categoryFilter = findViewById(R.id.workoutCategoryFilter);
        findViewById(R.id.myWorkoutsButton).setOnClickListener(v ->
                startActivity(new Intent(this, CustomWorkoutsActivity.class)));
        setupFilter();
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_workouts);
        nav.setOnItemSelectedListener(this::onNavigationSelected);
        new WorkoutService(this).getAllWorkouts(new ServiceCallback<List<Workout>>() {
            @Override public void onSuccess(List<Workout> workouts) {
                allWorkouts.clear();
                allWorkouts.addAll(workouts);
                render();
            }
            @Override public void onError(String message) {
                count.setText("Workouts couldn't be loaded.");
                Snackbar.make(findViewById(R.id.workoutListRoot), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupFilter() {
        List<String> options = new ArrayList<>();
        options.add("All workout types");
        for (WorkoutCategory category : WorkoutCategory.values()) options.add(category.getLabel());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, options);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        categoryFilter.setAdapter(adapter);
        categoryFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                                int position, long id) { render(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void render() {
        if (list == null) return;
        list.removeAllViews();
        String selected = categoryFilter.getSelectedItem() == null ? "All workout types"
                : categoryFilter.getSelectedItem().toString();
        int shown = 0;
        for (Workout workout : allWorkouts) {
            if (!"All workout types".equals(selected) && (workout.getCategory() == null
                    || !selected.equals(workout.getCategory().getLabel()))) continue;
            list.addView(createCard(workout));
            shown++;
        }
        count.setText(shown == 1 ? "1 workout" : shown + " workouts");
        if (shown == 0 && !allWorkouts.isEmpty()) count.setText("No workouts in this category.");
    }

    private MaterialCardView createCard(Workout workout) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(218));
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        card.setRadius(dp(22)); card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER); card.setStrokeWidth(dp(1));
        card.setCardElevation(dp(1));
        card.setClickable(true); card.setFocusable(true);
        card.setClipToOutline(true);

        FrameLayout backdrop = new FrameLayout(this);
        card.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));

        ImageView image = new ImageView(this);
        image.setImageResource(workoutImage(workout));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.addView(image, new FrameLayout.LayoutParams(-1, -1));

        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x88090B0D, 0x9E090B0D, 0xF2090B0D}));
        backdrop.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(15), dp(16), dp(15));
        backdrop.addView(content, new FrameLayout.LayoutParams(-1, -1));

        String category = workout.getCategory() == null ? "Workout" : workout.getCategory().getLabel();
        String level = workout.getLevel() == null ? "All levels" : workout.getLevel().getLabel();
        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView categoryBadge = new TextView(this);
        categoryBadge.setText(categoryMark(category));
        categoryBadge.setTextColor(workoutAccent(workout));
        categoryBadge.setTextSize(12);
        categoryBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryBadge.setGravity(Gravity.CENTER);
        categoryBadge.setBackground(rounded(workoutTint(workout), dp(13)));
        topRow.addView(categoryBadge, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView eyebrow = new TextView(this);
        eyebrow.setText(category.toUpperCase(java.util.Locale.ROOT) + " ROUTINE");
        eyebrow.setTextColor(UiColors.MUTED);
        eyebrow.setTextSize(10);
        eyebrow.setLetterSpacing(0.07f);
        eyebrow.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams eyebrowParams = new LinearLayout.LayoutParams(0, -2, 1f);
        eyebrowParams.leftMargin = dp(10);
        topRow.addView(eyebrow, eyebrowParams);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(UiColors.ORANGE_DARK);
        arrow.setTextSize(25);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(rounded(UiColors.ORANGE_TINT, dp(30)));
        topRow.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(32)));
        content.addView(topRow);

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView title = new TextView(this);
        title.setText(workout.getName());
        title.setTextColor(UiColors.GRAPHITE);
        title.setTextSize(19);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        content.addView(title, titleParams);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(10);
        content.addView(footer, footerParams);
        footer.addView(pill(level, UiColors.ORANGE_TINT, UiColors.ORANGE_DARK));
        TextView duration = pill(formatDuration(workout.getEstimatedSeconds()), UiColors.SURFACE_ALT, UiColors.MUTED);
        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(-2, dp(30));
        durationParams.leftMargin = dp(7);
        footer.addView(duration, durationParams);
        TextView action = new TextView(this);
        action.setText("View plan  →");
        action.setTextColor(UiColors.ORANGE_DARK);
        action.setTextSize(12);
        action.setTypeface(null, android.graphics.Typeface.BOLD);
        action.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        footer.addView(action, new LinearLayout.LayoutParams(0, -2, 1f));

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutDetailActivity.class);
            intent.putExtra(EXTRA_WORKOUT_NAME, workout.getName());
            intent.putExtra(EXTRA_PREVIEW_MODE, previewMode);
            startActivity(intent);
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

    private int workoutAccent(Workout workout) {
        return UiColors.ORANGE_DARK;
    }

    private int workoutImage(Workout workout) {
        String name = workout.getName();
        if ("Advanced Calisthenics".equals(name)) return R.drawable.workout_01;
        if ("Beginner Full Body".equals(name)) return R.drawable.workout_02;
        if ("Cardio Blast".equals(name)) return R.drawable.workout_03;
        if ("Core Basics".equals(name)) return R.drawable.workout_04;
        if ("Lower Body Burn".equals(name)) return R.drawable.workout_05;
        if ("Upper Body Strength".equals(name)) return R.drawable.workout_06;
        return R.drawable.workout_02;
    }

    private int workoutTint(Workout workout) {
        return UiColors.ORANGE_TINT;
    }

    private String categoryMark(String category) {
        String[] words = category.trim().split("\\s+");
        if (words.length > 1) return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase(java.util.Locale.ROOT);
        return category.length() < 2 ? category.toUpperCase(java.util.Locale.ROOT)
                : category.substring(0, 2).toUpperCase(java.util.Locale.ROOT);
    }

    private boolean onNavigationSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_workouts) return true;
        if (item.getItemId() == R.id.nav_exercises) {
            Intent i = new Intent(this, ExerciseListActivity.class);
            i.putExtra(ExerciseListActivity.EXTRA_PREVIEW_MODE, previewMode); startActivity(i); finish(); return true;
        }
        if (item.getItemId() == R.id.nav_home) {
            Intent i = new Intent(this, HomeActivity.class);
            i.putExtra(HomeActivity.EXTRA_PREVIEW_MODE, previewMode); startActivity(i); finish(); return true;
        }
        if (item.getItemId() == R.id.nav_schedule) {
            Intent i = new Intent(this, ScheduleActivity.class);
            i.putExtra(ScheduleActivity.EXTRA_PREVIEW_MODE, previewMode); startActivity(i); finish(); return true;
        }
        if (item.getItemId() == R.id.nav_history) {
            Intent i = new Intent(this, HistoryActivity.class);
            i.putExtra(HistoryActivity.EXTRA_PREVIEW_MODE, previewMode); startActivity(i); finish(); return true;
        }
        Snackbar.make(findViewById(R.id.workoutListRoot), "This section is coming next.", Snackbar.LENGTH_SHORT).show();
        return false;
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) return "time varies";
        int minutes = Math.max(1, seconds / 60);
        return minutes + " min";
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
