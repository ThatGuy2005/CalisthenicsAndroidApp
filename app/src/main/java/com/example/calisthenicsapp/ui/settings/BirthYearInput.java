package com.example.calisthenicsapp.ui.settings;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.validation.UserValidator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The optional birth year of sign-up as a dropdown: "Select birth year" (not given), then every
 * year UserValidator accepts, this year first.
 */
public final class BirthYearInput {
    private final Spinner spinner;
    /** The year of the first item after the placeholder. */
    private final int newestYear;

    /** Fills the spinner with the years, the placeholder selected. */
    public BirthYearInput(Spinner spinner) {
        this.spinner = spinner;
        newestYear = Calendar.getInstance().get(Calendar.YEAR);
        Context context = spinner.getContext();
        List<String> items = new ArrayList<>();
        items.add(context.getString(R.string.birth_year_none));
        for (int year = newestYear; year >= UserValidator.BIRTH_YEAR_MIN; year--) {
            items.add(String.valueOf(year));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                R.layout.spinner_item_selected, items);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinner.setPopupBackgroundResource(R.color.performance_surface);
        spinner.setAdapter(adapter);
    }

    /** The chosen year, or null when the placeholder is selected. */
    public Integer getYear() {
        int position = spinner.getSelectedItemPosition();
        return position <= 0 ? null : newestYear - position + 1;
    }
}
