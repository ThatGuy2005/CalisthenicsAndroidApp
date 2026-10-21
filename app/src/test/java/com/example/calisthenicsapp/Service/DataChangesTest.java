package com.example.calisthenicsapp.Service;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)   // not CalisthenicsApplication: no Firebase or sync here
public class DataChangesTest {
    /** Records every call; removed again after each test, since DataChanges is static. */
    private static final class Recorder implements DataChanges.Listener {
        final List<DataChanges.Kind> calls = new ArrayList<>();

        @Override
        public void onDataChanged(DataChanges.Kind kind) {
            calls.add(kind);
        }
    }

    private static final class Screen implements LifecycleOwner {
        final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return lifecycle;
        }
    }

    private final List<Recorder> recorders = new ArrayList<>();

    private Recorder recorder() {
        Recorder recorder = new Recorder();
        recorders.add(recorder);
        return recorder;
    }

    private static void runMainLooper() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    @After
    public void tearDown() {
        runMainLooper();   // nothing may stay pending for the next test
        for (Recorder recorder : recorders) DataChanges.removeListener(recorder);
    }

    @Test
    public void listenersOfThatKindAreCalledOnTheMainLoop() {
        Recorder schedules = recorder();
        Recorder logs = recorder();
        DataChanges.addListener(schedules, DataChanges.Kind.SCHEDULES);
        DataChanges.addListener(logs, DataChanges.Kind.WORKOUT_LOGS);

        DataChanges.notifyChanged(DataChanges.Kind.SCHEDULES);
        assertEquals(Collections.emptyList(), schedules.calls);   // posted, not called directly
        runMainLooper();
        assertEquals(Collections.singletonList(DataChanges.Kind.SCHEDULES), schedules.calls);
        assertEquals(Collections.emptyList(), logs.calls);
    }

    @Test
    public void changesBeforeDeliveryAreDeliveredOnce() {
        Recorder recorder = recorder();
        DataChanges.addListener(recorder, DataChanges.Kind.CUSTOM_WORKOUTS);
        DataChanges.notifyChanged(DataChanges.Kind.CUSTOM_WORKOUTS);
        DataChanges.notifyChanged(DataChanges.Kind.CUSTOM_WORKOUTS);
        DataChanges.notifyChanged(DataChanges.Kind.CUSTOM_WORKOUTS);
        runMainLooper();
        assertEquals(1, recorder.calls.size());

        DataChanges.notifyChanged(DataChanges.Kind.CUSTOM_WORKOUTS);
        runMainLooper();
        assertEquals(2, recorder.calls.size());
    }

    @Test
    public void changesFromABackgroundThreadArrive() throws InterruptedException {
        Recorder recorder = recorder();
        DataChanges.addListener(recorder, DataChanges.Kind.PROFILE);
        Thread sync = new Thread(() -> DataChanges.notifyChanged(DataChanges.Kind.PROFILE));
        sync.start();
        sync.join();
        runMainLooper();
        assertEquals(Collections.singletonList(DataChanges.Kind.PROFILE), recorder.calls);
    }

    @Test
    public void removedListenersAreNotCalled() {
        Recorder recorder = recorder();
        DataChanges.addListener(recorder, DataChanges.Kind.values());
        DataChanges.removeListener(recorder);
        DataChanges.notifyAllChanged();
        runMainLooper();
        assertEquals(Collections.emptyList(), recorder.calls);
    }

    @Test
    public void observeLoadsOnStartListensWhileStartedAndStopsOnStop() {
        Recorder recorder = recorder();
        Screen screen = new Screen();
        DataChanges.observe(screen, recorder, DataChanges.Kind.SCHEDULES, DataChanges.Kind.CUSTOM_WORKOUTS);
        screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        assertEquals(Collections.emptyList(), recorder.calls);

        screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);   // the first load
        assertEquals(Arrays.asList(DataChanges.Kind.SCHEDULES, DataChanges.Kind.CUSTOM_WORKOUTS),
                recorder.calls);

        DataChanges.notifyChanged(DataChanges.Kind.SCHEDULES);
        runMainLooper();
        assertEquals(3, recorder.calls.size());

        screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        DataChanges.notifyChanged(DataChanges.Kind.SCHEDULES);
        runMainLooper();
        assertEquals(3, recorder.calls.size());

        screen.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);   // back on screen: reload
        assertEquals(5, recorder.calls.size());
    }
}
