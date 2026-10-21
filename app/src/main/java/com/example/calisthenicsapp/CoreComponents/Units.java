package com.example.calisthenicsapp.CoreComponents;

import com.example.calisthenicsapp.Verificate.UserValidator;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Height and weight in the units the user picked (FR-1.7). The services always take and return
 * kg and cm; a screen shows values with the format and *FromKg / cmToFeetInches methods, and
 * converts what the user typed back to kg and cm before calling a service. To check typed values,
 * convert them, ask UserValidator.isValidHeight / isValidWeight, and show
 * {@link #heightError} / {@link #weightError}.
 *
 * Weights are rounded to 0.1 kg and 0.1 lb, so a weight shown in lb and saved unchanged comes
 * back as the same kg. An imperial height is in whole inches, coarser than the stored cm, so
 * convert a height back only if the user changed it. Pure Java, so it is unit-tested on the JVM.
 */
public final class Units {
    /** Exact, by definition. */
    public static final double KG_PER_LB = 0.45359237;
    /** Exact, by definition. */
    public static final double CM_PER_INCH = 2.54;
    public static final int INCHES_PER_FOOT = 12;

    private Units() {
    }

    /** A height in whole feet and inches; inches is 0 to 11. */
    public static final class FeetInches {
        private final int feet;
        private final int inches;

        public FeetInches(int feet, int inches) {
            this.feet = feet;
            this.inches = inches;
        }

        public int getFeet() {
            return feet;
        }

        public int getInches() {
            return inches;
        }

        /** "5 ft 9 in". */
        @Override
        public String toString() {
            return feet + " ft " + inches + " in";
        }
    }

    /** Rounded to 0.1 lb. */
    public static double kgToLb(double kg) {
        return oneDecimal(kg / KG_PER_LB);
    }

    /** Rounded to 0.1 kg. */
    public static double lbToKg(double lb) {
        return oneDecimal(lb * KG_PER_LB);
    }

    /** To the nearest inch. */
    public static FeetInches cmToFeetInches(double cm) {
        return ofInches(Math.round(cm / CM_PER_INCH));
    }

    /** Rounded to 0.1 cm. inches may have a fraction and may be 12 or more. */
    public static double feetInchesToCm(int feet, double inches) {
        return oneDecimal((feet * INCHES_PER_FOOT + inches) * CM_PER_INCH);
    }

    /** A weight typed in the system's unit, in kg for the services. Null stays null. */
    public static Double weightToKg(Double value, UnitSystem system) {
        if (value == null) return null;
        return system == UnitSystem.IMPERIAL ? lbToKg(value) : value;
    }

    /** A stored weight in the system's unit, e.g. for a form field. Null or 0 (not set) gives null. */
    public static Double weightFromKg(Double kg, UnitSystem system) {
        if (!isSet(kg)) return null;
        return system == UnitSystem.IMPERIAL ? kgToLb(kg) : kg;
    }

    /** "70 kg" or "154.3 lb". Null or 0 (not set) gives null. */
    public static String formatWeight(Double kg, UnitSystem system, Locale locale) {
        if (!isSet(kg)) return null;
        return system == UnitSystem.IMPERIAL
                ? number(kgToLb(kg), locale) + " lb" : number(kg, locale) + " kg";
    }

    /** "175 cm" or "5 ft 9 in". Null or 0 (not set) gives null. */
    public static String formatHeight(Double cm, UnitSystem system, Locale locale) {
        if (!isSet(cm)) return null;
        return system == UnitSystem.IMPERIAL
                ? cmToFeetInches(cm).toString() : number(cm, locale) + " cm";
    }

    /**
     * The allowed range in the system's units. In metric it is the back end's own message; the
     * imperial limits are rounded inwards, so both ends shown are accepted.
     */
    public static String heightError(UnitSystem system, Locale locale) {
        double min = UserValidator.HEIGHT_MIN, max = UserValidator.HEIGHT_MAX;
        if (system != UnitSystem.IMPERIAL) {
            return "Height must be between " + number(min, locale) + " and " + number(max, locale) + " cm.";
        }
        return "Height must be between " + ofInches((long) Math.ceil(min / CM_PER_INCH))
                + " and " + ofInches((long) Math.floor(max / CM_PER_INCH)) + ".";
    }

    /** Like {@link #heightError}. */
    public static String weightError(UnitSystem system, Locale locale) {
        double min = UserValidator.WEIGHT_MIN, max = UserValidator.WEIGHT_MAX;
        if (system != UnitSystem.IMPERIAL) {
            return "Weight must be between " + number(min, locale) + " and " + number(max, locale) + " kg.";
        }
        return "Weight must be between " + number(Math.ceil(min / KG_PER_LB * 10) / 10, locale)
                + " and " + number(Math.floor(max / KG_PER_LB * 10) / 10, locale) + " lb.";
    }

    private static FeetInches ofInches(long inches) {
        return new FeetInches((int) (inches / INCHES_PER_FOOT), (int) (inches % INCHES_PER_FOOT));
    }

    /** The profile stores 0 for a height or weight that was never given. */
    private static boolean isSet(Double value) {
        return value != null && value > 0;
    }

    private static double oneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /** At most one decimal, with the locale's decimal separator: "70", "154.3", "154,3". */
    private static String number(double value, Locale locale) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale)).format(value);
    }
}
