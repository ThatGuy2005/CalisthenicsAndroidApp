package com.example.calisthenicsapp.ui.schedule;

import static com.example.calisthenicsapp.ui.common.Views.dp;
import static com.example.calisthenicsapp.ui.common.Views.rounded;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.scheduling.Recurrence;
import com.example.calisthenicsapp.ui.common.UiColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The "Plan a workout" form: which workout, date and time (18:00 on the given day to start
 * with), repeat, whether to add it to the device calendar, and whether to email it to the user
 * (ticked to start with). Saving hands the answers to OnPlanned; asking for calendar permission,
 * saving the schedule and opening the email are the screen's job.
 */
final class PlanWorkoutDialog {
    interface OnPlanned {
        /** at is in epoch seconds; recurrence is null for once. */
        void onPlanned(WorkoutChoices.Option choice, long at, Recurrence recurrence,
                       boolean addToCalendar, boolean email);
    }

    private PlanWorkoutDialog() {
    }

    static void show(Context context, List<WorkoutChoices.Option> options, Calendar day,
                     Consumer<String> showMessage, OnPlanned onPlanned) {
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(context, 22), dp(context, 8), dp(context, 22), dp(context, 4));
        addFormLabel(form, R.string.schedule_form_workout);
        Spinner workoutSpinner = new Spinner(context);
        workoutSpinner.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<WorkoutChoices.Option> workoutAdapter = new ArrayAdapter<>(context,
                R.layout.spinner_item_selected, options);
        workoutAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        workoutSpinner.setAdapter(workoutAdapter);
        form.addView(workoutSpinner, new LinearLayout.LayoutParams(-1, dp(context, 48)));

        addFormLabel(form, R.string.schedule_form_date_time);
        Calendar chosen = (Calendar) day.clone();
        chosen.set(Calendar.HOUR_OF_DAY, 18);
        chosen.set(Calendar.MINUTE, 0);
        chosen.set(Calendar.SECOND, 0);
        chosen.set(Calendar.MILLISECOND, 0);
        TextView dateTimeButton = new TextView(context);
        dateTimeButton.setText(formatDateTime(chosen));
        dateTimeButton.setTextColor(UiColors.GRAPHITE);
        dateTimeButton.setTextSize(14);
        dateTimeButton.setGravity(Gravity.CENTER_VERTICAL);
        dateTimeButton.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        dateTimeButton.setBackground(rounded(UiColors.CANVAS, dp(context, 12)));
        form.addView(dateTimeButton, new LinearLayout.LayoutParams(-1, dp(context, 48)));
        dateTimeButton.setOnClickListener(v -> pickDateTime(context, chosen, dateTimeButton));

        addFormLabel(form, R.string.schedule_form_repeat);
        Spinner repeatSpinner = RepeatOptions.newSpinner(context);
        form.addView(repeatSpinner, new LinearLayout.LayoutParams(-1, dp(context, 48)));

        CheckBox calendarCheck = new CheckBox(context);
        calendarCheck.setText(R.string.schedule_add_to_calendar);
        calendarCheck.setTextColor(UiColors.MUTED);
        calendarCheck.setTextSize(13);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(-1, -2);
        checkParams.topMargin = dp(context, 8);
        form.addView(calendarCheck, checkParams);

        CheckBox emailCheck = new CheckBox(context);
        emailCheck.setText(R.string.schedule_email_me);
        emailCheck.setTextColor(UiColors.MUTED);
        emailCheck.setTextSize(13);
        emailCheck.setChecked(true);
        form.addView(emailCheck, new LinearLayout.LayoutParams(-1, -2));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CalisthenicsApp_Dialog)
                .setTitle(R.string.schedule_form_title)
                .setView(form)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.schedule_form_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    WorkoutChoices.Option choice = (WorkoutChoices.Option) workoutSpinner.getSelectedItem();
                    if (choice == null) {
                        showMessage.accept(context.getString(R.string.schedule_choose_workout));
                        return;
                    }
                    onPlanned.onPlanned(choice, chosen.getTimeInMillis() / 1000L,
                            RepeatOptions.recurrenceAt(repeatSpinner.getSelectedItemPosition()),
                            calendarCheck.isChecked(), emailCheck.isChecked());
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private static void pickDateTime(Context context, Calendar chosen, TextView button) {
        new DatePickerDialog(context, R.style.ThemeOverlay_CalisthenicsApp_Picker, (picker, year, month, day) -> {
            chosen.set(Calendar.YEAR, year);
            chosen.set(Calendar.MONTH, month);
            chosen.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(context, R.style.ThemeOverlay_CalisthenicsApp_Picker, (timePicker, hour, minute) -> {
                chosen.set(Calendar.HOUR_OF_DAY, hour);
                chosen.set(Calendar.MINUTE, minute);
                chosen.set(Calendar.SECOND, 0);
                chosen.set(Calendar.MILLISECOND, 0);
                button.setText(formatDateTime(chosen));
            }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), false).show();
        }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static void addFormLabel(LinearLayout form, int text) {
        Context context = form.getContext();
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(UiColors.MUTED);
        label.setTextSize(10);
        label.setLetterSpacing(0.08f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(context, 14);
        params.bottomMargin = dp(context, 4);
        form.addView(label, params);
    }

    private static String formatDateTime(Calendar calendar) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(calendar.getTime());
    }
}
