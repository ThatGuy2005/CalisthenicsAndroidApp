package com.example.calisthenicsapp;

import static com.example.calisthenicsapp.Views.dp;
import static com.example.calisthenicsapp.Views.rounded;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_PREVIEW_MODE = HomeActivity.EXTRA_PREVIEW_MODE;

    private final ScheduleWeek week = new ScheduleWeek();
    private final List<WorkoutOption> workoutOptions = new ArrayList<>();
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
    private boolean reloadWorkoutChoices;
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
            week.selectToday();
            renderWeek();
            loadWeek();
        });
        findViewById(R.id.scheduleAddButton).setOnClickListener(v -> showAddWorkoutDialog());

        BottomNav.attach(this, R.id.nav_schedule, previewMode);

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

    private void changeWeek(int offset) {
        week.moveWeeks(offset);
        renderWeek();
        loadWeek();
    }

    private void renderWeek() {
        Calendar selectedDay = week.getSelectedDay();
        monthTitle.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDay.getTime()));
        selectedDateTitle.setText(new SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                .format(selectedDay.getTime()));
        daySubtitle.setText(week.isTodaySelected() ? R.string.schedule_today : R.string.schedule_day_subtitle);
        weekDays.removeAllViews();

        List<Calendar> days = week.days();
        for (int i = 0; i < days.size(); i++) {
            Calendar day = days.get(i);
            boolean selected = week.isSelected(day);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(rounded(selected ? UiColors.ORANGE : Color.TRANSPARENT, dp(this, 15)));
            // Keep the two-letter weekday labels inside narrow tiles on small screens.
            tile.setPadding(dp(this, 3), dp(this, 7), dp(this, 3), dp(this, 5));
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
            numberParams.topMargin = dp(this, 4);
            tile.addView(dayNumber, numberParams);

            View dot = new View(this);
            dot.setBackground(rounded(selected ? 0xFFC35CFF : 0xFFD08BFF, dp(this, 4)));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(this, 4), dp(this, 4));
            dotParams.topMargin = dp(this, 4);
            dot.setVisibility(week.hasOccurrenceOn(day) ? View.VISIBLE : View.INVISIBLE);
            tile.addView(dot, dotParams);

            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(this, 72), 1f);
            if (i > 0) tileParams.leftMargin = dp(this, 2);
            weekDays.addView(tile, tileParams);
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setOnClickListener(v -> {
                week.select(day.getTimeInMillis());
                renderWeek();
                renderAgendaForSelectedDay();
            });
        }
    }

    private void loadWeek() {
        scheduleService.getOccurrences(week.fromSeconds(), week.toSeconds(),
                new ServiceCallback<List<ScheduleOccurrence>>() {
                    @Override public void onSuccess(List<ScheduleOccurrence> result) {
                        week.setOccurrences(result);
                        renderWeek();
                        renderAgendaForSelectedDay();
                    }
                    @Override public void onError(String message) {
                        week.setOccurrences(Collections.emptyList());
                        renderWeek();
                        showEmptyState(getString(R.string.schedule_load_failed), message);
                    }
                });
    }

    private void renderAgendaForSelectedDay() {
        List<ScheduleOccurrence> selected = week.occurrencesOnSelectedDay();
        if (selected.isEmpty()) {
            showEmptyState(getString(R.string.schedule_empty_title),
                    getString(R.string.schedule_empty_message));
            return;
        }
        agenda.removeAllViews();
        TextView section = new TextView(this);
        section.setText(getResources().getQuantityString(R.plurals.schedule_plan_count,
                selected.size(), selected.size()));
        section.setTextColor(UiColors.MUTED);
        section.setTextSize(11);
        section.setLetterSpacing(0.08f);
        section.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
        sectionParams.bottomMargin = dp(this, 9);
        agenda.addView(section, sectionParams);
        for (ScheduleOccurrence occurrence : selected) addOccurrenceCard(occurrence);
    }

    private void showEmptyState(String title, String subtitle) {
        agenda.removeAllViews();
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(this, 20));
        card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(this, 1));
        card.setCardElevation(0);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(this, 22), dp(this, 25), dp(this, 22), dp(this, 24));
        TextView icon = new TextView(this);
        icon.setText("＋");
        icon.setTextColor(UiColors.ORANGE_DARK);
        icon.setTextSize(25);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(UiColors.ORANGE_TINT, dp(this, 28)));
        content.addView(icon, new LinearLayout.LayoutParams(dp(this, 50), dp(this, 50)));
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(UiColors.GRAPHITE);
        heading.setTextSize(16);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(this, 12);
        content.addView(heading, headingParams);
        TextView message = new TextView(this);
        message.setText(subtitle);
        message.setTextColor(UiColors.MUTED);
        message.setTextSize(13);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(this, 5);
        content.addView(message, messageParams);
        card.addView(content);
        agenda.addView(card);
        card.setOnClickListener(v -> showAddWorkoutDialog());
    }

    private void renderPreviewAgenda() {
        showEmptyState(getString(R.string.schedule_preview_title), getString(R.string.schedule_preview_message));
    }

    private void addOccurrenceCard(ScheduleOccurrence occurrence) {
        ScheduledWorkout schedule = occurrence.getSchedule();
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(this, 10);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(this, 20));
        card.setCardBackgroundColor(UiColors.SURFACE);
        card.setStrokeColor(UiColors.BORDER);
        card.setStrokeWidth(dp(this, 1));
        card.setCardElevation(dp(this, 1));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        View accent = new View(this);
        int accentColor = occurrence.getStatus() == OccurrenceStatus.COMPLETED
                ? UiColors.SUCCESS : occurrence.getStatus() == OccurrenceStatus.MISSED
                ? 0xFFB66D58 : UiColors.ORANGE_DARK;
        accent.setBackgroundColor(accentColor);
        shell.addView(accent, new LinearLayout.LayoutParams(dp(this, 4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(this, 14), dp(this, 13), dp(this, 13), dp(this, 13));
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
        status.setPadding(dp(this, 9), dp(this, 6), dp(this, 9), dp(this, 6));
        int statusBg = occurrence.getStatus() == OccurrenceStatus.COMPLETED ? UiColors.SUCCESS_TINT
                : occurrence.getStatus() == OccurrenceStatus.MISSED ? 0xFFFFEEE9 : UiColors.SURFACE_ALT;
        status.setBackground(rounded(statusBg, dp(this, 16)));
        header.addView(status);

        TextView workoutName = new TextView(this);
        workoutName.setText(schedule.isCustom() ? findCustomWorkoutName(schedule.getCustomWorkoutId())
                : schedule.getWorkoutName());
        workoutName.setTextColor(UiColors.GRAPHITE);
        workoutName.setTextSize(17);
        workoutName.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(this, 8);
        body.addView(workoutName, nameParams);

        String repeat = schedule.getRecurrence() == null ? getString(R.string.schedule_one_time)
                : schedule.getRecurrence().describe();
        TextView repeatView = new TextView(this);
        repeatView.setText(getString(schedule.isCustom()
                ? R.string.schedule_repeat_custom : R.string.schedule_repeat_workout, repeat));
        repeatView.setTextColor(UiColors.MUTED);
        repeatView.setTextSize(11);
        repeatView.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams repeatParams = new LinearLayout.LayoutParams(-1, -2);
        repeatParams.topMargin = dp(this, 3);
        body.addView(repeatView, repeatParams);

        if (occurrence.getStatus() == OccurrenceStatus.MISSED
                || (occurrence.getStatus() == OccurrenceStatus.PENDING
                && occurrence.getStartSeconds() <= System.currentTimeMillis() / 1000L + 12 * 3600L)) {
            TextView complete = new TextView(this);
            complete.setText(R.string.schedule_mark_complete);
            complete.setTextColor(UiColors.SUCCESS);
            complete.setTextSize(12);
            complete.setTypeface(null, android.graphics.Typeface.BOLD);
            complete.setGravity(Gravity.CENTER_VERTICAL);
            complete.setPadding(dp(this, 11), 0, dp(this, 11), 0);
            complete.setBackground(rounded(UiColors.SUCCESS_TINT, dp(this, 10)));
            LinearLayout.LayoutParams completeParams = new LinearLayout.LayoutParams(-2, dp(this, 36));
            completeParams.topMargin = dp(this, 11);
            body.addView(complete, completeParams);
            complete.setOnClickListener(v -> markCompleted(occurrence));
        }

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextColor(UiColors.MUTED);
        more.setTextSize(21);
        more.setGravity(Gravity.CENTER);
        more.setContentDescription(getString(R.string.schedule_options));
        more.setBackground(rounded(UiColors.SURFACE_ALT, dp(this, 20)));
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(this, 32), dp(this, 32));
        moreParams.gravity = Gravity.END;
        body.addView(more, moreParams);
        more.setOnClickListener(v -> showOccurrenceMenu(more, occurrence));

        card.addView(shell);
        card.setContentDescription(schedule.isCustom() ? getString(R.string.schedule_card_custom) :
                getString(R.string.schedule_card_workout, schedule.getWorkoutName()));
        card.setOnClickListener(v -> openWorkout(schedule));
        agenda.addView(card);
    }

    private void showOccurrenceMenu(View anchor, ScheduleOccurrence occurrence) {
        String[] options = getResources().getStringArray(R.array.schedule_occurrence_actions);
        android.widget.PopupMenu menu = new android.widget.PopupMenu(
                new android.view.ContextThemeWrapper(this, R.style.ThemeOverlay_CalisthenicsApp_Menu), anchor);
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
                R.layout.spinner_item_selected, getResources().getStringArray(R.array.schedule_repeat_options));
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        repeat.setAdapter(adapter);
        Recurrence current = schedule.getRecurrence();
        int selection = current == null ? 0
                : current.getFrequency() == Recurrence.Frequency.DAILY ? 1
                : current.getFrequency() == Recurrence.Frequency.WEEKLY ? 2 : 3;
        repeat.setSelection(selection);
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(this, 22), dp(this, 8), dp(this, 22), dp(this, 4));
        form.addView(repeat, new LinearLayout.LayoutParams(-1, dp(this, 50)));
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.schedule_repeat_title)
                .setMessage(R.string.schedule_repeat_message)
                .setView(form).setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_save, (dialog, which) -> {
                    Recurrence updated = recurrenceAt(repeat.getSelectedItemPosition());
                    scheduleService.setRecurrence(schedule.getId(), updated, new ServiceCallback<Void>() {
                        @Override public void onSuccess(Void ignored) {
                            loadWeek(); showMessage(getString(R.string.schedule_repeat_updated));
                        }
                        @Override public void onError(String message) { showMessage(message); }
                    });
                }).show();
    }

    private void markCompleted(ScheduleOccurrence occurrence) {
        scheduleService.markCompleted(occurrence.getSchedule().getId(), occurrence.getStartSeconds(),
                new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void ignored) { showMessage(getString(R.string.schedule_marked_complete)); }
                    @Override public void onError(String message) { showMessage(message); }
                });
    }

    private void confirmDelete(ScheduledWorkout schedule) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog)
                .setTitle(R.string.schedule_delete_title)
                .setMessage(schedule.isRecurring()
                        ? R.string.schedule_delete_message_repeating
                        : R.string.schedule_delete_message_once)
                .setNegativeButton(R.string.schedule_delete_keep, null)
                .setPositiveButton(R.string.common_delete, (dialog, which) -> scheduleService.delete(schedule.getId(),
                        new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void ignored) { showMessage(getString(R.string.schedule_deleted)); }
                            @Override public void onError(String message) { showMessage(message); }
                        }))
                .show();
    }

    private void reschedule(ScheduleOccurrence occurrence) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(occurrence.getStartSeconds() * 1000L);
        new DatePickerDialog(this, R.style.ThemeOverlay_CalisthenicsApp_Picker, (picker, year, month, day) -> {
            Calendar updated = (Calendar) initial.clone();
            updated.set(year, month, day);
            new TimePickerDialog(this, R.style.ThemeOverlay_CalisthenicsApp_Picker, (timePicker, hour, minute) -> {
                updated.set(Calendar.HOUR_OF_DAY, hour);
                updated.set(Calendar.MINUTE, minute);
                updated.set(Calendar.SECOND, 0);
                updated.set(Calendar.MILLISECOND, 0);
                scheduleService.reschedule(occurrence.getSchedule().getId(),
                        updated.getTimeInMillis() / 1000L, new ServiceCallback<Void>() {
                            @Override public void onSuccess(Void ignored) {
                                week.select(updated.getTimeInMillis());
                                renderWeek();
                                loadWeek();
                                showMessage(getString(R.string.schedule_rescheduled));
                            }
                            @Override public void onError(String message) { showMessage(message); }
                        });
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addToCalendar(String scheduleId) {
        if (scheduleService.hasCalendarPermission()) {
            scheduleService.addToCalendar(scheduleId, new ServiceCallback<Long>() {
                @Override public void onSuccess(Long id) { showMessage(getString(R.string.schedule_added_to_calendar)); }
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
                catch (android.content.ActivityNotFoundException e) { showMessage(getString(R.string.schedule_no_email_app)); }
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
            else showMessage(getString(R.string.schedule_calendar_denied));
        } else if (pendingChoice != null) {
            WorkoutOption choice = pendingChoice;
            long at = pendingScheduledAt;
            Recurrence recurrence = pendingRecurrence;
            pendingChoice = null;
            createSchedule(choice, at, recurrence, granted);
            if (!granted) showMessage(getString(R.string.schedule_saved_without_calendar));
        }
    }

    private void loadWorkoutChoices() {
        if (previewMode) return;
        if (loadingWorkoutChoices) {
            // The load under way may have read the old data, so load again once it is done.
            reloadWorkoutChoices = true;
            return;
        }
        loadingWorkoutChoices = true;
        List<WorkoutOption> premade = new ArrayList<>();
        List<WorkoutOption> custom = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(2);
        Runnable finished = () -> {
            if (pending.decrementAndGet() > 0) return;
            workoutOptions.clear();
            workoutOptions.addAll(premade);
            workoutOptions.addAll(custom);
            loadingWorkoutChoices = false;
            workoutChoicesLoaded = true;
            if (reloadWorkoutChoices) {
                reloadWorkoutChoices = false;
                loadWorkoutChoices();
            }
        };
        workoutService.getAllWorkouts(new ServiceCallback<List<Workout>>() {
            @Override public void onSuccess(List<Workout> result) {
                for (Workout workout : result) premade.add(new WorkoutOption(workout.getName(), null, false));
                finished.run();
            }
            @Override public void onError(String message) { finished.run(); }
        });
        customWorkoutService.getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
            @Override public void onSuccess(List<CustomWorkout> result) {
                for (CustomWorkout workout : result) {
                    custom.add(new WorkoutOption(workout.getName(), workout.getId(), true));
                }
                finished.run();
            }
            @Override public void onError(String message) { finished.run(); }
        });
    }

    private void showAddWorkoutDialog() {
        if (previewMode) {
            showMessage(getString(R.string.schedule_preview_sign_in));
            return;
        }
        if (!workoutChoicesLoaded) {
            loadWorkoutChoices();
            showMessage(getString(R.string.schedule_workouts_loading));
            return;
        }
        if (workoutOptions.isEmpty()) {
            showMessage(getString(R.string.schedule_no_workouts));
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(this, 22), dp(this, 8), dp(this, 22), dp(this, 4));
        addFormLabel(form, R.string.schedule_form_workout);
        Spinner workoutSpinner = new Spinner(this);
        workoutSpinner.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<WorkoutOption> workoutAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, workoutOptions);
        workoutAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        workoutSpinner.setAdapter(workoutAdapter);
        form.addView(workoutSpinner, new LinearLayout.LayoutParams(-1, dp(this, 48)));

        addFormLabel(form, R.string.schedule_form_date_time);
        Calendar chosen = week.getSelectedDay();
        chosen.set(Calendar.HOUR_OF_DAY, 18);
        chosen.set(Calendar.MINUTE, 0);
        chosen.set(Calendar.SECOND, 0);
        chosen.set(Calendar.MILLISECOND, 0);
        TextView dateTimeButton = new TextView(this);
        dateTimeButton.setText(formatDateTime(chosen));
        dateTimeButton.setTextColor(UiColors.GRAPHITE);
        dateTimeButton.setTextSize(14);
        dateTimeButton.setGravity(Gravity.CENTER_VERTICAL);
        dateTimeButton.setPadding(dp(this, 12), 0, dp(this, 12), 0);
        dateTimeButton.setBackground(rounded(UiColors.CANVAS, dp(this, 12)));
        form.addView(dateTimeButton, new LinearLayout.LayoutParams(-1, dp(this, 48)));
        dateTimeButton.setOnClickListener(v -> pickDateTime(chosen, dateTimeButton));

        addFormLabel(form, R.string.schedule_form_repeat);
        Spinner repeatSpinner = new Spinner(this);
        repeatSpinner.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, getResources().getStringArray(R.array.schedule_repeat_options));
        repeatAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        repeatSpinner.setAdapter(repeatAdapter);
        form.addView(repeatSpinner, new LinearLayout.LayoutParams(-1, dp(this, 48)));

        CheckBox calendarCheck = new CheckBox(this);
        calendarCheck.setText(R.string.schedule_add_to_calendar);
        calendarCheck.setTextColor(UiColors.MUTED);
        calendarCheck.setTextSize(13);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(-1, -2);
        checkParams.topMargin = dp(this, 8);
        form.addView(calendarCheck, checkParams);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog)
                .setTitle(R.string.schedule_form_title)
                .setView(form)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.schedule_form_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    WorkoutOption choice = (WorkoutOption) workoutSpinner.getSelectedItem();
                    if (choice == null) { showMessage(getString(R.string.schedule_choose_workout)); return; }
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
            showMessage(getString(R.string.schedule_past_time));
            return;
        }
        ServiceCallback<String> callback = new ServiceCallback<String>() {
            @Override public void onSuccess(String id) {
                week.select(at * 1000L);
                renderWeek();
                loadWeek();
                showMessage(getString(R.string.schedule_added));   // the server emails it (functions/)
            }
            @Override public void onError(String message) { showMessage(message); }
        };
        if (choice.custom) scheduleService.scheduleCustom(choice.id, at, recurrence, addToCalendar, callback);
        else scheduleService.schedulePremade(choice.name, at, recurrence, addToCalendar, callback);
    }

    private void pickDateTime(Calendar chosen, TextView button) {
        new DatePickerDialog(this, R.style.ThemeOverlay_CalisthenicsApp_Picker, (picker, year, month, day) -> {
            chosen.set(Calendar.YEAR, year);
            chosen.set(Calendar.MONTH, month);
            chosen.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, R.style.ThemeOverlay_CalisthenicsApp_Picker, (timePicker, hour, minute) -> {
                chosen.set(Calendar.HOUR_OF_DAY, hour);
                chosen.set(Calendar.MINUTE, minute);
                chosen.set(Calendar.SECOND, 0);
                chosen.set(Calendar.MILLISECOND, 0);
                button.setText(formatDateTime(chosen));
            }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), false).show();
        }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addFormLabel(LinearLayout form, int text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(UiColors.MUTED);
        label.setTextSize(10);
        label.setLetterSpacing(0.08f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(this, 14);
        params.bottomMargin = dp(this, 4);
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
        return getString(R.string.schedule_custom_workout);
    }

    private String formatDateTime(Calendar calendar) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(calendar.getTime());
    }

    private String statusLabel(OccurrenceStatus status) {
        if (status == OccurrenceStatus.COMPLETED) return getString(R.string.schedule_status_completed);
        if (status == OccurrenceStatus.MISSED) return getString(R.string.schedule_status_missed);
        return getString(R.string.schedule_status_planned);
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
        @Override public String toString() {
            return custom ? getString(R.string.schedule_my_workout_option, name) : name;
        }
    }
}
