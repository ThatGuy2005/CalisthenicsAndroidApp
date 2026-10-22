package com.example.calisthenicsapp;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The bottom bar shared by the five tab screens: Home, Workouts, Exercises, Schedule and History.
 *
 * Home stays open underneath while another tab is shown; the other tabs close when you leave
 * them, so Back from any tab goes to Home. Going to Home brings back the Home already open
 * instead of starting a second one.
 */
final class BottomNav {

    private BottomNav() {
    }

    /** Sets up the bar on a tab screen. selectedItemId is the screen's own tab, e.g. R.id.nav_home. */
    static void attach(Activity activity, int selectedItemId, boolean previewMode) {
        BottomNavigationView navigation = activity.findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(selectedItemId);
        navigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedItemId) return true;
            if (!open(activity, itemId, previewMode)) return false;
            if (selectedItemId != R.id.nav_home) activity.finish();
            // Not selected here: Home stays open and keeps its own tab highlighted.
            return false;
        });
    }

    /** Opens the screen of a tab, e.g. from a shortcut on Home. False for an unknown item. */
    static boolean open(Activity activity, int itemId, boolean previewMode) {
        Class<? extends Activity> screen = screenOf(itemId);
        if (screen == null) return false;
        Intent intent = new Intent(activity, screen)
                .putExtra(HomeActivity.EXTRA_PREVIEW_MODE, previewMode);
        if (screen == HomeActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        activity.startActivity(intent);
        return true;
    }

    private static Class<? extends Activity> screenOf(int itemId) {
        // Resource ids are not constants in this project, so no switch.
        if (itemId == R.id.nav_home) return HomeActivity.class;
        if (itemId == R.id.nav_workouts) return WorkoutListActivity.class;
        if (itemId == R.id.nav_exercises) return ExerciseListActivity.class;
        if (itemId == R.id.nav_schedule) return ScheduleActivity.class;
        if (itemId == R.id.nav_history) return HistoryActivity.class;
        return null;
    }
}
