package com.example.calisthenicsapp.ui.schedule;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.scheduling.Recurrence;

/** The repeat choice of a schedule: Once, Daily, Weekly or Monthly, in that order. */
final class RepeatOptions {
    private RepeatOptions() {
    }

    /** A spinner of the choices, styled like the other dialog spinners. */
    static Spinner newSpinner(Context context) {
        Spinner spinner = new Spinner(context);
        spinner.setPopupBackgroundResource(R.color.performance_surface);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_selected,
                context.getResources().getStringArray(R.array.schedule_repeat_options));
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinner.setAdapter(adapter);
        return spinner;
    }

    /** The rule at a spinner position; null means once. */
    static Recurrence recurrenceAt(int position) {
        switch (position) {
            case 1: return Recurrence.daily();
            case 2: return Recurrence.weekly();
            case 3: return Recurrence.monthly();
            default: return null;
        }
    }

    /**
     * The spinner position of a rule, so a dialog opens on the current one; the inverse of
     * recurrenceAt. A rule with an interval above 1 shows as its plain frequency.
     */
    static int positionOf(Recurrence recurrence) {
        if (recurrence == null) return 0;
        if (recurrence.getFrequency() == Recurrence.Frequency.DAILY) return 1;
        if (recurrence.getFrequency() == Recurrence.Frequency.WEEKLY) return 2;
        return 3;
    }
}
