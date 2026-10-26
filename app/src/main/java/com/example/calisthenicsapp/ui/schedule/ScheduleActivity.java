package com.example.calisthenicsapp.ui.schedule;

import static com.example.calisthenicsapp.ui.common.Views.dp;
import static com.example.calisthenicsapp.ui.common.Views.rounded;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.model.ScheduledWorkout;
import com.example.calisthenicsapp.scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.scheduling.Recurrence;
import com.example.calisthenicsapp.scheduling.ScheduleOccurrence;
import com.example.calisthenicsapp.service.CustomWorkoutService;
import com.example.calisthenicsapp.service.DataChanges;
import com.example.calisthenicsapp.service.ScheduleService;
import com.example.calisthenicsapp.service.ServiceCallback;
import com.example.calisthenicsapp.service.WorkoutService;
import com.example.calisthenicsapp.ui.common.BottomNav;
import com.example.calisthenicsapp.ui.common.UiColors;
import com.example.calisthenicsapp.ui.home.HomeActivity;
import com.example.calisthenicsapp.ui.workouts.CustomWorkoutDetailActivity;
import com.example.calisthenicsapp.ui.workouts.WorkoutDetailActivity;
import com.example.calisthenicsapp.ui.workouts.WorkoutListActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Schedule tab (FR-6): a week strip with a dot on the days that have a workout, the selected
 * day's planned workouts with their status (FR-6.4), and the "Plan a workout" button. Each card can
 * be completed, opened, rescheduled, given another repeat, added to the calendar (FR-6.2), emailed
 * (FR-6.3) or deleted.
 */
public class ScheduleActivity extends AppCompatActivity {
    /** See HomeActivity.EXTRA_PREVIEW_MODE: an empty week, and planning needs an account. */
    public static final String EXTRA_PREVIEW_MODE = HomeActivity.EXTRA_PREVIEW_MODE;

    /** The selected day, its week and the week's occurrences. */
    private final ScheduleWeek week = new ScheduleWeek();
    private LinearLayout weekDays;
    private LinearLayout agenda;
    private TextView monthTitle;
    private TextView selectedDateTitle;
    private TextView daySubtitle;
    private ScheduleService scheduleService;
    /** The workouts the plan form offers, and the custom workouts' names for the cards. */
    private WorkoutChoices workoutChoices;
    private boolean previewMode;
    /** A schedule to add to the calendar once the permission answer comes. */
    private String pendingCalendarScheduleId;
    /**
     * A new schedule waiting for the calendar permission answer: pendingChoice and the fields after
     * it hold the plan form's answers until then.
     */
    private WorkoutChoices.Option pendingChoice;
    private long pendingScheduledAt;
    private Recurrence pendingRecurrence;
    private boolean pendingEmail;

