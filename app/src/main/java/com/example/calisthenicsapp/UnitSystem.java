package com.example.calisthenicsapp;

import android.content.Context;
import android.content.SharedPreferences;

/** Presentation preference only; profile data stays stored in kg and cm. */
public final class UnitSystem {
    private static final String PREFS = "training_preferences";
    private static final String KEY_IMPERIAL = "imperial_units";
    private static final double KG_TO_LB = 2.2046226218;
    private static final double CM_PER_FOOT = 30.48;

    private UnitSystem() { }

    public static boolean isImperial(Context context) { return prefs(context).getBoolean(KEY_IMPERIAL, false); }
    public static void setImperial(Context context, boolean imperial) {
        prefs(context).edit().putBoolean(KEY_IMPERIAL, imperial).apply();
    }
    public static double weightFromStorage(double kg, Context context) {
        return isImperial(context) ? kg * KG_TO_LB : kg;
    }
    public static double weightToStorage(double value, Context context) {
        return isImperial(context) ? value / KG_TO_LB : value;
    }
    public static double heightFromStorage(double cm, Context context) {
        return isImperial(context) ? cm / CM_PER_FOOT : cm;
    }
    public static double heightToStorage(double value, Context context) {
        return isImperial(context) ? value * CM_PER_FOOT : value;
    }
    public static String weightUnit(Context context) { return isImperial(context) ? "lb" : "kg"; }
    public static String heightUnit(Context context) { return isImperial(context) ? "ft" : "cm"; }
    public static String formatInputValue(double value) {
        return String.format(java.util.Locale.getDefault(), "%.2f", value)
                .replaceAll("([,.]\\d*?)0+$", "$1").replaceAll("[,.]$", "");
    }
    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
