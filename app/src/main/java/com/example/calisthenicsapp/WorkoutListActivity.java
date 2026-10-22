package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.pill;
import static com.example.calisthenicsapp.Views.rounded;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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
        BottomNav.attach(this, R.id.nav_workouts, previewMode);
        new WorkoutService(this).getAllWorkouts(new ServiceCallback<List<Workout>>() {
            @Override public void onSuccess(List<Workout> workouts) {
                allWorkouts.clear();
                allWorkouts.addAll(workouts);
                render();
            }
            @Override public void onError(String message) {
                count.setText(R.string.workout_list_failed);
                Snackbar.make(findViewById(R.id.workoutListRoot), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupFilter() {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.workout_list_all_types));
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
        // The first option shows every type; null means no filter.
        String selected = categoryFilter.getSelectedItemPosition() <= 0 ? null
                : categoryFilter.getSelectedItem().toString();
        int shown = 0;
        for (Workout workout : allWorkouts) {
            if (selected != null && (workout.getCategory() == null
                    || !selected.equals(workout.getCategory().getLabel()))) continue;
            list.addView(createCard(workout));
            shown++;
        }
        count.setText(getResources().getQuantityString(R.plurals.workout_list_count, shown, shown));
        if (shown == 0 && !allWorkouts.isEmpty()) count.setText(R.string.workout_list_empty_category);
    }

    private MaterialCardView createCard(Workout workout) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(this, 218));
        params.bottomMargin = dp(this, 12);
        card.setLayoutParams(params);
        card.setRadius(dp(this, 22)); card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER); card.setStrokeWidth(dp(this, 1));
        card.setCardElevation(dp(this, 1));
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
                new int[]{0x55090B0D, 0x66090B0D, 0xD6090B0D}));
        backdrop.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(this, 16), dp(this, 15), dp(this, 16), dp(this, 15));
        backdrop.addView(content, new FrameLayout.LayoutParams(-1, -1));

        String category = workout.getCategory() == null ? getString(R.string.workout_no_category)
                : workout.getCategory().getLabel();
        String level = workout.getLevel() == null ? getString(R.string.workout_any_level)
                : workout.getLevel().getLabel();
        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow = new TextView(this);
        eyebrow.setText(getString(R.string.workout_list_routine, category.toUpperCase(java.util.Locale.ROOT)));
        eyebrow.setTextColor(UiColors.GRAPHITE);
        eyebrow.setTextSize(10);
        eyebrow.setLetterSpacing(0.08f);
        eyebrow.setTypeface(null, android.graphics.Typeface.BOLD);
        eyebrow.setGravity(Gravity.CENTER_VERTICAL);
        eyebrow.setPadding(dp(this, 12), 0, dp(this, 12), 0);
        eyebrow.setBackground(rounded(0xCC101217, dp(this, 18)));
        topRow.addView(eyebrow, new LinearLayout.LayoutParams(-2, dp(this, 32)));
        content.addView(topRow);

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView title = new TextView(this);
        title.setText(workout.getName());
        title.setTextColor(UiColors.GRAPHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(this, 8);
        content.addView(title, titleParams);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(this, 10);
        content.addView(footer, footerParams);
        footer.addView(pill(this, level, UiColors.ORANGE_TINT, UiColors.ORANGE_DARK));
        String length = Durations.estimate(this, workout.getEstimatedSeconds(), R.string.workout_time_varies);
        TextView duration = pill(this, length, UiColors.SURFACE_ALT, UiColors.MUTED);
        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(-2, dp(this, 30));
        durationParams.leftMargin = dp(this, 7);
        footer.addView(duration, durationParams);
        TextView action = new TextView(this);
        action.setText(R.string.workout_list_view_plan);
        action.setTextColor(UiColors.INK);
        action.setTextSize(12);
        action.setTypeface(null, android.graphics.Typeface.BOLD);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(this, 13), 0, dp(this, 13), 0);
        action.setBackground(rounded(UiColors.ORANGE_DARK, dp(this, 18)));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-2, dp(this, 34));
        actionParams.leftMargin = dp(this, 8);
        footer.addView(action, actionParams);
        action.setContentDescription("View " + workout.getName() + " plan");

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutDetailActivity.class);
            intent.putExtra(EXTRA_WORKOUT_NAME, workout.getName());
            intent.putExtra(EXTRA_PREVIEW_MODE, previewMode);
            startActivity(intent);
        });
        return card;
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
}
