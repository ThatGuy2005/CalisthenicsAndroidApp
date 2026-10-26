package com.example.calisthenicsapp.ui.common;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.calisthenicsapp.ui.auth.MainActivity;
import com.example.calisthenicsapp.ui.home.HomeActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Keeps every screen's content off the status bar, the navigation bar, the camera cutout and the
 * keyboard. Registered once in CalisthenicsApplication.
 *
 * The app targets Android 16, and from Android 15 on every screen is drawn edge to edge, under
 * the system bars. This pads each screen's content view by the bars' size instead, so the screens
 * need no code of their own. The padding shows the window background, page_background, which is
 * the background of every screen. The insets are consumed, so views that would pad themselves
 * again (BottomNavigationView) do not. Before Android 15 the window does not reach under the bars,
 * the insets are 0 and nothing changes.
 */
public class SystemBarPadding implements Application.ActivityLifecycleCallbacks {
    /** Screens that call EdgeToEdge.enable and pad themselves. */
    private static final Set<Class<?>> HANDLE_THEIR_OWN = new HashSet<>(Arrays.asList(
            MainActivity.class, HomeActivity.class));

    // onStart: the content view is in place after onCreate's setContentView, and insets only
    // arrive once the window is shown after onResume. Setting the same listener again is harmless.
    @Override public void onActivityStarted(Activity activity) {
        if (HANDLE_THEIR_OWN.contains(activity.getClass())) return;
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            // The open keyboard covers the navigation bar; the screens use adjustResize.
            int bottom = Math.max(bars.bottom,
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            view.setPadding(bars.left, bars.top, bars.right, bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
