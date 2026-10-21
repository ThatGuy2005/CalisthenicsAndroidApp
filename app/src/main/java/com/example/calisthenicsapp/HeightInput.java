package com.example.calisthenicsapp;

import android.view.View;
import android.widget.EditText;

import com.example.calisthenicsapp.CoreComponents.UnitSystem;
import com.example.calisthenicsapp.CoreComponents.Units;

/**
 * The optional height of sign-up and the profile (FR-1.7): one field in cm, or in imperial two
 * fields for whole feet and inches. Gives the height in cm, which the services take.
 */
final class HeightInput {
    private final EditText cm;
    private final EditText feet;
    private final EditText inches;
    private final boolean imperial;

    // What show() filled in. Whole inches are coarser than cm, so an imperial height the user
    // did not change gives back the stored cm (see Units).
    private Double shownCm;
    private String shownFeet;
    private String shownInches;

    HeightInput(EditText cm, View imperialRow, EditText feet, EditText inches, UnitSystem system) {
        this.cm = cm;
        this.feet = feet;
        this.inches = inches;
        imperial = system == UnitSystem.IMPERIAL;
        cm.setVisibility(imperial ? View.GONE : View.VISIBLE);
        imperialRow.setVisibility(imperial ? View.VISIBLE : View.GONE);
    }

    /** Fills the fields with a stored height; null or 0 (not set) leaves them empty. */
    void show(Double storedCm) {
        if (storedCm == null || storedCm <= 0) return;
        if (imperial) {
            Units.FeetInches value = Units.cmToFeetInches(storedCm);
            shownFeet = String.valueOf(value.getFeet());
            shownInches = String.valueOf(value.getInches());
            feet.setText(shownFeet);
            inches.setText(shownInches);
        } else {
            cm.setText(storedCm == Math.rint(storedCm)
                    ? String.valueOf(storedCm.intValue()) : String.valueOf(storedCm));
        }
        shownCm = storedCm;
    }

    boolean isEmpty() {
        return imperial ? text(feet).isEmpty() && text(inches).isEmpty() : text(cm).isEmpty();
    }

    /** The typed height in cm; null when the fields are empty or hold no number. */
    Double toCm() {
        if (isEmpty()) return null;
        try {
            if (!imperial) return Double.parseDouble(text(cm).replace(',', '.'));
            if (text(feet).equals(shownFeet) && text(inches).equals(shownInches)) return shownCm;
            int wholeFeet = text(feet).isEmpty() ? 0 : Integer.parseInt(text(feet));
            int wholeInches = text(inches).isEmpty() ? 0 : Integer.parseInt(text(inches));
            return Units.feetInchesToCm(wholeFeet, wholeInches);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** The field an error message goes on. */
    EditText errorField() {
        return imperial ? feet : cm;
    }

    void setEnabled(boolean enabled) {
        cm.setEnabled(enabled);
        feet.setEnabled(enabled);
        inches.setEnabled(enabled);
    }

    private static String text(EditText field) {
        return field.getText().toString().trim();
    }
}
