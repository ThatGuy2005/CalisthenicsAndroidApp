package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.rounded;
import static com.example.calisthenicsapp.Views.text;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Service.CustomWorkoutService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutService;
import com.example.calisthenicsapp.Service.WorkoutSessionService;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/** Manage the user's routines and copy a premade routine as a starting point. */
public class CustomWorkoutsActivity extends AppCompatActivity {
    private LinearLayout list;
    private TextView status;
    private View root;
    private CustomWorkoutService customService;
    private List<Workout> premade = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        customService = new CustomWorkoutService(this);
        buildScreen();
        new WorkoutService(this).getAllWorkouts(new ServiceCallback<List<Workout>>() {
            @Override public void onSuccess(List<Workout> result) { premade = result; }
            @Override public void onError(String message) { show(message); }
        });
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundResource(R.drawable.page_background);
        root = scroll;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(this, 22), dp(this, 20), dp(this, 22), dp(this, 28));
        scroll.addView(page);

        TextView back = text(this, getString(R.string.custom_workouts_back), 15, UiColors.ORANGE_DARK, false);
        back.setPadding(dp(this, 2), dp(this, 4), dp(this, 4), dp(this, 4));
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(space(16));
        page.addView(text(this, getString(R.string.custom_workouts_eyebrow), 12, UiColors.ORANGE_DARK, true));
        TextView title = text(this, getString(R.string.custom_workouts_title), 30, UiColors.GRAPHITE, true);
        LinearLayout.LayoutParams titleParams = matchWrap(); titleParams.topMargin = dp(this, 6);
        page.addView(title, titleParams);
        TextView subtitle = text(this, getString(R.string.custom_workouts_subtitle), 15, UiColors.MUTED, false);
        LinearLayout.LayoutParams subParams = matchWrap(); subParams.topMargin = dp(this, 4);
        page.addView(subtitle, subParams);

        Button create = button(getString(R.string.custom_workouts_create), true);
        LinearLayout.LayoutParams actionParams = matchWrap(); actionParams.topMargin = dp(this, 20);
        page.addView(create, actionParams);
        create.setOnClickListener(v -> openEditor(null));
        Button copy = button(getString(R.string.custom_workouts_copy), false);
        LinearLayout.LayoutParams copyParams = matchWrap(); copyParams.topMargin = dp(this, 8);
        page.addView(copy, copyParams);
        copy.setOnClickListener(v -> choosePremade());

        status = text(this, getString(R.string.custom_workouts_loading), 13, UiColors.MUTED, true);
        LinearLayout.LayoutParams statusParams = matchWrap(); statusParams.topMargin = dp(this, 22);
        page.addView(status, statusParams);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap(); listParams.topMargin = dp(this, 10);
        page.addView(list, listParams);
        setContentView(scroll);
    }

    private void load() {
        if (list == null) return;
        status.setText(R.string.custom_workouts_loading);
        customService.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
            @Override public void onSuccess(List<CustomWorkout> workouts) {
                list.removeAllViews();
                status.setText(getResources().getQuantityString(R.plurals.custom_workouts_count,
                        workouts.size(), workouts.size()));
                for (CustomWorkout workout : workouts) list.addView(workoutCard(workout));
                if (workouts.isEmpty()) {
                    Context context = CustomWorkoutsActivity.this;
                    TextView empty = text(context, getString(R.string.custom_workouts_empty),
                            14, UiColors.MUTED, false);
                    empty.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 12)); list.addView(empty);
                }
            }
            @Override public void onError(String message) { status.setText(R.string.custom_workouts_failed); show(message); }
        });
    }

    private View workoutCard(CustomWorkout workout) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(this, 20)); card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER); card.setStrokeWidth(dp(this, 1)); card.setCardElevation(dp(this, 1));
        LinearLayout.LayoutParams cardParams = matchWrap(); cardParams.bottomMargin = dp(this, 12);
        card.setLayoutParams(cardParams);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(this, 16), dp(this, 15), dp(this, 16), dp(this, 14)); card.addView(content);
        TextView title = text(this, workout.getName(), 18, UiColors.GRAPHITE, true); content.addView(title);
        String description = workout.getDescription();
        TextView details = text(this, (description == null || description.trim().isEmpty()
                ? getString(R.string.custom_workouts_no_description) : description) + "\n"
                + Durations.estimate(this, workout.getEstimatedSeconds(), R.string.custom_workouts_no_length),
                13, UiColors.MUTED, false);
        LinearLayout.LayoutParams detailParams = matchWrap(); detailParams.topMargin = dp(this, 7);
        content.addView(details, detailParams);
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = matchWrap(); actionsParams.topMargin = dp(this, 12);
        content.addView(actions, actionsParams);
        Button start = smallButton(getString(R.string.custom_workouts_start), true); actions.addView(start, weightButton());
        Button edit = smallButton(getString(R.string.custom_workouts_edit), false); LinearLayout.LayoutParams editParams = weightButton();
        editParams.leftMargin = dp(this, 7); actions.addView(edit, editParams);
        Button more = smallButton("•••", false); LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(this, 54), dp(this, 40));
        moreParams.leftMargin = dp(this, 7); actions.addView(more, moreParams);
        start.setOnClickListener(v -> startWorkout(workout));
        edit.setOnClickListener(v -> openEditor(workout));
        card.setOnClickListener(v -> openDetails(workout));
        more.setOnClickListener(v -> showActions(workout));
        return card;
    }

    private void showActions(CustomWorkout workout) {
        String[] actions = getResources().getStringArray(R.array.custom_workouts_actions);
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(workout.getName()).setItems(actions, (dialog, which) -> {
            if (which == 0) openDetails(workout);
            else if (which == 1) openEditor(workout);
            else if (which == 2) rename(workout);
            else confirmDelete(workout);
        }).show();
    }

    private void choosePremade() {
        if (premade.isEmpty()) {
            new WorkoutService(this).getAllWorkouts(new ServiceCallback<List<Workout>>() {
                @Override public void onSuccess(List<Workout> result) { premade = result; showPremadeDialog(); }
                @Override public void onError(String message) { show(message); }
            });
        } else showPremadeDialog();
    }

    private void showPremadeDialog() {
        String[] names = new String[premade.size()];
        for (int i = 0; i < premade.size(); i++) names[i] = premade.get(i).getName();
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.custom_workouts_copy_title).setItems(names, (d, which) -> {
            Workout selected = premade.get(which);
            customService.duplicatePremade(selected.getName(), null, new ServiceCallback<String>() {
                @Override public void onSuccess(String id) { load(); show(getString(R.string.custom_workouts_copied)); }
                @Override public void onError(String message) { show(message); }
            });
        }).show();
    }

    private void rename(CustomWorkout workout) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true); input.setText(workout.getName());
        input.setPadding(dp(this, 14), dp(this, 10), dp(this, 14), dp(this, 10));
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.custom_workouts_rename_title).setView(input)
                .setNegativeButton(R.string.common_cancel, null).setPositiveButton(R.string.common_save, (d, w) ->
                        customService.rename(workout.getId(), input.getText().toString(), new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void result) { load(); }
                            @Override public void onError(String message) { show(message); }
                        })).show();
    }

    private void confirmDelete(CustomWorkout workout) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.custom_workouts_delete_title)
                .setMessage(getString(R.string.custom_workouts_delete_message, workout.getName()))
                .setNegativeButton(R.string.common_cancel, null).setPositiveButton(R.string.common_delete, (d, w) ->
                        customService.delete(workout.getId(), new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void result) { load(); }
                            @Override public void onError(String message) { show(message); }
                        })).show();
    }

    private void startWorkout(CustomWorkout workout) {
        new WorkoutSessionService(this).startCustom(workout.getId(), new ServiceCallback<WorkoutSession>() {
            @Override public void onSuccess(WorkoutSession session) {
                Intent i = new Intent(CustomWorkoutsActivity.this, WorkoutSessionActivity.class);
                i.putExtra(WorkoutSessionActivity.EXTRA_SESSION, session); startActivity(i);
            }
            @Override public void onError(String message) { show(message); }
        });
    }

    private void openEditor(CustomWorkout workout) {
        Intent i = new Intent(this, CustomWorkoutEditorActivity.class);
        if (workout != null) i.putExtra(CustomWorkoutEditorActivity.EXTRA_WORKOUT_ID, workout.getId());
        startActivity(i);
    }

    private void openDetails(CustomWorkout workout) {
        Intent i = new Intent(this, CustomWorkoutDetailActivity.class);
        i.putExtra(CustomWorkoutDetailActivity.EXTRA_WORKOUT_ID, workout.getId());
        startActivity(i);
    }

    private void show(String message) { Snackbar.make(root, message, Snackbar.LENGTH_LONG).show(); }
    private Button button(String label, boolean primary) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(primary ? UiColors.INK : UiColors.ORANGE_DARK);
        b.setBackground(rounded(primary ? UiColors.ORANGE_DARK : UiColors.SURFACE, dp(this, 14)));
        return b;
    }
    private Button smallButton(String label, boolean primary) {
        Button b = button(label, primary); b.setTextSize(12); b.setMinHeight(dp(this, 40)); b.setMinimumHeight(dp(this, 40));
        b.setPadding(dp(this, 4), 0, dp(this, 4), 0); return b;
    }
    private View space(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(this, height))); return v; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weightButton() { return new LinearLayout.LayoutParams(0, dp(this, 40), 1f); }
}
