package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.rounded;
import static com.example.calisthenicsapp.Views.text;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        page.setPadding(dp(this, 22), dp(this, 20), dp(this, 22), dp(this, 30)); scroll.addView(page);
        TextView back = text(this, getString(R.string.custom_editor_back), 15, UiColors.ORANGE_DARK, true);
        back.setPadding(dp(this, 2), dp(this, 4), dp(this, 4), dp(this, 4)); back.setOnClickListener(v -> finish()); page.addView(back);
        page.addView(gap(15));
        page.addView(text(this, getString(workoutId == null ? R.string.custom_editor_eyebrow_new : R.string.custom_editor_eyebrow_edit), 12,
                UiColors.ORANGE_DARK, true));
        TextView title = text(this, getString(workoutId == null ? R.string.custom_editor_create : R.string.custom_editor_title_edit), 29, UiColors.GRAPHITE, true);
        LinearLayout.LayoutParams titleParams = matchWrap(); titleParams.topMargin = dp(this, 5); page.addView(title, titleParams);
        TextView intro = text(this, getString(R.string.custom_editor_intro),
                14, UiColors.MUTED, false);
        LinearLayout.LayoutParams introParams = matchWrap(); introParams.topMargin = dp(this, 4); page.addView(intro, introParams);

        page.addView(sectionLabel(getString(R.string.custom_editor_details_label), 22));
        name = field(getString(R.string.custom_editor_hint_name), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        page.addView(name, matchWrap());
        description = field(getString(R.string.custom_editor_hint_description), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        description.setMinLines(2); description.setMaxLines(4);
        LinearLayout.LayoutParams descParams = matchWrap(); descParams.topMargin = dp(this, 10); page.addView(description, descParams);

        page.addView(sectionLabel(getString(R.string.custom_editor_exercises_label), 22));
        count = text(this, getString(R.string.custom_editor_count_empty), 13, UiColors.MUTED, true);
        page.addView(count);
        Button choose = button(getString(R.string.custom_editor_add_exercises), false);
        LinearLayout.LayoutParams chooseParams = matchWrap(); chooseParams.topMargin = dp(this, 10); page.addView(choose, chooseParams);
        choose.setOnClickListener(v -> chooseExercises());
        selectedList = new LinearLayout(this); selectedList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams selectedParams = matchWrap(); selectedParams.topMargin = dp(this, 12);
        page.addView(selectedList, selectedParams);

        save = text(this, getString(saveLabel()), 15, UiColors.INK, true);
        save.setGravity(Gravity.CENTER); save.setBackground(rounded(UiColors.ORANGE_DARK, dp(this, 16)));
        save.setPadding(dp(this, 16), dp(this, 14), dp(this, 16), dp(this, 14));
        LinearLayout.LayoutParams saveParams = matchWrap(); saveParams.topMargin = dp(this, 22); page.addView(save, saveParams);
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
        if (loading) { show(getString(R.string.custom_editor_loading_library)); return; }
        String[] labels = new String[library.size()]; boolean[] checked = new boolean[library.size()];
        for (int i = 0; i < library.size(); i++) {
            Exercise e = library.get(i); labels[i] = e.getName() + "  ·  " + e.getMuscleGroup();
            checked[i] = contains(e.getName());
        }
        List<String> before = new ArrayList<>(); for (Exercise e : selected) before.add(e.getName());
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.custom_editor_select_title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    Exercise exercise = library.get(which);
                    if (isChecked && !contains(exercise.getName())) selected.add(exercise);
                    else if (!isChecked) remove(exercise.getName());
                }).setNegativeButton(R.string.common_cancel, (dialog, which) -> {
                    selected.clear();
                    for (String exerciseName : before) for (Exercise exercise : library)
                        if (exercise.getName().equals(exerciseName)) { selected.add(exercise); break; }
                    renderSelected();
                }).setPositiveButton(R.string.common_done, (dialog, which) -> renderSelected()).show();
    }

    private void renderSelected() {
        if (selectedList == null) return;
        selectedList.removeAllViews();
        count.setText(selected.isEmpty() ? getString(R.string.custom_editor_count_empty)
                : getResources().getQuantityString(R.plurals.custom_editor_count,
                        selected.size(), selected.size()));
        for (int i = 0; i < selected.size(); i++) {
            final int index = i; Exercise exercise = selected.get(i);
            MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(this, 16));
            card.setCardBackgroundColor(UiColors.SURFACE); card.setStrokeColor(UiColors.BORDER);
            card.setStrokeWidth(dp(this, 1)); card.setCardElevation(0);
            LinearLayout.LayoutParams cp = matchWrap(); cp.bottomMargin = dp(this, 8); card.setLayoutParams(cp);
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(this, 12), dp(this, 10), dp(this, 8), dp(this, 10)); card.addView(row);
            TextView number = text(this, String.format(java.util.Locale.ROOT, "%02d", i + 1), 13,
                    UiColors.ORANGE_DARK, true); number.setGravity(Gravity.CENTER);
            row.addView(number, new LinearLayout.LayoutParams(dp(this, 34), dp(this, 34)));
            LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, -2, 1f);
            labelsParams.leftMargin = dp(this, 9); row.addView(labels, labelsParams);
            labels.addView(text(this, exercise.getName(), 14, UiColors.GRAPHITE, true));
            TextView meta = text(this, settingsOf(exercise), 11, UiColors.MUTED, false);
            LinearLayout.LayoutParams mp = matchWrap(); mp.topMargin = dp(this, 2); labels.addView(meta, mp);
            meta.setOnClickListener(v -> editSettings(exercise));
            meta.setContentDescription(getString(R.string.custom_editor_settings_description, exercise.getName()));
            TextView edit = mini("⚙", true); LinearLayout.LayoutParams editParams = miniParams();
            editParams.leftMargin = dp(this, 5); row.addView(edit, editParams);
            edit.setContentDescription(getString(R.string.custom_editor_edit_description, exercise.getName()));
            TextView up = mini("↑", index > 0); LinearLayout.LayoutParams upParams = miniParams();
            upParams.leftMargin = dp(this, 5); row.addView(up, upParams);
            up.setContentDescription(getString(R.string.custom_editor_up_description, exercise.getName()));
            TextView down = mini("↓", index < selected.size() - 1); LinearLayout.LayoutParams downParams = miniParams();
            downParams.leftMargin = dp(this, 5); row.addView(down, downParams);
            down.setContentDescription(getString(R.string.custom_editor_down_description, exercise.getName()));
            TextView remove = mini("×", true); LinearLayout.LayoutParams removeParams = miniParams();
            removeParams.leftMargin = dp(this, 5); row.addView(remove, removeParams);
            remove.setContentDescription(getString(R.string.custom_editor_remove_description, exercise.getName()));
            up.setOnClickListener(v -> move(index, -1)); down.setOnClickListener(v -> move(index, 1));
            edit.setOnClickListener(v -> editSettings(exercise));
            remove.setOnClickListener(v -> { selected.remove(index); renderSelected(); });
            selectedList.addView(card);
        }
    }

    private void move(int index, int offset) {
        int target = index + offset; if (target < 0 || target >= selected.size()) return;
        Exercise item = selected.remove(index); selected.add(target, item); renderSelected();
    }

    private CustomExerciseOrderEntry currentSettings(Exercise exercise) {
        CustomExerciseOrderEntry saved = savedSettings.get(exercise.getName());
        int sets = saved == null || saved.getSets() == null ? 3 : saved.getSets();
        int rest = saved == null || saved.getRestSeconds() == null ? 60 : saved.getRestSeconds();
        int restAfter = saved == null || saved.getRestAfterSeconds() == null ? rest : saved.getRestAfterSeconds();
        Integer reps = exercise.isTimed() ? null : saved == null || saved.getReps() == null
                ? exercise.getDefaultReps() : saved.getReps();
        Integer seconds = exercise.isTimed() ? saved == null || saved.getSeconds() == null
                ? exercise.getDefaultSeconds() : saved.getSeconds() : null;
        return new CustomExerciseOrderEntry(workoutId, exercise.getName(), 0, sets, reps,
                seconds, rest, restAfter);
    }

    private void editSettings(Exercise exercise) {
        CustomExerciseOrderEntry current = currentSettings(exercise);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(this, 22), dp(this, 8), dp(this, 22), dp(this, 4));
        EditText setsField = numberField(String.valueOf(current.getSets()));
        Integer amount = exercise.isTimed() ? current.getSeconds() : current.getReps();
        EditText targetField = numberField(amount == null ? "" : String.valueOf(amount));
        EditText restField = numberField(String.valueOf(current.getRestSeconds()));
        EditText restAfterField = numberField(String.valueOf(current.getRestAfterSeconds()));
        addSettingsField(form, getString(R.string.custom_editor_sets_label), setsField);
        addSettingsField(form, getString(exercise.isTimed() ? R.string.custom_editor_seconds_label
                : R.string.custom_editor_reps_label), targetField);
        addSettingsField(form, getString(R.string.custom_editor_rest_label), restField);
        addSettingsField(form, getString(R.string.custom_editor_rest_after_label), restAfterField);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog)
                .setTitle(exercise.getName())
                .setView(form)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_save, (callbackDialog, which) -> {
                    Integer sets = positiveInt(setsField), target = positiveInt(targetField);
                    Integer rest = nonNegativeInt(restField), restAfter = nonNegativeInt(restAfterField);
                    if (sets == null || target == null || rest == null || restAfter == null) {
                        show(getString(R.string.custom_editor_error_settings));
                        return;
                    }
                    savedSettings.put(exercise.getName(), new CustomExerciseOrderEntry(workoutId,
                            exercise.getName(), 0, sets, exercise.isTimed() ? null : target,
                            exercise.isTimed() ? target : null, rest, restAfter));
                    renderSelected();
                }).create();
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(R.color.performance_surface);
        android.widget.TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (title != null) title.setTextColor(UiColors.GRAPHITE);
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(UiColors.ORANGE_DARK);
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(UiColors.MUTED);
    }

    private void addSettingsField(LinearLayout form, String label, EditText input) {
        TextView labelView = text(this, label, 11, UiColors.MUTED, true);
        labelView.setLetterSpacing(.06f);
        LinearLayout.LayoutParams labelParams = matchWrap();
        labelParams.topMargin = dp(this, 6);
        labelParams.bottomMargin = dp(this, 4);
        form.addView(labelView, labelParams);
        form.addView(input, dialogFieldParams());
    }

    private EditText numberField(String value) {
        EditText field = field("", InputType.TYPE_CLASS_NUMBER);
        field.setText(value);
        return field;
    }
    private LinearLayout.LayoutParams dialogFieldParams() {
        LinearLayout.LayoutParams params = matchWrap(); params.bottomMargin = dp(this, 8); return params;
    }
    private Integer positiveInt(EditText field) {
        Integer value = nonNegativeInt(field);
        return value == null || value == 0 ? null : value;
    }
    private Integer nonNegativeInt(EditText field) {
        try {
            String value = field.getText().toString().trim();
            if (value.isEmpty()) return null;
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ignored) { return null; }
    }

    private void saveWorkout() {
        String workoutName = name.getText().toString().trim();
        if (workoutName.isEmpty()) { name.setError(getString(R.string.custom_editor_error_name)); name.requestFocus(); return; }
        if (selected.isEmpty()) { show(getString(R.string.custom_editor_error_no_exercises)); return; }
        save.setEnabled(false); save.setText(R.string.custom_editor_saving);
        List<CustomExerciseOrderEntry> entries = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            Exercise exercise = selected.get(i); CustomExerciseOrderEntry settings = currentSettings(exercise);
            entries.add(new CustomExerciseOrderEntry(workoutId, exercise.getName(), i + 1,
                    settings.getSets(), settings.getReps(), settings.getSeconds(),
                    settings.getRestSeconds(), settings.getRestAfterSeconds()));
        }
        if (workoutId == null) {
            workouts.create(workoutName, description.getText().toString(), entries, new ServiceCallback<String>() {
                @Override public void onSuccess(String id) { show(getString(R.string.custom_editor_created)); finish(); }
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
                            @Override public void onSuccess(Void result) { show(getString(R.string.custom_editor_updated)); finish(); }
                            @Override public void onError(String message) { restoreSave(message); }
                        });
                    }
                    @Override public void onError(String message) { restoreSave(message); }
                });
            }
            @Override public void onError(String message) { restoreSave(message); }
        });
    }

    private void restoreSave(String message) { save.setEnabled(true); save.setText(saveLabel()); show(message); }
    private int saveLabel() { return workoutId == null ? R.string.custom_editor_create : R.string.custom_editor_save; }
    /** "3 sets  ·  12 reps  ·  60 sec rest": what the workout will ask for this exercise. */
    private String settingsOf(Exercise exercise) {
        CustomExerciseOrderEntry settings = currentSettings(exercise);
        Integer amount = exercise.isTimed() ? settings.getSeconds() : settings.getReps();
        String target = amount == null ? getString(exercise.isTimed() ? R.string.target_timed : R.string.target_reps_unknown)
                : exercise.isTimed() ? getString(R.string.target_seconds, amount)
                : getResources().getQuantityString(R.plurals.target_reps, amount, amount);
        return getString(R.string.target_sets_target_rest,
                getResources().getQuantityString(R.plurals.target_sets, settings.getSets(), settings.getSets()),
                target, getString(R.string.target_rest, settings.getRestSeconds()));
    }
    private boolean contains(String name) { for (Exercise e : selected) if (e.getName().equals(name)) return true; return false; }
    private void remove(String name) { for (int i = selected.size() - 1; i >= 0; i--) if (selected.get(i).getName().equals(name)) selected.remove(i); }
    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this); e.setSingleLine(inputType != (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setHint(hint); e.setTextSize(14); e.setTextColor(UiColors.GRAPHITE); e.setHintTextColor(UiColors.MUTED);
        e.setInputType(inputType); e.setPadding(dp(this, 14), dp(this, 10), dp(this, 14), dp(this, 10)); e.setBackground(rounded(UiColors.SURFACE, dp(this, 14)));
        return e;
    }
    private TextView sectionLabel(String value, int margin) { TextView t = text(this, value, 12, UiColors.MUTED, true); t.setLetterSpacing(.08f); LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(this, margin); p.bottomMargin = dp(this, 9); t.setLayoutParams(p); return t; }
    private Button button(String label, boolean primary) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(primary ? UiColors.INK : UiColors.ORANGE_DARK); b.setBackground(rounded(primary ? UiColors.ORANGE_DARK : UiColors.SURFACE, dp(this, 14))); return b; }
    private TextView mini(String label, boolean enabled) { TextView t = text(this, label, 16, enabled ? UiColors.ORANGE_DARK : UiColors.MUTED, true); t.setGravity(Gravity.CENTER); t.setBackground(rounded(UiColors.SURFACE_ALT, dp(this, 9))); t.setEnabled(enabled); return t; }
    private LinearLayout.LayoutParams miniParams() { return new LinearLayout.LayoutParams(dp(this, 32), dp(this, 36)); }
    private View gap(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(this, height))); return v; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private void show(String message) { Snackbar.make(root, message, Snackbar.LENGTH_LONG).show(); }
}
