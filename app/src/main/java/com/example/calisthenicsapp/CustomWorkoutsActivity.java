package com.example.calisthenicsapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Service.CustomWorkoutService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutService;
import com.example.calisthenicsapp.Service.WorkoutSessionService;
import com.example.calisthenicsapp.Session.WorkoutSession;
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
        page.setPadding(dp(22), dp(20), dp(22), dp(28));
        scroll.addView(page);

        TextView back = text("‹  Workouts", 15, UiColors.ORANGE_DARK, false);
        back.setPadding(dp(2), dp(4), dp(4), dp(4));
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(space(16));
        page.addView(text("MY TRAINING", 12, UiColors.ORANGE_DARK, true));
        TextView title = text("My workouts", 30, UiColors.GRAPHITE, true);
        LinearLayout.LayoutParams titleParams = matchWrap(); titleParams.topMargin = dp(6);
        page.addView(title, titleParams);
        TextView subtitle = text("Build a routine that fits your goals.", 15, UiColors.MUTED, false);
        LinearLayout.LayoutParams subParams = matchWrap(); subParams.topMargin = dp(4);
        page.addView(subtitle, subParams);

        Button create = button("＋   Create workout", true);
        LinearLayout.LayoutParams actionParams = matchWrap(); actionParams.topMargin = dp(20);
        page.addView(create, actionParams);
        create.setOnClickListener(v -> openEditor(null));
        Button copy = button("Copy a ready-made plan", false);
        LinearLayout.LayoutParams copyParams = matchWrap(); copyParams.topMargin = dp(8);
        page.addView(copy, copyParams);
        copy.setOnClickListener(v -> choosePremade());

        status = text("Loading your workouts…", 13, UiColors.MUTED, true);
        LinearLayout.LayoutParams statusParams = matchWrap(); statusParams.topMargin = dp(22);
        page.addView(status, statusParams);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap(); listParams.topMargin = dp(10);
        page.addView(list, listParams);
        setContentView(scroll);
    }

    private void load() {
        if (list == null) return;
        status.setText("Loading your workouts…");
        customService.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
            @Override public void onSuccess(List<CustomWorkout> workouts) {
                list.removeAllViews();
                status.setText(workouts.size() == 1 ? "1 saved workout" : workouts.size() + " saved workouts");
                for (CustomWorkout workout : workouts) list.addView(workoutCard(workout));
                if (workouts.isEmpty()) {
                    TextView empty = text("No custom workouts yet. Create one or copy a plan to get started.",
                            14, UiColors.MUTED, false);
                    empty.setPadding(dp(4), dp(8), dp(4), dp(12)); list.addView(empty);
                }
            }
            @Override public void onError(String message) { status.setText("Couldn't load workouts"); show(message); }
        });
    }

    private View workoutCard(CustomWorkout workout) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(20)); card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER); card.setStrokeWidth(dp(1)); card.setCardElevation(dp(1));
        LinearLayout.LayoutParams cardParams = matchWrap(); cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(15), dp(16), dp(14)); card.addView(content);
        TextView title = text(workout.getName(), 18, UiColors.GRAPHITE, true); content.addView(title);
        String description = workout.getDescription();
        TextView details = text((description == null || description.trim().isEmpty()
                ? "Your custom routine" : description) + "\n" + duration(workout.getEstimatedSeconds()),
                13, UiColors.MUTED, false);
        LinearLayout.LayoutParams detailParams = matchWrap(); detailParams.topMargin = dp(7);
        content.addView(details, detailParams);
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = matchWrap(); actionsParams.topMargin = dp(12);
        content.addView(actions, actionsParams);
        Button start = smallButton("Start", true); actions.addView(start, weightButton());
        Button edit = smallButton("Edit", false); LinearLayout.LayoutParams editParams = weightButton();
        editParams.leftMargin = dp(7); actions.addView(edit, editParams);
        Button more = smallButton("•••", false); LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(54), dp(40));
        moreParams.leftMargin = dp(7); actions.addView(more, moreParams);
        start.setOnClickListener(v -> startWorkout(workout));
        edit.setOnClickListener(v -> openEditor(workout));
        card.setOnClickListener(v -> openDetails(workout));
        more.setOnClickListener(v -> showActions(workout));
        return card;
    }

    private void showActions(CustomWorkout workout) {
        String[] actions = {"View workout", "Edit workout", "Rename", "Delete"};
        new AlertDialog.Builder(this).setTitle(workout.getName()).setItems(actions, (dialog, which) -> {
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
        new AlertDialog.Builder(this).setTitle("Choose a plan to copy").setItems(names, (d, which) -> {
            Workout selected = premade.get(which);
            customService.duplicatePremade(selected.getName(), null, new ServiceCallback<String>() {
                @Override public void onSuccess(String id) { load(); show("Workout copied — you can edit it now."); }
                @Override public void onError(String message) { show(message); }
            });
        }).show();
    }

    private void rename(CustomWorkout workout) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true); input.setText(workout.getName());
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        new AlertDialog.Builder(this).setTitle("Rename workout").setView(input)
                .setNegativeButton("Cancel", null).setPositiveButton("Save", (d, w) ->
                        customService.rename(workout.getId(), input.getText().toString(), new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void result) { load(); }
                            @Override public void onError(String message) { show(message); }
                        })).show();
    }

    private void confirmDelete(CustomWorkout workout) {
        new AlertDialog.Builder(this).setTitle("Delete workout?")
                .setMessage("“" + workout.getName() + "” will be removed, along with its future schedule entries.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) ->
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
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD); return t;
    }
    private Button button(String label, boolean primary) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(primary ? UiColors.INK : UiColors.ORANGE_DARK);
        b.setBackground(rounded(primary ? UiColors.ORANGE_DARK : UiColors.SURFACE, dp(14)));
        return b;
    }
    private Button smallButton(String label, boolean primary) {
        Button b = button(label, primary); b.setTextSize(12); b.setMinHeight(dp(40)); b.setMinimumHeight(dp(40));
        b.setPadding(dp(4), 0, dp(4), 0); return b;
    }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private View space(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); return v; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weightButton() { return new LinearLayout.LayoutParams(0, dp(40), 1f); }
    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private String duration(Integer seconds) {
        if (seconds == null || seconds <= 0) return "Ready to train";
        int mins = Math.max(1, (seconds + 59) / 60); return mins + " min";
    }
}
