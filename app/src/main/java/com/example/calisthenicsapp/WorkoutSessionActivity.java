package com.example.calisthenicsapp;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutLogService;
import com.example.calisthenicsapp.Session.WorkoutSession;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lottiefiles.dotlottie.core.model.Config;
import com.lottiefiles.dotlottie.core.util.DotLottieSource;
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation;

import java.io.IOException;
import java.util.Locale;

/** Guided workout timer and progress screen (FR-5.1, FR-5.2). */
public class WorkoutSessionActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION = "com.example.calisthenicsapp.WORKOUT_SESSION";
    private static final String STATE_SESSION = "active_workout_session";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WorkoutSession session;
    private TextView workoutName, progress, exerciseName, phaseLabel, timer, target, next,
            instructions, elapsed, mainAction, pauseAction;
    private android.widget.ProgressBar progressBar;
    private ImageView animation;
    private DotLottieAnimation lottie;
    private View placeholder;
    private String displayedAnimation;
    private ToneGenerator tone;
    private TelephonyManager telephony;
    private PhoneStateListener phoneStateListener;
    private boolean pausedByCall;
    private boolean askingPhonePermission;
    private static final int REQUEST_PHONE_STATE = 7102;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (session == null || !session.isActive()) return;
            boolean changed = session.tick();
            if (changed) cue();
            if (session.isOver()) { openSummary(); return; }
            render();
            handler.postDelayed(this, 250);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_workout_session);
        bindViews();
        if (savedInstanceState != null) {
            session = (WorkoutSession) savedInstanceState.getSerializable(STATE_SESSION);
        }
        if (session == null) session = (WorkoutSession) getIntent().getSerializableExtra(EXTRA_SESSION);
        if (session == null) { finish(); return; }
        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65);
        setupPhoneInterruption();
        findViewById(R.id.sessionCancel).setOnClickListener(v -> confirmCancel());
        findViewById(R.id.sessionPause).setOnClickListener(v -> togglePause());
        findViewById(R.id.sessionSkipExercise).setOnClickListener(v -> confirmSkipExercise());
        findViewById(R.id.sessionMainAction).setOnClickListener(v -> mainAction());
        render();
    }

    private void bindViews() {
        workoutName = findViewById(R.id.sessionWorkoutName);
        progress = findViewById(R.id.sessionProgress);
        exerciseName = findViewById(R.id.sessionExerciseName);
        phaseLabel = findViewById(R.id.sessionPhaseLabel);
        timer = findViewById(R.id.sessionTimer);
        target = findViewById(R.id.sessionTarget);
        next = findViewById(R.id.sessionNext);
        instructions = findViewById(R.id.sessionInstructions);
        elapsed = findViewById(R.id.sessionElapsed);
        mainAction = findViewById(R.id.sessionMainAction);
        pauseAction = findViewById(R.id.sessionPause);
        progressBar = findViewById(R.id.sessionProgressBar);
        animation = findViewById(R.id.sessionAnimation);
        lottie = findViewById(R.id.sessionLottieAnimation);
        placeholder = findViewById(R.id.sessionAnimationPlaceholder);
    }

    private void render() {
        if (session == null || session.isOver()) return;
        WorkoutSession.SessionExercise current = session.getCurrentExercise();
        if (current == null) return;
        workoutName.setText(session.getWorkoutName());
        exerciseName.setText(current.getName());
        int totalExercises = session.getExercises().size();
        int allSets = 0;
        for (WorkoutSession.SessionExercise e : session.getExercises()) allSets += e.getSets();
        int done = session.getCompletedSets();
        progress.setText("Exercise " + session.getCurrentExerciseNumber() + " of " + totalExercises
                + "  ·  Set " + session.getCurrentSet() + " of " + current.getSets());
        progressBar.setMax(Math.max(1, allSets));
        progressBar.setProgress(done, true);
        instructions.setText(current.getInstructions() == null || current.getInstructions().trim().isEmpty()
                ? "Move with control and keep a comfortable range of motion."
                : current.getInstructions().trim());
        target.setText(targetText(current));
        elapsed.setText(formatTime(session.getTotalSeconds()) + " elapsed  ·  "
                + String.format(Locale.getDefault(), "%.0f kcal", session.getTotalKcal()));
        if (session.getPhase() == WorkoutSession.Phase.REST) {
            phaseLabel.setText("REST · RECOVER FOR THE NEXT SET");
            timer.setText(formatTime(session.getRemainingSeconds() == null ? 0 : session.getRemainingSeconds()));
            mainAction.setText("Skip rest");
            next.setText("Get ready for " + current.getName() + " · Set " + session.getCurrentSet());
        } else {
            phaseLabel.setText(session.isPaused() ? "PAUSED" : "YOUR SET");
            Integer remaining = session.getRemainingSeconds();
            if (current.isTimed() && remaining != null) timer.setText(formatTime(remaining));
            else timer.setText(targetText(current));
            mainAction.setText(current.isTimed() ? "Finish set" : "Set done");
            WorkoutSession.SessionExercise following = session.getNextExercise();
            next.setText(following == null ? "Last exercise · you’re doing great"
                    : "Next up · " + following.getName());
        }
        pauseAction.setText(session.isPaused() ? "Resume" : "Pause");
        if (askingPhonePermission) {
            pauseAction.setEnabled(false);
            mainAction.setEnabled(false);
        }
        showAnimation(current.getAnimationFile(), current.getName());
    }

    /** Pause automatically during an incoming or active phone call, when the user allows it. */
    private void setupPhoneInterruption() {
        telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephony == null) return;
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (!session.isPaused()) session.pause();
            askingPhonePermission = true;
            requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE}, REQUEST_PHONE_STATE);
            return;
        }
        registerPhoneListener();
    }

    private void registerPhoneListener() {
        if (telephony == null || checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;
        phoneStateListener = new PhoneStateListener() {
            @Override public void onCallStateChanged(int state, String phoneNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    if (!session.isPaused()) {
                        session.pause();
                        pausedByCall = true;
                        render();
                    }
                } else if (state == TelephonyManager.CALL_STATE_IDLE && pausedByCall) {
                    pausedByCall = false;
                    session.resume();
                    render();
                }
            }
        };
        telephony.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PHONE_STATE) return;
        askingPhonePermission = false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerPhoneListener();
        } else {
            android.widget.Toast.makeText(this,
                    "Workout will continue normally. You can pause it manually during a call.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
        if (session != null && session.isPaused() && !pausedByCall) session.resume();
        render();
    }

    private String targetText(WorkoutSession.SessionExercise exercise) {
        String value;
        if (exercise.isTimed()) value = (exercise.getTargetSeconds() == null ? "Timed" : exercise.getTargetSeconds() + " sec");
        else value = (exercise.getTargetReps() == null ? "Reps" : exercise.getTargetReps() + " reps");
        return exercise.isPerSide() ? value + " / side" : value;
    }

    private void mainAction() {
        if (session.getPhase() == WorkoutSession.Phase.REST) {
            session.skipRest();
            cue();
        } else if (session.getPhase() == WorkoutSession.Phase.WORK) {
            session.completeSet();
            cue();
        }
        afterAction();
    }

    private void togglePause() {
        if (session.isPaused()) session.resume(); else session.pause();
        render();
    }

    private void afterAction() {
        if (session.isOver()) openSummary(); else render();
    }

    private void confirmSkipExercise() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Skip this exercise?")
                .setMessage("Its remaining sets will be skipped and the workout will be saved as partial.")
                .setNegativeButton("Keep going", null)
                .setPositiveButton("Skip exercise", (d, w) -> {
                    session.skipExercise(); cue(); afterAction();
                }).show();
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("End workout?")
                .setMessage(session.getCompletedSets() == 0
                        ? "No completed sets will be saved."
                        : "Your completed sets will be saved as a partial workout.")
                .setNegativeButton("Keep going", null)
                .setPositiveButton("End workout", (d, w) -> {
                    session.cancel();
                    if (session.isLoggable()) openSummary(); else finish();
                }).show();
    }

    private void openSummary() {
        handler.removeCallbacks(ticker);
        Intent intent = new Intent(this, WorkoutSummaryActivity.class);
        intent.putExtra(WorkoutSummaryActivity.EXTRA_SESSION, session);
        startActivity(intent);
        finish();
    }

    private void showAnimation(String fileName, String exercise) {
        if (fileName == null) {
            displayedAnimation = null;
            animation.setVisibility(View.GONE);
            lottie.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            return;
        }
        if (fileName.equals(displayedAnimation)) return;
        displayedAnimation = fileName;
        animation.setVisibility(View.GONE);
        lottie.setVisibility(View.GONE);
        placeholder.setVisibility(View.VISIBLE);
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".lottie")) {
            try {
                Config config = new Config.Builder().source(new DotLottieSource.Asset(fileName))
                        .autoplay(true).loop(true).build();
                lottie.load(config);
                lottie.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
            } catch (RuntimeException ignored) { }
            return;
        }
        if (!lower.endsWith(".webp")) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            try (java.io.InputStream input = getAssets().open(fileName)) {
                animation.setImageBitmap(BitmapFactory.decodeStream(input));
                animation.setContentDescription(exercise + " movement animation");
                animation.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
            } catch (IOException | RuntimeException ignored) { }
            return;
        }
        try {
            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(getAssets(), fileName));
            animation.setImageDrawable(drawable);
            animation.setContentDescription(exercise + " movement animation");
            animation.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
            if (drawable instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) drawable).start();
        } catch (IOException | RuntimeException ignored) { }
    }

    private void cue() {
        if (tone != null) tone.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        findViewById(R.id.sessionRoot).performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override protected void onResume() {
        super.onResume();
        if (session != null && session.isActive()) handler.post(ticker);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (session != null) outState.putSerializable(STATE_SESSION, session);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() { confirmCancel(); }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if (telephony != null && phoneStateListener != null) {
            telephony.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        if (tone != null) tone.release();
        if (lottie != null) lottie.destroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }
}
