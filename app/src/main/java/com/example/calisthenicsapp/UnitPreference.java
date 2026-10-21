package com.example.calisthenicsapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.calisthenicsapp.CoreComponents.UnitSystem;

/** The units this device shows height and weight in (FR-1.7). A device setting, not synced. */
final class UnitPreference {
    private static final String PREFS = "training_preferences";
    private static final String KEY_UNIT_SYSTEM = "unit_system";

    private UnitPreference() {
    }

    /** METRIC until the user picks another one in Settings. */
    static UnitSystem get(Context context) {
        return UnitSystem.fromName(prefs(context).getString(KEY_UNIT_SYSTEM, null));
    }

    static void set(Context context, UnitSystem system) {
        prefs(context).edit().putString(KEY_UNIT_SYSTEM, system.name()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
