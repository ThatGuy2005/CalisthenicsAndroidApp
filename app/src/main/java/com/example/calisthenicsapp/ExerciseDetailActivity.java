package com.example.calisthenicsapp;

import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.lottiefiles.dotlottie.core.model.Config;
import com.lottiefiles.dotlottie.core.util.DotLottieSource;
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.Exercise;
import com.example.calisthenicsapp.Service.ExerciseService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExerciseDetailActivity extends AppCompatActivity {
    public static final String EXTRA_EXERCISE_NAME = "com.example.calisthenicsapp.EXERCISE_NAME";

    private TextView nameView;
    private TextView summaryView;
    private TextView muscleGroupView;
    private TextView difficultyView;
    private LinearLayout instructionsContainer;
    private TextView tipView;
    private View tipCard;
    private TextView equipmentView;
    private TextView targetView;
    private ImageView animationView;
    private DotLottieAnimation lottieAnimationView;
    private View animationPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        nameView = findViewById(R.id.exerciseDetailName);
        summaryView = findViewById(R.id.exerciseDetailSummary);
        muscleGroupView = findViewById(R.id.exerciseMuscleGroup);
        difficultyView = findViewById(R.id.exerciseDifficulty);
        instructionsContainer = findViewById(R.id.exerciseInstructions);
        tipView = findViewById(R.id.exerciseTip);
        tipCard = findViewById(R.id.exerciseTipCard);
        equipmentView = findViewById(R.id.exerciseEquipment);
        targetView = findViewById(R.id.exerciseTarget);
        animationView = findViewById(R.id.exerciseAnimation);
        lottieAnimationView = findViewById(R.id.exerciseLottieAnimation);
        animationPlaceholder = findViewById(R.id.animationPlaceholder);

        Button back = findViewById(R.id.exerciseBackButton);
        back.setOnClickListener(view -> finish());

        String exerciseName = getIntent().getStringExtra(EXTRA_EXERCISE_NAME);
        if (exerciseName == null || exerciseName.trim().isEmpty()) {
            finish();
            return;
        }
        new ExerciseService(this).getExercise(exerciseName,
                new ServiceCallback<Exercise>() {
                    @Override
                    public void onSuccess(Exercise exercise) {
                        if (exercise == null) {
                            Snackbar.make(findViewById(R.id.exerciseDetailRoot),
                                    "This exercise could not be found.", Snackbar.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        showExercise(exercise);
                    }

                    @Override
                    public void onError(String message) {
                        Snackbar.make(findViewById(R.id.exerciseDetailRoot), message,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showExercise(Exercise exercise) {
        nameView.setText(exercise.getName());
        summaryView.setText(getString(exercise.isMultiMuscle()
                ? R.string.exercise_detail_compound : R.string.exercise_detail_focused,
                exercise.getMuscleGroup()));
        muscleGroupView.setText(exercise.getMuscleGroup());
        difficultyView.setText(exercise.getDifficulty().getLabel());
        animationView.setContentDescription(exercise.getName() + " movement animation");
        populateInstructions(exercise.getInstructions());
        equipmentView.setText(exercise.getEquipment().getLabel());

        String target = exercise.isTimed()
                ? "Suggested target: " + exercise.getDefaultSeconds() + " seconds"
                : "Suggested target: " + exercise.getDefaultReps() + " reps";
        if (exercise.isPerSide()) target += " per side";
        targetView.setText(target);

        if (exercise.getAnimationFile() != null) showAnimation(exercise.getAnimationFile());
    }

    private void populateInstructions(String instructions) {
        instructionsContainer.removeAllViews();
        tipCard.setVisibility(View.GONE);

        String text = instructions == null ? "" : instructions.trim();
        String[] parts = text.split("(?i)\\bTip:\\s*", 2);
        String stepsText = parts[0].trim();
        if (parts.length > 1 && !parts[1].trim().isEmpty()) {
            tipView.setText(parts[1].trim());
            tipCard.setVisibility(View.VISIBLE);
        }

        Matcher matcher = Pattern.compile("(\\d+)\\)\\s*").matcher(stepsText);
        int previousEnd = 0;
        String stepNumber = null;
        while (matcher.find()) {
            if (stepNumber != null) {
                addInstructionStep(stepNumber, stepsText.substring(previousEnd, matcher.start()).trim());
            } else if (!stepsText.substring(0, matcher.start()).trim().isEmpty()) {
                addInstructionStep("•", stepsText.substring(0, matcher.start()).trim());
            }
            stepNumber = matcher.group(1);
            previousEnd = matcher.end();
        }

        if (stepNumber != null) {
            addInstructionStep(stepNumber, stepsText.substring(previousEnd).trim());
        } else if (!stepsText.isEmpty()) {
            addInstructionStep("•", stepsText);
        }
    }

    private void addInstructionStep(String number, String text) {
        if (text.isEmpty()) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(12);
        instructionsContainer.addView(row, rowParams);

        TextView numberView = new TextView(this);
        numberView.setBackgroundResource(R.drawable.exercise_step_number_background);
        numberView.setText(number);
        numberView.setTextColor(UiColors.ORANGE_DARK);
        numberView.setTextSize(13);
        numberView.setTypeface(null, android.graphics.Typeface.BOLD);
        numberView.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        numberParams.topMargin = dp(1);
        row.addView(numberView, numberParams);

        TextView instructionView = new TextView(this);
        instructionView.setText(text);
        instructionView.setTextColor(UiColors.GRAPHITE);
        instructionView.setTextSize(15);
        instructionView.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(12);
        row.addView(instructionView, textParams);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showAnimation(String fileName) {
        String lowerName = fileName.toLowerCase(java.util.Locale.ROOT);
        if (lowerName.endsWith(".lottie")) {
            try {
                Config config = new Config.Builder()
                        .source(new DotLottieSource.Asset(fileName))
                        .autoplay(true)
                        .loop(true)
                        .build();
                lottieAnimationView.load(config);
                lottieAnimationView.setVisibility(View.VISIBLE);
                animationPlaceholder.setVisibility(View.GONE);
            } catch (RuntimeException ignored) {
                // Keep the designed placeholder if the animation cannot be loaded.
            }
            return;
        }
        if (!lowerName.endsWith(".webp")) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            try (java.io.InputStream input = getAssets().open(fileName)) {
                Bitmap firstFrame = BitmapFactory.decodeStream(input);
                if (firstFrame != null) {
                    animationView.setImageBitmap(firstFrame);
                    animationPlaceholder.setVisibility(View.GONE);
                    animationView.setVisibility(View.VISIBLE);
                }
            } catch (IOException | RuntimeException ignored) {
                // Keep the placeholder if the WebP cannot be loaded.
            }
            return;
        }
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(getAssets(), fileName);
            Drawable drawable = ImageDecoder.decodeDrawable(source);
            animationView.setImageDrawable(drawable);
            animationPlaceholder.setVisibility(View.GONE);
            animationView.setVisibility(View.VISIBLE);
            if (drawable instanceof AnimatedImageDrawable) {
                ((AnimatedImageDrawable) drawable).start();
            }
        } catch (IOException | RuntimeException ignored) {
            // Keep the designed placeholder if the animation has not been added or cannot load.
        }
    }

    @Override
    protected void onDestroy() {
        if (lottieAnimationView != null) lottieAnimationView.destroy();
        super.onDestroy();
    }
}
