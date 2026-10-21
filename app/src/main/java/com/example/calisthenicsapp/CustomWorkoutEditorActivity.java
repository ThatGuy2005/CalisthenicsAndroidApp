package com.example.calisthenicsapp;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.Service.CustomWorkoutService;
import com.example.calisthenicsapp.Service.ExerciseService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Create or edit a custom routine; ordering is the order used when starting the workout. */
public class CustomWorkoutEditorActivity extends AppCompatActivity {
    public static final String EXTRA_WORKOUT_ID = "custom_workout_id";
    private final List<Exercise> library = new ArrayList<>();
    private final List<Exercise> selected = new ArrayList<>();
    // Linked, so maybeReady lists the saved exercises in their saved order.
    private final Map<String, CustomExerciseOrderEntry> savedSettings = new LinkedHashMap<>();
    private CustomWorkoutService workouts;
    private EditText name, description;
    private LinearLayout selectedList;
    private TextView count, save;
    private View root;
    private String workoutId;
    private boolean libraryReady, savedExercisesReady, loading = true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        workoutId = getIntent().getStringExtra(EXTRA_WORKOUT_ID);
        workouts = new CustomWorkoutService(this);
        buildScreen();
        new ExerciseService(this).getAllExercises(new ServiceCallback<List<Exercise>>() {
            @Override public void onSuccess(List<Exercise> exercises) {
                library.clear(); library.addAll(exercises); libraryReady = true; maybeReady();
            }
            @Override public void onError(String message) { loading = false; save.setEnabled(false); show(message); }
        });
        if (workoutId == null) {
            loading = false; savedExercisesReady = true; maybeReady();
        } else {
            workouts.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
                @Override public void onSuccess(List<CustomWorkout> items) {
                    for (CustomWorkout item : items) if (workoutId.equals(item.getId())) {
                        name.setText(item.getName());
                        description.setText(item.getDescription() == null ? "" : item.getDescription());
                        break;
                    }
                }
                @Override public void onError(String message) { show(message); }
            });
            workouts.getExercises(workoutId, new ServiceCallback<List<CustomExerciseOrderEntry>>() {
                @Override public void onSuccess(List<CustomExerciseOrderEntry> entries) {
                    savedSettings.clear();
                    for (CustomExerciseOrderEntry entry : entries) savedSettings.put(entry.getExerciseName(), entry);
                    savedExercisesReady = true; maybeReady();
                }
                @Override public void onError(String message) { loading = false; save.setEnabled(false); show(message); }
            });
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundResource(R.drawable.page_background); root = scroll;
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(20), dp(22), dp(30)); scroll.addView(page);
        TextView back = text("‹  My workouts", 15, UiColors.ORANGE_DARK, true);
        back.setPadding(dp(2), dp(4), dp(4), dp(4)); back.setOnClickListener(v -> finish()); page.addView(back);
        page.addView(gap(15));
        page.addView(text(workoutId == null ? "BUILD YOUR ROUTINE" : "MAKE IT YOURS", 12,
                UiColors.ORANGE_DARK, true));
        TextView title = text(workoutId == null ? "Create workout" : "Edit workout", 29, UiColors.GRAPHITE, true);
        LinearLayout.LayoutParams titleParams = matchWrap(); titleParams.topMargin = dp(5); page.addView(title, titleParams);
        TextView intro = text("Give your session a name, then choose and arrange its exercises.",
                14, UiColors.MUTED, false);
        LinearLayout.LayoutParams introParams = matchWrap(); introParams.topMargin = dp(4); page.addView(intro, introParams);

        page.addView(sectionLabel("WORKOUT DETAILS", 22));
        name = field("Workout name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        page.addView(name, matchWrap());
        description = field("Description (optional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        description.setMinLines(2); description.setMaxLines(4);
        LinearLayout.LayoutParams descParams = matchWrap(); descParams.topMargin = dp(10); page.addView(description, descParams);

        page.addView(sectionLabel("EXERCISES", 22));
        count = text("Choose at least one exercise", 13, UiColors.MUTED, true);
        page.addView(count);
        Button choose = button("＋   Add exercises", false);
        LinearLayout.LayoutParams chooseParams = matchWrap(); chooseParams.topMargin = dp(10); page.addView(choose, chooseParams);
        choose.setOnClickListener(v -> chooseExercises());
        selectedList = new LinearLayout(this); selectedList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams selectedParams = matchWrap(); selectedParams.topMargin = dp(12);
        page.addView(selectedList, selectedParams);

        save = text(workoutId == null ? "Create workout" : "Save changes", 15, UiColors.INK, true);
        save.setGravity(Gravity.CENTER); save.setBackground(rounded(UiColors.ORANGE_DARK, dp(16)));
        save.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams saveParams = matchWrap(); saveParams.topMargin = dp(22); page.addView(save, saveParams);
        save.setOnClickListener(v -> saveWorkout());
        setContentView(scroll);
    }

    private void maybeReady() {
        if (!libraryReady || !savedExercisesReady) return;
        loading = false;
        if (workoutId != null) {
            for (CustomExerciseOrderEntry entry : savedSettings.values()) {
                for (Exercise exercise : library) if (exercise.getName().equals(entry.getExerciseName())) {
                    selected.add(exercise); break;
                }
            }
        }
        renderSelected();
    }

    private void chooseExercises() {
        if (loading) { show("Loading exercise library…"); return; }
        String[] labels = new String[library.size()]; boolean[] checked = new boolean[library.size()];
        for (int i = 0; i < library.size(); i++) {
            Exercise e = library.get(i); labels[i] = e.getName() + "  ·  " + e.getMuscleGroup();
            checked[i] = contains(e.getName());
        }
        List<String> before = new ArrayList<>(); for (Exercise e : selected) before.add(e.getName());
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Select exercises")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    Exercise exercise = library.get(which);
                    if (isChecked && !contains(exercise.getName())) selected.add(exercise);
                    else if (!isChecked) remove(exercise.getName());
                }).setNegativeButton("Cancel", (dialog, which) -> {
                    selected.clear();
                    for (String exerciseName : before) for (Exercise exercise : library)
                        if (exercise.getName().equals(exerciseName)) { selected.add(exercise); break; }
                    renderSelected();
                }).setPositiveButton("Done", (dialog, which) -> renderSelected()).show();
    }

    private void renderSelected() {
        if (selectedList == null) return;
        selectedList.removeAllViews();
        count.setText(selected.size() == 1 ? "1 exercise · drag the order with arrows" :
                selected.size() + " exercises · arrange the order with arrows");
        for (int i = 0; i < selected.size(); i++) {
            final int index = i; Exercise exercise = selected.get(i);
            MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(16));
            card.setCardBackgroundColor(UiColors.SURFACE); card.setStrokeColor(UiColors.BORDER);
            card.setStrokeWidth(dp(1)); card.setCardElevation(0);
            LinearLayout.LayoutParams cp = matchWrap(); cp.bottomMargin = dp(8); card.setLayoutParams(cp);
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(8), dp(10)); card.addView(row);
            TextView number = text(String.format(java.util.Locale.ROOT, "%02d", i + 1), 13,
                    UiColors.ORANGE_DARK, true); number.setGravity(Gravity.CENTER);
            row.addView(number, new LinearLayout.LayoutParams(dp(34), dp(34)));
            LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, -2, 1f);
            labelsParams.leftMargin = dp(9); row.addView(labels, labelsParams);
            labels.addView(text(exercise.getName(), 14, UiColors.GRAPHITE, true));
            String target = exercise.isTimed() ? exercise.getDefaultSeconds() + " sec" : exercise.getDefaultReps() + " reps";
            TextView meta = text((exercise.getMuscleGroup() == null ? "Exercise" : exercise.getMuscleGroup())
                    + "  ·  " + target + " default", 11, UiColors.MUTED, false);
            LinearLayout.LayoutParams mp = matchWrap(); mp.topMargin = dp(2); labels.addView(meta, mp);
            TextView up = mini("↑", index > 0); row.addView(up, miniParams());
            TextView down = mini("↓", index < selected.size() - 1); LinearLayout.LayoutParams downParams = miniParams();
            downParams.leftMargin = dp(4); row.addView(down, downParams);
            TextView remove = mini("×", true); LinearLayout.LayoutParams removeParams = miniParams();
            removeParams.leftMargin = dp(4); row.addView(remove, removeParams);
            up.setOnClickListener(v -> move(index, -1)); down.setOnClickListener(v -> move(index, 1));
            remove.setOnClickListener(v -> { selected.remove(index); renderSelected(); });
            selectedList.addView(card);
        }
    }

    private void move(int index, int offset) {
        int target = index + offset; if (target < 0 || target >= selected.size()) return;
        Exercise item = selected.remove(index); selected.add(target, item); renderSelected();
    }

    private void saveWorkout() {
        String workoutName = name.getText().toString().trim();
        if (workoutName.isEmpty()) { name.setError("Add a name for this workout"); name.requestFocus(); return; }
        if (selected.isEmpty()) { show("Add at least one exercise to your workout."); return; }
        save.setEnabled(false); save.setText("Saving…");
        List<CustomExerciseOrderEntry> entries = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            Exercise exercise = selected.get(i); CustomExerciseOrderEntry old = savedSettings.get(exercise.getName());
            entries.add(new CustomExerciseOrderEntry(workoutId, exercise.getName(), i + 1,
                    old == null ? null : old.getSets(), old == null ? null : old.getReps(),
                    old == null ? null : old.getSeconds(), old == null ? null : old.getRestSeconds(),
                    old == null ? null : old.getRestAfterSeconds()));
        }
        if (workoutId == null) {
            workouts.create(workoutName, description.getText().toString(), entries, new ServiceCallback<String>() {
                @Override public void onSuccess(String id) { show("Workout created."); finish(); }
                @Override public void onError(String message) { restoreSave(message); }
            });
        } else {
            saveEdited(workoutName, description.getText().toString(), entries);
        }
    }

    private void saveEdited(String newName, String newDescription, List<CustomExerciseOrderEntry> entries) {
        workouts.rename(workoutId, newName, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) {
                workouts.setDescription(workoutId, newDescription, new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void result) {
                        workouts.replaceExercises(workoutId, entries, new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void result) { show("Workout updated."); finish(); }
                            @Override public void onError(String message) { restoreSave(message); }
                        });
                    }
                    @Override public void onError(String message) { restoreSave(message); }
                });
            }
            @Override public void onError(String message) { restoreSave(message); }
        });
    }

    private void restoreSave(String message) { save.setEnabled(true); save.setText(workoutId == null ? "Create workout" : "Save changes"); show(message); }
    private boolean contains(String name) { for (Exercise e : selected) if (e.getName().equals(name)) return true; return false; }
    private void remove(String name) { for (int i = selected.size() - 1; i >= 0; i--) if (selected.get(i).getName().equals(name)) selected.remove(i); }
    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this); e.setSingleLine(inputType != (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setHint(hint); e.setTextSize(14); e.setTextColor(UiColors.GRAPHITE); e.setHintTextColor(UiColors.MUTED);
        e.setInputType(inputType); e.setPadding(dp(14), dp(10), dp(14), dp(10)); e.setBackground(rounded(UiColors.SURFACE, dp(14)));
        return e;
    }
    private TextView sectionLabel(String value, int margin) { TextView t = text(value, 12, UiColors.MUTED, true); t.setLetterSpacing(.08f); LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(margin); p.bottomMargin = dp(9); t.setLayoutParams(p); return t; }
    private Button button(String label, boolean primary) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(primary ? UiColors.INK : UiColors.ORANGE_DARK); b.setBackground(rounded(primary ? UiColors.ORANGE_DARK : UiColors.SURFACE, dp(14))); return b; }
    private TextView mini(String label, boolean enabled) { TextView t = text(label, 16, enabled ? UiColors.ORANGE_DARK : UiColors.MUTED, true); t.setGravity(Gravity.CENTER); t.setBackground(rounded(UiColors.SURFACE_ALT, dp(9))); t.setEnabled(enabled); return t; }
    private LinearLayout.LayoutParams miniParams() { return new LinearLayout.LayoutParams(dp(30), dp(30)); }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(null, Typeface.BOLD); return t; }
    private View gap(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); return v; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private void show(String message) { Snackbar.make(root, message, Snackbar.LENGTH_LONG).show(); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
