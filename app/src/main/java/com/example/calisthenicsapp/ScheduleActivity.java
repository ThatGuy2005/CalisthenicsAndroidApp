package com.example.calisthenicsapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.Workout;
import com.example.calisthenicsapp.Scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.Scheduling.Recurrence;
import com.example.calisthenicsapp.Scheduling.ScheduleOccurrence;
import com.example.calisthenicsapp.Service.CustomWorkoutService;
import com.example.calisthenicsapp.Service.DataChanges;
import com.example.calisthenicsapp.Service.ScheduleService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Service.WorkoutService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = HomeActivity.EXTRA_PREVIEW_MODE;
    private static final String[] REPEAT_OPTIONS = {
            "Does not repeat", "Every day", "Every week", "Every month"
    };

    private final Calendar selectedDay = startOfDay(Calendar.getInstance());
    private final List<WorkoutOption> workoutOptions = new ArrayList<>();
    private final List<ScheduleOccurrence> weekOccurrences = new ArrayList<>();
    private LinearLayout weekDays;
    private LinearLayout agenda;
    private TextView monthTitle;
    private TextView selectedDateTitle;
    private TextView daySubtitle;
    private ScheduleService scheduleService;
    private WorkoutService workoutService;
    private CustomWorkoutService customWorkoutService;
    private boolean previewMode;
    private boolean workoutChoicesLoaded;
    private boolean loadingWorkoutChoices;
    private String pendingCalendarScheduleId;
    private WorkoutOption pendingChoice;
    private long pendingScheduledAt;
    private Recurrence pendingRecurrence;

    private final DataChanges.Listener dataListener = kind -> {
        if (kind == DataChanges.Kind.SCHEDULES) loadWeek();
        if (kind == DataChanges.Kind.CUSTOM_WORKOUTS) loadWorkoutChoices();
    };

    private final ActivityResultLauncher<String[]> calendarPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> onCalendarPermissionResult(result));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        previewMode = getIntent().getBooleanExtra(EXTRA_PREVIEW_MODE, false);
        weekDays = findViewById(R.id.scheduleWeekDays);
        agenda = findViewById(R.id.scheduleAgenda);
        monthTitle = findViewById(R.id.scheduleMonthTitle);
        selectedDateTitle = findViewById(R.id.scheduleSelectedDate);
        daySubtitle = findViewById(R.id.scheduleDaySubtitle);

        findViewById(R.id.schedulePreviousWeek).setOnClickListener(v -> changeWeek(-1));
        findViewById(R.id.scheduleNextWeek).setOnClickListener(v -> changeWeek(1));
        findViewById(R.id.scheduleTodayButton).setOnClickListener(v -> {
            selectedDay.setTimeInMillis(System.currentTimeMillis());
            selectedDay.set(Calendar.HOUR_OF_DAY, 0);
            selectedDay.set(Calendar.MINUTE, 0);
            selectedDay.set(Calendar.SECOND, 0);
            selectedDay.set(Calendar.MILLISECOND, 0);
            renderWeek();
            loadWeek();
        });
        findViewById(R.id.scheduleAddButton).setOnClickListener(v -> showAddWorkoutDialog());

        BottomNavigationView navigation = findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(R.id.nav_schedule);
        navigation.setOnItemSelectedListener(this::onNavigationSelected);

        if (previewMode) {
            renderWeek();
            renderPreviewAgenda();
        } else {
            scheduleService = new ScheduleService(this);
            workoutService = new WorkoutService(this);
            customWorkoutService = new CustomWorkoutService(this);
            renderWeek();
            DataChanges.observe(this, dataListener,
                    DataChanges.Kind.SCHEDULES, DataChanges.Kind.CUSTOM_WORKOUTS);
        }
    }

    private boolean onNavigationSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_schedule) return true;
        Intent intent;
        if (item.getItemId() == R.id.nav_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.nav_workouts) {
            intent = new Intent(this, WorkoutListActivity.class);
        } else if (item.getItemId() == R.id.nav_exercises) {
            intent = new Intent(this, ExerciseListActivity.class);
        } else if (item.getItemId() == R.id.nav_history) {
            intent = new Intent(this, HistoryActivity.class);
        } else {
            return false;
        }
        intent.putExtra(HomeActivity.EXTRA_PREVIEW_MODE, previewMode);
        startActivity(intent);
        finish();
        return true;
    }

    private void changeWeek(int offset) {
        selectedDay.add(Calendar.DAY_OF_MONTH, offset * 7);
        renderWeek();
        loadWeek();
    }

    private void renderWeek() {
        Calendar firstDay = (Calendar) selectedDay.clone();
        int difference = (firstDay.get(Calendar.DAY_OF_WEEK) - firstDay.getFirstDayOfWeek() + 7) % 7;
        firstDay.add(Calendar.DAY_OF_MONTH, -difference);
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDay.getTime()));
        selectedDateTitle.setText(new SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                .format(selectedDay.getTime()));
        daySubtitle.setText(isToday(selectedDay) ? "Today" : "Your plan for this day");
        weekDays.removeAllViews();

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) firstDay.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            boolean selected = sameDay(day, selectedDay);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(rounded(selected ? UiColors.ORANGE : Color.TRANSPARENT, dp(15)));
            // Keep the two-letter weekday labels inside narrow tiles on small screens.
            tile.setPadding(dp(3), dp(7), dp(3), dp(5));
            TextView dayName = new TextView(this);
            dayName.setText(new SimpleDateFormat("EE", Locale.getDefault()).format(day.getTime())
                    .replace(".", "").toUpperCase(Locale.getDefault()));
            dayName.setTextColor(selected ? UiColors.INK : UiColors.MUTED);
            dayName.setTextSize(9);
            dayName.setTypeface(null, android.graphics.Typeface.BOLD);
            dayName.setGravity(Gravity.CENTER);
            tile.addView(dayName, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView dayNumber = new TextView(this);
            dayNumber.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            dayNumber.setTextColor(selected ? UiColors.INK : UiColors.GRAPHITE);
            dayNumber.setTextSize(16);
            dayNumber.setTypeface(null, android.graphics.Typeface.BOLD);
            dayNumber.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            numberParams.topMargin = dp(4);
            tile.addView(dayNumber, numberParams);

            View dot = new View(this);
            dot.setBackground(rounded(selected ? 0xFFC35CFF : 0xFFD08BFF, dp(4)));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(4), dp(4));
            dotParams.topMargin = dp(4);
            dot.setVisibility(hasOccurrenceOn(day) ? View.VISIBLE : View.INVISIBLE);
            tile.addView(dot, dotParams);

            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(72), 1f);
            if (i > 0) tileParams.leftMargin = dp(2);
            weekDays.addView(tile, tileParams);
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setOnClickListener(v -> {
                selectedDay.setTimeInMillis(day.getTimeInMillis());
                renderWeek();
                renderAgendaForSelectedDay();
            });
        }
    }

    private void loadWeek() {
        Calendar start = weekStart(selectedDay);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 7);
        scheduleService.getOccurrences(start.getTimeInMillis() / 1000L,
                end.getTimeInMillis() / 1000L, new ServiceCallback<List<ScheduleOccurrence>>() {
                    @Override public void onSuccess(List<ScheduleOccurrence> result) {
                        weekOccurrences.clear();
                        weekOccurrences.addAll(result);
                        renderWeek();
                        renderAgendaForSelectedDay();
                    }
                    @Override public void onError(String message) {
                        weekOccurrences.clear();
                        renderWeek();
                        showEmptyState("Your schedule could not be loaded.", message);
                    }
                });
    }

    private void renderAgendaForSelectedDay() {
        List<ScheduleOccurrence> selected = new ArrayList<>();
        for (ScheduleOccurrence occurrence : weekOccurrences) {
            if (sameDay(occurrence.getStartSeconds(), selectedDay)) selected.add(occurrence);
        }
        if (selected.isEmpty()) {
            showEmptyState("A little space for your next session.",
                    "Nothing planned yet. Pick a workout and set a time that works for you.");
            return;
        }
        agenda.removeAllViews();
        TextView section = new TextView(this);
        section.setText(selected.size() == 1 ? "YOUR PLAN · 1 SESSION"
                : "YOUR PLAN · " + selected.size() + " SESSIONS");
        section.setTextColor(UiColors.MUTED);
        section.setTextSize(11);
        section.setLetterSpacing(0.08f);
        section.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
        sectionParams.bottomMargin = dp(9);
        agenda.addView(section, sectionParams);
        for (ScheduleOccurrence occurrence : selected) addOccurrenceCard(occurrence);
    }

    private void showEmptyState(String title, String subtitle) {
        agenda.removeAllViews();
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(20));
        card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(1));
        card.setCardElevation(0);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(22), dp(25), dp(22), dp(24));
        TextView icon = new TextView(this);
        icon.setText("＋");
        icon.setTextColor(UiColors.ORANGE_DARK);
        icon.setTextSize(25);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(UiColors.ORANGE_TINT, dp(28)));
        content.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(UiColors.GRAPHITE);
        heading.setTextSize(16);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(12);
        content.addView(heading, headingParams);
        TextView message = new TextView(this);
        message.setText(subtitle);
        message.setTextColor(UiColors.MUTED);
        message.setTextSize(13);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(5);
        content.addView(message, messageParams);
        card.addView(content);
        agenda.addView(card);
        card.setOnClickListener(v -> showAddWorkoutDialog());
    }

    private void renderPreviewAgenda() {
        showEmptyState("Your training week starts here", "Choose a date and plan your first workout.");
    }

    private void addOccurrenceCard(ScheduleOccurrence occurrence) {
        ScheduledWorkout schedule = occurrence.getSchedule();
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(20));
        card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(1));
        card.setCardElevation(dp(1));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        View accent = new View(this);
        int accentColor = occurrence.getStatus() == OccurrenceStatus.COMPLETED
                ? UiColors.SUCCESS : occurrence.getStatus() == OccurrenceStatus.MISSED
                ? 0xFFB66D58 : UiColors.ORANGE_DARK;
        accent.setBackgroundColor(accentColor);
        shell.addView(accent, new LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(13), dp(13), dp(13));
        shell.addView(body, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(header);
        TextView time = new TextView(this);
        time.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                .format(new Date(occurrence.getStartSeconds() * 1000L)));
        time.setTextColor(UiColors.ORANGE_DARK);
        time.setTextSize(14);
        time.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(time, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView status = new TextView(this);
        status.setText(statusLabel(occurrence.getStatus()));
        status.setTextColor(occurrence.getStatus() == OccurrenceStatus.COMPLETED
                ? UiColors.SUCCESS : occurrence.getStatus() == OccurrenceStatus.MISSED
                ? 0xFF9B4E3D : UiColors.MUTED);
        status.setTextSize(10);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setPadding(dp(9), dp(6), dp(9), dp(6));
        int statusBg = occurrence.getStatus() == OccurrenceStatus.COMPLETED ? UiColors.SUCCESS_TINT
                : occurrence.getStatus() == OccurrenceStatus.MISSED ? 0xFFFFEEE9 : UiColors.SURFACE_ALT;
        status.setBackground(rounded(statusBg, dp(16)));
        header.addView(status);

        TextView workoutName = new TextView(this);
        workoutName.setText(schedule.isCustom() ? findCustomWorkoutName(schedule.getCustomWorkoutId())
                : schedule.getWorkoutName());
        workoutName.setTextColor(UiColors.GRAPHITE);
        workoutName.setTextSize(17);
        workoutName.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(8);
        body.addView(workoutName, nameParams);

        String repeat = schedule.getRecurrence() == null ? "One-time workout"
                : schedule.getRecurrence().describe();
        TextView repeatView = new TextView(this);
        repeatView.setText(repeat + (schedule.isCustom() ? "  ·  CUSTOM" : "  ·  WORKOUT"));
        repeatView.setTextColor(UiColors.MUTED);
        repeatView.setTextSize(11);
        repeatView.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams repeatParams = new LinearLayout.LayoutParams(-1, -2);
        repeatParams.topMargin = dp(3);
        body.addView(repeatView, repeatParams);

        if (occurrence.getStatus() == OccurrenceStatus.MISSED
                || (occurrence.getStatus() == OccurrenceStatus.PENDING
                && occurrence.getStartSeconds() <= System.currentTimeMillis() / 1000L + 12 * 3600L)) {
            TextView complete = new TextView(this);
            complete.setText("Mark as complete  ✓");
            complete.setTextColor(UiColors.SUCCESS);
            complete.setTextSize(12);
            complete.setTypeface(null, android.graphics.Typeface.BOLD);
            complete.setGravity(Gravity.CENTER_VERTICAL);
            complete.setPadding(dp(11), 0, dp(11), 0);
            complete.setBackground(rounded(UiColors.SUCCESS_TINT, dp(10)));
            LinearLayout.LayoutParams completeParams = new LinearLayout.LayoutParams(-2, dp(36));
            completeParams.topMargin = dp(11);
            body.addView(complete, completeParams);
            complete.setOnClickListener(v -> markCompleted(occurrence));
        }

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextColor(UiColors.MUTED);
        more.setTextSize(21);
        more.setGravity(Gravity.CENTER);
        more.setContentDescription("Workout options");
        more.setBackground(rounded(UiColors.SURFACE_ALT, dp(20)));
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        moreParams.gravity = Gravity.END;
        body.addView(more, moreParams);
        more.setOnClickListener(v -> showOccurrenceMenu(more, occurrence));

        card.addView(shell);
        card.setContentDescription(schedule.isCustom() ? "Scheduled custom workout" :
                "Scheduled workout " + schedule.getWorkoutName());
        card.setOnClickListener(v -> openWorkout(schedule));
        agenda.addView(card);
    }

    private void showOccurrenceMenu(View anchor, ScheduleOccurrence occurrence) {
        String[] options = {"View workout", "Change date and time", "Change repeat",
                "Add to phone calendar", "Email invite", "Delete plan"};
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, anchor);
        for (int i = 0; i < options.length; i++) menu.getMenu().add(0, i, i, options[i]);
        menu.setOnMenuItemClickListener(item -> {
            int selected = item.getItemId();
            if (selected == 0) openWorkout(occurrence.getSchedule());
            else if (selected == 1) reschedule(occurrence);
            else if (selected == 2) editRecurrence(occurrence.getSchedule());
            else if (selected == 3)
                addToCalendar(occurrence.getSchedule().getId());
            else if (selected == 4)
                emailInvite(occurrence.getSchedule().getId());
            else if (selected == 5)
                confirmDelete(occurrence.getSchedule());
            else return false;
            return true;
        });
        menu.show();
    }

    private void openWorkout(ScheduledWorkout schedule) {
        if (schedule.isCustom()) {
            Intent intent = new Intent(this, CustomWorkoutDetailActivity.class);
            intent.putExtra(CustomWorkoutDetailActivity.EXTRA_WORKOUT_ID, schedule.getCustomWorkoutId());
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(this, WorkoutDetailActivity.class);
        intent.putExtra(WorkoutListActivity.EXTRA_WORKOUT_NAME, schedule.getWorkoutName());
        intent.putExtra(WorkoutListActivity.EXTRA_PREVIEW_MODE, previewMode);
        startActivity(intent);
    }

    private void editRecurrence(ScheduledWorkout schedule) {
        Spinner repeat = new Spinner(this);
        repeat.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, REPEAT_OPTIONS);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        repeat.setAdapter(adapter);
        Recurrence current = schedule.getRecurrence();
        int selection = current == null ? 0
                : current.getFrequency() == Recurrence.Frequency.DAILY ? 1
                : current.getFrequency() == Recurrence.Frequency.WEEKLY ? 2 : 3;
        repeat.setSelection(selection);
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(22), dp(8), dp(22), dp(4));
        form.addView(repeat, new LinearLayout.LayoutParams(-1, dp(50)));
        new AlertDialog.Builder(this).setTitle("Change repeat")
                .setMessage("This updates the repeating plan and its future dates.")
                .setView(form).setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    Recurrence updated = recurrenceAt(repeat.getSelectedItemPosition());
                    scheduleService.setRecurrence(schedule.getId(), updated, new ServiceCallback<Void>() {
                        @Override public void onSuccess(Void ignored) {
                            loadWeek(); showMessage("Repeat setting updated.");
                        }
                        @Override public void onError(String message) { showMessage(message); }
                    });
                }).show();
    }

    private void markCompleted(ScheduleOccurrence occurrence) {
        scheduleService.markCompleted(occurrence.getSchedule().getId(), occurrence.getStartSeconds(),
                new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void ignored) { showMessage("Workout marked complete."); }
                    @Override public void onError(String message) { showMessage(message); }
                });
    }

    private void confirmDelete(ScheduledWorkout schedule) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this plan?")
                .setMessage(schedule.isRecurring()
                        ? "This removes the repeating workout and its future dates."
                        : "This removes the workout from your schedule.")
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Delete", (dialog, which) -> scheduleService.delete(schedule.getId(),
                        new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void ignored) { showMessage("Plan deleted."); }
                            @Override public void onError(String message) { showMessage(message); }
                        }))
                .show();
    }

    private void reschedule(ScheduleOccurrence occurrence) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(occurrence.getStartSeconds() * 1000L);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar updated = (Calendar) initial.clone();
            updated.set(year, month, day);
            new TimePickerDialog(this, (timePicker, hour, minute) -> {
                updated.set(Calendar.HOUR_OF_DAY, hour);
                updated.set(Calendar.MINUTE, minute);
                updated.set(Calendar.SECOND, 0);
                updated.set(Calendar.MILLISECOND, 0);
                scheduleService.reschedule(occurrence.getSchedule().getId(),
                        updated.getTimeInMillis() / 1000L, new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void ignored) {
                                selectedDay.setTimeInMillis(updated.getTimeInMillis());
                                selectedDay.set(Calendar.HOUR_OF_DAY, 0);
                                selectedDay.set(Calendar.MINUTE, 0);
                                selectedDay.set(Calendar.SECOND, 0);
                                selectedDay.set(Calendar.MILLISECOND, 0);
                                renderWeek();
                                loadWeek();
                                showMessage("Workout rescheduled.");
                            }
                            @Override public void onError(String message) { showMessage(message); }
                        });
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addToCalendar(String scheduleId) {
        if (scheduleService.hasCalendarPermission()) {
            scheduleService.addToCalendar(scheduleId, new ServiceCallback<Long>() {
                @Override public void onSuccess(Long id) { showMessage("Added to your phone calendar."); }
                @Override public void onError(String message) { showMessage(message); }
            });
        } else {
            pendingCalendarScheduleId = scheduleId;
            calendarPermissionLauncher.launch(ScheduleService.CALENDAR_PERMISSIONS);
        }
    }

    private void emailInvite(String scheduleId) {
        scheduleService.buildEmail(scheduleId, true, new ServiceCallback<Intent>() {
            @Override public void onSuccess(Intent intent) {
                try { startActivity(intent); }
                catch (android.content.ActivityNotFoundException e) { showMessage("No email app is available."); }
            }
            @Override public void onError(String message) { showMessage(message); }
        });
    }

    private void onCalendarPermissionResult(Map<String, Boolean> result) {
        boolean granted = true;
        for (String permission : ScheduleService.CALENDAR_PERMISSIONS) {
            granted &= Boolean.TRUE.equals(result.get(permission));
        }
        if (pendingCalendarScheduleId != null) {
            String id = pendingCalendarScheduleId;
            pendingCalendarScheduleId = null;
            if (granted) addToCalendar(id);
            else showMessage("Plan saved. Calendar access was not granted.");
        } else if (pendingChoice != null) {
            WorkoutOption choice = pendingChoice;
            long at = pendingScheduledAt;
            Recurrence recurrence = pendingRecurrence;
            pendingChoice = null;
            createSchedule(choice, at, recurrence, granted);
            if (!granted) showMessage("Plan saved without adding it to your phone calendar.");
        }
    }

    private void loadWorkoutChoices() {
        if (previewMode || loadingWorkoutChoices) return;
        loadingWorkoutChoices = true;
        workoutOptions.clear();
        AtomicInteger pending = new AtomicInteger(2);
        Runnable finished = () -> {
            if (pending.decrementAndGet() == 0) {
                loadingWorkoutChoices = false;
                workoutChoicesLoaded = true;
            }
        };
        workoutService.getAllWorkouts(new ServiceCallback<List<Workout>>() {
            @Override public void onSuccess(List<Workout> result) {
                for (Workout workout : result) workoutOptions.add(new WorkoutOption(workout.getName(), null, false));
                finished.run();
            }
            @Override public void onError(String message) { finished.run(); }
        });
        customWorkoutService.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
            @Override public void onSuccess(List<CustomWorkout> result) {
                for (CustomWorkout workout : result) {
                    workoutOptions.add(new WorkoutOption(workout.getName(), workout.getId(), true));
                }
                finished.run();
            }
            @Override public void onError(String message) { finished.run(); }
        });
    }

    private void showAddWorkoutDialog() {
        if (previewMode) {
            showMessage("Sign in to add workouts to your schedule.");
            return;
        }
        if (!workoutChoicesLoaded) {
            loadWorkoutChoices();
            showMessage("Loading your workouts. Try again in a moment.");
            return;
        }
        if (workoutOptions.isEmpty()) {
            showMessage("Create or choose a workout before planning a session.");
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(8), dp(22), dp(4));
        addFormLabel(form, "WORKOUT");
        Spinner workoutSpinner = new Spinner(this);
        workoutSpinner.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<WorkoutOption> workoutAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, workoutOptions);
        workoutAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        workoutSpinner.setAdapter(workoutAdapter);
        form.addView(workoutSpinner, new LinearLayout.LayoutParams(-1, dp(48)));

        addFormLabel(form, "DATE AND TIME");
        Calendar chosen = (Calendar) selectedDay.clone();
        chosen.set(Calendar.HOUR_OF_DAY, 18);
        chosen.set(Calendar.MINUTE, 0);
        chosen.set(Calendar.SECOND, 0);
        chosen.set(Calendar.MILLISECOND, 0);
        TextView dateTimeButton = new TextView(this);
        dateTimeButton.setText(formatDateTime(chosen));
        dateTimeButton.setTextColor(UiColors.GRAPHITE);
        dateTimeButton.setTextSize(14);
        dateTimeButton.setGravity(Gravity.CENTER_VERTICAL);
        dateTimeButton.setPadding(dp(12), 0, dp(12), 0);
        dateTimeButton.setBackground(rounded(UiColors.CANVAS, dp(12)));
        form.addView(dateTimeButton, new LinearLayout.LayoutParams(-1, dp(48)));
        dateTimeButton.setOnClickListener(v -> pickDateTime(chosen, dateTimeButton));

        addFormLabel(form, "REPEAT");
        Spinner repeatSpinner = new Spinner(this);
        repeatSpinner.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, REPEAT_OPTIONS);
        repeatAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        repeatSpinner.setAdapter(repeatAdapter);
        form.addView(repeatSpinner, new LinearLayout.LayoutParams(-1, dp(48)));

        CheckBox calendarCheck = new CheckBox(this);
        calendarCheck.setText("Add to my phone calendar");
        calendarCheck.setTextColor(UiColors.MUTED);
        calendarCheck.setTextSize(13);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(-1, -2);
        checkParams.topMargin = dp(8);
        form.addView(calendarCheck, checkParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Plan a workout")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save plan", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    WorkoutOption choice = (WorkoutOption) workoutSpinner.getSelectedItem();
                    if (choice == null) { showMessage("Choose a workout first."); return; }
                    Recurrence recurrence = recurrenceAt(repeatSpinner.getSelectedItemPosition());
                    boolean addToCalendar = calendarCheck.isChecked();
                    if (addToCalendar && !scheduleService.hasCalendarPermission()) {
                        pendingChoice = choice;
                        pendingScheduledAt = chosen.getTimeInMillis() / 1000L;
                        pendingRecurrence = recurrence;
                        calendarPermissionLauncher.launch(ScheduleService.CALENDAR_PERMISSIONS);
                    } else {
                        createSchedule(choice, chosen.getTimeInMillis() / 1000L, recurrence, addToCalendar);
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void createSchedule(WorkoutOption choice, long at, Recurrence recurrence, boolean addToCalendar) {
        if (at <= System.currentTimeMillis() / 1000L) {
            showMessage("Choose a future date and time.");
            return;
        }
        ServiceCallback<String> callback = new ServiceCallback<String>() {
            @Override public void onSuccess(String id) {
                selectedDay.setTimeInMillis(at * 1000L);
                selectedDay.set(Calendar.HOUR_OF_DAY, 0);
                selectedDay.set(Calendar.MINUTE, 0);
                selectedDay.set(Calendar.SECOND, 0);
                selectedDay.set(Calendar.MILLISECOND, 0);
                renderWeek();
                loadWeek();
                showMessage("Workout added to your schedule.");
            }
            @Override public void onError(String message) { showMessage(message); }
        };
        if (choice.custom) scheduleService.scheduleCustom(choice.id, at, recurrence, addToCalendar, callback);
        else scheduleService.schedulePremade(choice.name, at, recurrence, addToCalendar, callback);
    }

    private void pickDateTime(Calendar chosen, TextView button) {
        new DatePickerDialog(this, (picker, year, month, day) -> {
            chosen.set(Calendar.YEAR, year);
            chosen.set(Calendar.MONTH, month);
            chosen.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (timePicker, hour, minute) -> {
                chosen.set(Calendar.HOUR_OF_DAY, hour);
                chosen.set(Calendar.MINUTE, minute);
                chosen.set(Calendar.SECOND, 0);
                chosen.set(Calendar.MILLISECOND, 0);
                button.setText(formatDateTime(chosen));
            }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), false).show();
        }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addFormLabel(LinearLayout form, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(UiColors.MUTED);
        label.setTextSize(10);
        label.setLetterSpacing(0.08f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        params.bottomMargin = dp(4);
        form.addView(label, params);
    }

    private Recurrence recurrenceAt(int position) {
        switch (position) {
            case 1: return Recurrence.daily();
            case 2: return Recurrence.weekly();
            case 3: return Recurrence.monthly();
            default: return null;
        }
    }

    private String findCustomWorkoutName(String id) {
        for (WorkoutOption option : workoutOptions) {
            if (option.custom && option.id.equals(id)) return option.name;
        }
        return "Custom workout";
    }

    private boolean hasOccurrenceOn(Calendar day) {
        for (ScheduleOccurrence occurrence : weekOccurrences) {
            if (sameDay(occurrence.getStartSeconds(), day)) return true;
        }
        return false;
    }

    private boolean sameDay(long seconds, Calendar day) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(seconds * 1000L);
        return sameDay(date, day);
    }

    private boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isToday(Calendar day) {
        return sameDay(day, Calendar.getInstance());
    }

    private Calendar weekStart(Calendar day) {
        Calendar start = startOfDay((Calendar) day.clone());
        int difference = (start.get(Calendar.DAY_OF_WEEK) - start.getFirstDayOfWeek() + 7) % 7;
        start.add(Calendar.DAY_OF_MONTH, -difference);
        return start;
    }

    private static Calendar startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private String formatDateTime(Calendar calendar) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(calendar.getTime());
    }

    private String statusLabel(OccurrenceStatus status) {
        if (status == OccurrenceStatus.COMPLETED) return "COMPLETED";
        if (status == OccurrenceStatus.MISSED) return "MISSED";
        return "PLANNED";
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.scheduleRoot), message, Snackbar.LENGTH_LONG).show();
    }

    private final class WorkoutOption {
        final String name;
        final String id;
        final boolean custom;
        WorkoutOption(String name, String id, boolean custom) {
            this.name = name;
            this.id = id;
            this.custom = custom;
        }
        @Override public String toString() { return name + (custom ? " · My workout" : ""); }
    }
}
