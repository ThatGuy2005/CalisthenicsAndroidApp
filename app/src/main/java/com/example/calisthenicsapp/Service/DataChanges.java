package com.example.calisthenicsapp.Service;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tells screens when the signed-in user's data has changed, so they can reload it. Changes
 * come from this device's own writes (once they are in SQLite) and from other devices, through
 * SyncManager. Listeners are called on the main thread; by then the services' reads already
 * return the new data.
 *
 * The easiest way for a screen:
 * <pre>
 * DataChanges.observe(this, kind -> loadWorkouts(), DataChanges.Kind.CUSTOM_WORKOUTS);
 * </pre>
 * observe calls the listener once each time the screen starts (so it loads), and again after
 * every change while it is started; it unsubscribes by itself when the screen stops.
 */
public final class DataChanges {

    /** What changed. A listener can watch several. */
    public enum Kind { PROFILE, CUSTOM_WORKOUTS, WORKOUT_LOGS, SCHEDULES }

    public interface Listener {
        /** Called on the main thread. */
        void onDataChanged(Kind kind);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Kind, Set<Listener>> LISTENERS = new EnumMap<>(Kind.class);
    /** Per kind: a notification is already posted and not delivered yet, so another is not needed. */
    private static final Map<Kind, AtomicBoolean> PENDING = new EnumMap<>(Kind.class);

    static {
        for (Kind kind : Kind.values()) {
            LISTENERS.put(kind, new CopyOnWriteArraySet<>());
            PENDING.put(kind, new AtomicBoolean());
        }
    }

    private DataChanges() {
    }

    /** Remember to call {@link #removeListener}; {@link #observe} does that for you. */
    public static void addListener(Listener listener, Kind... kinds) {
        for (Kind kind : kinds) LISTENERS.get(kind).add(listener);
    }

    public static void removeListener(Listener listener) {
        for (Set<Listener> listeners : LISTENERS.values()) listeners.remove(listener);
    }

    /**
     * Calls the listener for each kind whenever the owner (an Activity or Fragment) starts, and
     * after every change of those kinds while it is started. Call it once, e.g. in onCreate.
     */
    public static void observe(LifecycleOwner owner, Listener listener, Kind... kinds) {
        Kind[] watched = kinds.clone();
        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner source) {
                addListener(listener, watched);
                for (Kind kind : watched) listener.onDataChanged(kind);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner source) {
                removeListener(listener);
            }
        });
    }

    /**
     * Reports that data of this kind changed in SQLite. Safe from any thread. Changes that come
     * in before the listeners have been called are delivered as one call.
     */
    public static void notifyChanged(Kind kind) {
        AtomicBoolean pending = PENDING.get(kind);
        if (!pending.compareAndSet(false, true)) return;
        MAIN.post(() -> {
            pending.set(false);
            for (Listener listener : LISTENERS.get(kind)) listener.onDataChanged(kind);
        });
    }

    /** Everything changed, e.g. on sign-in or sign-out. */
    public static void notifyAllChanged() {
        for (Kind kind : Kind.values()) notifyChanged(kind);
    }
}