    /** Reloads the week when schedules change, and the form's choices when custom workouts do. */
    private final DataChanges.Listener dataListener = kind -> {
        if (kind == DataChanges.Kind.SCHEDULES) loadWeek();
        if (kind == DataChanges.Kind.CUSTOM_WORKOUTS) workoutChoices.load();
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
            workoutChoices = new WorkoutChoices(this, new WorkoutService(this),
                    new CustomWorkoutService(this));
            renderWeek();
            DataChanges.observe(this, dataListener,
                    DataChanges.Kind.SCHEDULES, DataChanges.Kind.CUSTOM_WORKOUTS);
        }
    }

    /** Shows the previous (-1) or next (+1) week. */
    private void changeWeek(int offset) {
        week.moveWeeks(offset);
        renderWeek();
        loadWeek();
    }

    /** Draws the month title, the selected date and the seven day tiles. */
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
            // INVISIBLE rather than GONE, so every tile keeps the same height.
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

    /** Loads the week's occurrences, then redraws. */
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

    /** The selected day's workouts, or the empty card that opens the plan form. */
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

    /** A card with a plus, a title and a line of text; tapping it opens the plan form. */
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

    /** What the preview mode shows instead of data. */
    private void renderPreviewAgenda() {
        showEmptyState(getString(R.string.schedule_preview_title), getString(R.string.schedule_preview_message));
    }

    /**
     * One planned workout: the time, its status, the workout, how it repeats, "Mark as complete"
     * when allowed, and the menu.
     */
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
        workoutName.setText(schedule.isCustom() ? workoutChoices.customWorkoutName(schedule.getCustomWorkoutId())
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

        // ScheduleService allows completing from OccurrenceStatus.MISSED_AFTER_SECONDS (12 h)
        // before the start, so the button shows from then on.
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

    /** The card's menu. The item ids are the positions in schedule_occurrence_actions. */
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

    /** Opens the planned workout's detail screen, premade or custom. */
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

    /** Lets the user change how the schedule repeats (FR-6.1). */
    private void editRecurrence(ScheduledWorkout schedule) {
        Spinner repeat = RepeatOptions.newSpinner(this);
        repeat.setSelection(RepeatOptions.positionOf(schedule.getRecurrence()));
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(this, 22), dp(this, 8), dp(this, 22), dp(this, 4));
        form.addView(repeat, new LinearLayout.LayoutParams(-1, dp(this, 50)));
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CalisthenicsApp_Dialog).setTitle(R.string.schedule_repeat_title)
                .setMessage(R.string.schedule_repeat_message)
                .setView(form).setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_save, (dialog, which) -> {
                    Recurrence updated = RepeatOptions.recurrenceAt(repeat.getSelectedItemPosition());
                    scheduleService.setRecurrence(schedule.getId(), updated, new ServiceCallback<Void>() {
                        @Override public void onSuccess(Void ignored) {
                            loadWeek(); showMessage(getString(R.string.schedule_repeat_updated));
                        }
                        @Override public void onError(String message) { showMessage(message); }
                    });
                }).show();
    }

    /** Marks the occurrence as done; the sync then redraws the card. */
    private void markCompleted(ScheduleOccurrence occurrence) {
        scheduleService.markCompleted(occurrence.getSchedule().getId(), occurrence.getStartSeconds(),
                new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void ignored) { showMessage(getString(R.string.schedule_marked_complete)); }
                    @Override public void onError(String message) { showMessage(message); }
                });
    }

    /** Asks first; a repeating schedule is deleted with every occurrence. */
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

    /** Moves the schedule to a new date and time; a repeating one moves as a whole series. */
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

    /** Adds the schedule to the device calendar, asking for the permission first if needed. */
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

    /** Opens the user's email app with the workout email and its .ics invite (FR-6.3). */
    private void emailInvite(String scheduleId) {
        scheduleService.buildEmail(scheduleId, true, new ServiceCallback<Intent>() {
            @Override public void onSuccess(Intent intent) {
                try { startActivity(intent); }
                catch (android.content.ActivityNotFoundException e) { showMessage(getString(R.string.schedule_no_email_app)); }
            }
            @Override public void onError(String message) { showMessage(message); }
        });
    }

    /**
     * Carries on with whatever asked for the calendar permission: an existing schedule, or a new
     * one that is then saved with or without its calendar event.
     */
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
            WorkoutChoices.Option choice = pendingChoice;
            long at = pendingScheduledAt;
            Recurrence recurrence = pendingRecurrence;
            pendingChoice = null;
            createSchedule(choice, at, recurrence, granted, pendingEmail);
            if (!granted) showMessage(getString(R.string.schedule_saved_without_calendar));
        }
    }

    /** Opens the plan form, once the workout choices are loaded. */
    private void showAddWorkoutDialog() {
        if (previewMode) {
            showMessage(getString(R.string.schedule_preview_sign_in));
            return;
        }
        if (!workoutChoices.isLoaded()) {
            workoutChoices.load();
            showMessage(getString(R.string.schedule_workouts_loading));
            return;
        }
        if (workoutChoices.get().isEmpty()) {
            showMessage(getString(R.string.schedule_no_workouts));
            return;
        }

        PlanWorkoutDialog.show(this, workoutChoices.get(), week.getSelectedDay(), this::showMessage,
                (choice, at, recurrence, addToCalendar, email) -> {
                    // Ask first; onCalendarPermissionResult saves the schedule with the answers
                    // kept here.
                    if (addToCalendar && !scheduleService.hasCalendarPermission()) {
                        pendingChoice = choice;
                        pendingScheduledAt = at;
                        pendingRecurrence = recurrence;
                        pendingEmail = email;
                        calendarPermissionLauncher.launch(ScheduleService.CALENDAR_PERMISSIONS);
                    } else {
                        createSchedule(choice, at, recurrence, addToCalendar, email);
                    }
                });
    }

    /** Saves the new schedule, shows its day, and opens the email when that box was ticked. */
    private void createSchedule(WorkoutChoices.Option choice, long at, Recurrence recurrence,
                                boolean addToCalendar, boolean email) {
        if (at <= System.currentTimeMillis() / 1000L) {
            showMessage(getString(R.string.schedule_past_time));
            return;
        }
        ServiceCallback<String> callback = new ServiceCallback<String>() {
            @Override public void onSuccess(String id) {
                week.select(at * 1000L);
                renderWeek();
                loadWeek();
                showMessage(getString(R.string.schedule_added));
                // The user's email app, filled in; the user taps Send (FR-6.3).
                if (email) emailInvite(id);
            }
            @Override public void onError(String message) { showMessage(message); }
        };
        if (choice.isCustom()) scheduleService.scheduleCustom(choice.customWorkoutId, at, recurrence, addToCalendar, callback);
        else scheduleService.schedulePremade(choice.name, at, recurrence, addToCalendar, callback);
    }

    /** The status tag text: completed, missed or planned. */
    private String statusLabel(OccurrenceStatus status) {
        if (status == OccurrenceStatus.COMPLETED) return getString(R.string.schedule_status_completed);
        if (status == OccurrenceStatus.MISSED) return getString(R.string.schedule_status_missed);
        return getString(R.string.schedule_status_planned);
    }

    /** A short message at the bottom. */
    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.scheduleRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
