package com.example.calisthenicsapp.Service;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Repos.WorkoutRepository;
import com.example.calisthenicsapp.Scheduling.DeviceCalendar;
import com.example.calisthenicsapp.Scheduling.OccurrenceStatus;
import com.example.calisthenicsapp.Scheduling.Recurrence;
import com.example.calisthenicsapp.Scheduling.ScheduleOccurrence;
import com.example.calisthenicsapp.Scheduling.WorkoutSummary;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * The signed-in user's planned workouts (users/{uid}/schedules). A schedule points at exactly
 * one premade workout (by name) or one custom workout (by id). Times are epoch seconds.
 * A schedule happens once, or repeats by a {@link Recurrence} from its first occurrence on
 * (FR-6.1). Each occurrence is pending, completed or missed ({@link OccurrenceStatus}); only
 * the completed occurrence times are stored, in the document's "completed" array.
 *
 * A schedule can also be an event in the device's calendar (FR-6.2). The event id is kept only
 * in this device's SQLite, see {@link ScheduledWorkout#getCalendarEventId()}. Rescheduling or
 * deleting a schedule, here or on another device, moves or deletes the event too (SyncManager
 * does that when the change arrives). Calendar actions need {@link #CALENDAR_PERMISSIONS},
 * which the screen asks for first (FR-6.5).
 */
public class ScheduleService extends UserDataService {
    private static final String TAG = "ScheduleService";

    /** Ask for these before adding to the calendar, e.g. with RequestMultiplePermissions. */
    public static final String[] CALENDAR_PERMISSIONS = DeviceCalendar.PERMISSIONS;

    private static final String NO_CALENDAR_PERMISSION =
            "Allow calendar access to add workouts to your calendar.";
    private static final String GONE = "That scheduled workout no longer exists.";
    /** getOccurrences covers at most this long a period. */
    private static final long MAX_RANGE_SECONDS = 366 * 86_400L;
    /** Must match file_paths.xml. */
    private static final String ICS_DIR = "ics";

    public ScheduleService(Context context) {
        super(context);
    }

    /** Soonest first. */
    public void getMySchedules(ServiceCallback<List<ScheduledWorkout>> callback) {
        readForUser(callback, uid -> new ScheduledWorkoutRepository(db()).getByUid(uid));
    }

    /**
     * Schedules that still have an occurrence from now on, sorted by their next occurrence
     * (see {@link ScheduledWorkout#nextOccurrence}).
     */
    public void getUpcoming(ServiceCallback<List<ScheduledWorkout>> callback) {
        long from = now();
        TimeZone zone = TimeZone.getDefault();
        readForUser(callback, uid -> {
            List<ScheduledWorkout> upcoming = new ArrayList<>();
            for (ScheduledWorkout s : new ScheduledWorkoutRepository(db()).getUpcoming(uid, from)) {
                if (s.nextOccurrence(from, zone) != null) upcoming.add(s);
            }
            upcoming.sort(Comparator.comparingLong(s -> s.nextOccurrence(from, zone)));
            return upcoming;
        });
    }

    /**
     * Every occurrence in [fromSeconds, toSeconds) with its status, soonest first: what an agenda
     * or calendar screen shows (FR-6.1, FR-6.4). The period can be at most a year long.
     */
    public void getOccurrences(long fromSeconds, long toSeconds,
                               ServiceCallback<List<ScheduleOccurrence>> callback) {
        if (toSeconds <= fromSeconds || toSeconds - fromSeconds > MAX_RANGE_SECONDS) {
            fail(callback, "Pick a period of at most a year.");
            return;
        }
        long nowSeconds = now();
        TimeZone zone = TimeZone.getDefault();
        readForUser(callback, uid -> ScheduleOccurrence.between(
                new ScheduledWorkoutRepository(db()).getBetween(uid, fromSeconds, toSeconds),
                fromSeconds, toSeconds, nowSeconds, zone));
    }

    /**
     * Marks one occurrence as done (FR-6.1); occurrenceSeconds is its
     * {@link ScheduleOccurrence#getStartSeconds()}. Works up to
     * {@link OccurrenceStatus#MISSED_AFTER_SECONDS} before it starts, and any time after.
     */
    public void markCompleted(String scheduleId, long occurrenceSeconds, ServiceCallback<Void> callback) {
        setCompleted(scheduleId, occurrenceSeconds, true, callback);
    }

    /** Undoes markCompleted: the occurrence is pending or missed again. */
    public void markNotCompleted(String scheduleId, long occurrenceSeconds, ServiceCallback<Void> callback) {
        setCompleted(scheduleId, occurrenceSeconds, false, callback);
    }

    public boolean hasCalendarPermission() {
        return DeviceCalendar.hasPermission(context());
    }

    /** On success, returns the new schedule's id. */
    public void schedulePremade(String workoutName, long scheduledAt, ServiceCallback<String> callback) {
        schedulePremade(workoutName, scheduledAt, false, callback);
    }

    /**
     * With addToCalendar, also adds the event if the calendar permission is granted. A failed
     * calendar add does not undo the schedule: its getCalendarEventId() stays null, and
     * {@link #addToCalendar} can try again. On success, returns the new schedule's id.
     */
    public void schedulePremade(String workoutName, long scheduledAt, boolean addToCalendar,
                                ServiceCallback<String> callback) {
        schedulePremade(workoutName, scheduledAt, null, addToCalendar, callback);
    }

    /** Like the other overloads; with a recurrence, scheduledAt is the first occurrence. */
    public void schedulePremade(String workoutName, long scheduledAt, Recurrence recurrence,
                                boolean addToCalendar, ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            if (workoutName == null || new WorkoutRepository(db()).getOne(workoutName) == null) {
                fail(callback, "Unknown workout: " + workoutName);
                return;
            }
            add(uid, workoutName, null, scheduledAt, recurrence, addToCalendar, callback);
        });
    }

    /** On success, returns the new schedule's id. */
    public void scheduleCustom(String customWorkoutId, long scheduledAt, ServiceCallback<String> callback) {
        scheduleCustom(customWorkoutId, scheduledAt, false, callback);
    }

    /** See {@link #schedulePremade(String, long, boolean, ServiceCallback)}. */
    public void scheduleCustom(String customWorkoutId, long scheduledAt, boolean addToCalendar,
                               ServiceCallback<String> callback) {
        scheduleCustom(customWorkoutId, scheduledAt, null, addToCalendar, callback);
    }

    /** Like the other overloads; with a recurrence, scheduledAt is the first occurrence. */
    public void scheduleCustom(String customWorkoutId, long scheduledAt, Recurrence recurrence,
                               boolean addToCalendar, ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            if (customWorkoutId == null
                    || new CustomWorkoutRepository(db()).getOne(customWorkoutId) == null) {
                fail(callback, "That custom workout no longer exists.");
                return;
            }
            add(uid, null, customWorkoutId, scheduledAt, recurrence, addToCalendar, callback);
        });
    }

    /**
     * Moves the schedule; for a repeating one, the whole series moves to repeat from the new
     * first occurrence. The calendar event, if any, moves with it.
     */
    public void reschedule(String scheduleId, long newScheduledAt, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (newScheduledAt <= 0) {
            fail(callback, "Pick a date and time.");
            return;
        }
        onDb(() -> {
            if (scheduleId == null || new ScheduledWorkoutRepository(db()).getOne(scheduleId) == null) {
                fail(callback, GONE);
                return;
            }
            watchWrite(schedules(uid).document(scheduleId).update("scheduledAt", newScheduledAt),
                    "Rescheduling " + scheduleId);
            succeed(callback, null);
        });
    }

    /**
     * Makes the schedule repeat by the recurrence, or with null happen only once (FR-6.1).
     * The first occurrence stays where it is. The calendar event, if any, follows.
     */
    public void setRecurrence(String scheduleId, Recurrence recurrence, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            if (scheduleId == null || new ScheduledWorkoutRepository(db()).getOne(scheduleId) == null) {
                fail(callback, GONE);
                return;
            }
            watchWrite(schedules(uid).document(scheduleId).update("recurrenceRule",
                            recurrence == null ? FieldValue.delete() : recurrence.toRule()),
                    "Changing the repeat of schedule " + scheduleId);
            succeed(callback, null);
        });
    }

    /** The calendar event, if any, is deleted with it. */
    public void delete(String scheduleId, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (scheduleId == null) {
            fail(callback, GONE);
            return;
        }
        watchWrite(schedules(uid).document(scheduleId).delete(), "Deleting schedule " + scheduleId);
        succeed(callback, null);
    }

    /**
     * Adds an existing schedule to the device calendar, or refreshes its event if it is already
     * there (FR-6.2). For a schedule created a moment ago, use the addToCalendar flag of
     * schedulePremade/scheduleCustom instead: the new schedule may not be in SQLite yet.
     * On success, returns the calendar event id.
     */
    public void addToCalendar(String scheduleId, ServiceCallback<Long> callback) {
        if (sessionManager.getCurrentUid() == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (!hasCalendarPermission()) {
            fail(callback, NO_CALENDAR_PERMISSION);
            return;
        }
        onDb(() -> {
            ScheduledWorkoutRepository repository = new ScheduledWorkoutRepository(db());
            ScheduledWorkout schedule = scheduleId == null ? null : repository.getOne(scheduleId);
            WorkoutSummary summary = schedule == null ? null : WorkoutSummary.of(db(), schedule);
            if (summary == null) {
                fail(callback, GONE);
                return;
            }
            Long eventId = DeviceCalendar.putEvent(context(), schedule.getCalendarEventId(), summary);
            if (eventId == null) {
                fail(callback, "Could not add the workout to your calendar.");
                return;
            }
            repository.setCalendarEventId(scheduleId, eventId);
            succeed(callback, eventId);
        });
    }

    /** Deletes the calendar event but keeps the schedule. */
    public void removeFromCalendar(String scheduleId, ServiceCallback<Void> callback) {
        if (sessionManager.getCurrentUid() == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            ScheduledWorkoutRepository repository = new ScheduledWorkoutRepository(db());
            Long eventId = scheduleId == null ? null : repository.getCalendarEventId(scheduleId);
            if (eventId != null) {
                if (!hasCalendarPermission()) {
                    fail(callback, "Allow calendar access to remove the workout from your calendar.");
                    return;
                }
                DeviceCalendar.deleteEvent(context(), eventId);
                repository.clearCalendarEventId(scheduleId);
            }
            succeed(callback, null);
        });
    }

    /**
     * Builds an email to the user's own address with the workout name, date, time and exercises
     * (FR-6.3), optionally with an .ics calendar invitation attached. On success, pass the
     * Intent to startActivity: it lets the user pick one of their email apps, which sends it.
     */
    public void buildEmail(String scheduleId, boolean attachIcs, ServiceCallback<Intent> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        FirebaseUser firebaseUser = sessionManager.getCurrentUser();
        onDb(() -> {
            ScheduledWorkout schedule = scheduleId == null
                    ? null : new ScheduledWorkoutRepository(db()).getOne(scheduleId);
            WorkoutSummary summary = schedule == null ? null : WorkoutSummary.of(db(), schedule);
            if (summary == null) {
                fail(callback, GONE);
                return;
            }
            User user = new UserRepository(db()).getOne(uid);
            String email = user != null ? user.getEmail()
                    : firebaseUser != null ? firebaseUser.getEmail() : null;
            if (email == null) {
                fail(callback, "Your account has no email address.");
                return;
            }
            // The selector limits the choice to email apps; SEND itself is what carries a file.
            Intent mailApps = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            if (mailApps.resolveActivity(context().getPackageManager()) == null) {
                fail(callback, "No email app is installed.");
                return;
            }
            TimeZone zone = TimeZone.getDefault();
            Locale locale = Locale.getDefault();
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            send.putExtra(Intent.EXTRA_SUBJECT, summary.getEmailSubject(zone, locale));
            send.putExtra(Intent.EXTRA_TEXT, summary.getEmailBody(zone, locale));
            if (attachIcs) {
                Uri ics;
                try {
                    ics = writeIcs(summary);
                } catch (IOException | IllegalArgumentException e) {
                    Log.w(TAG, "Writing the .ics file failed", e);
                    fail(callback, "Could not create the calendar file.");
                    return;
                }
                send.setType("text/calendar");
                send.putExtra(Intent.EXTRA_STREAM, ics);
                send.setClipData(ClipData.newRawUri("", ics));   // lets the grant reach the app
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            send.setSelector(mailApps);
            succeed(callback, Intent.createChooser(send, "Send workout by email"));
        });
    }

    /** arrayUnion/arrayRemove, so offline changes from two devices merge instead of overwriting. */
    private void setCompleted(String scheduleId, long occurrenceSeconds, boolean completed,
                              ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            ScheduledWorkout schedule = scheduleId == null
                    ? null : new ScheduledWorkoutRepository(db()).getOne(scheduleId);
            if (schedule == null) {
                fail(callback, GONE);
                return;
            }
            if (completed) {
                if (!schedule.isOccurrence(occurrenceSeconds, TimeZone.getDefault())) {
                    fail(callback, "The workout is not scheduled at that time.");
                    return;
                }
                if (occurrenceSeconds - OccurrenceStatus.MISSED_AFTER_SECONDS > now()) {
                    fail(callback, "That workout is not due yet.");
                    return;
                }
            }
            Object change = completed ? FieldValue.arrayUnion(occurrenceSeconds)
                    : FieldValue.arrayRemove(occurrenceSeconds);
            watchWrite(schedules(uid).document(scheduleId).update("completed", change),
                    (completed ? "Completing " : "Uncompleting ") + scheduleId + " at " + occurrenceSeconds);
            succeed(callback, null);
        });
    }

    /**
     * Writes only the one workout field, so the document has exactly one of the two, and the
     * repeat rule only for a repeating schedule.
     */
    private void add(String uid, String workoutName, String customWorkoutId, long scheduledAt,
                     Recurrence recurrence, boolean addToCalendar, ServiceCallback<String> callback) {
        if (scheduledAt <= 0) {
            fail(callback, "Pick a date and time.");
            return;
        }
        DocumentReference doc = schedules(uid).document();
        Map<String, Object> data = new HashMap<>();
        if (workoutName != null) data.put("workoutName", workoutName);
        else data.put("customWorkoutId", customWorkoutId);
        data.put("scheduledAt", scheduledAt);
        if (recurrence != null) data.put("recurrenceRule", recurrence.toRule());
        watchWrite(doc.set(data), "Creating schedule " + doc.getId());

        if (addToCalendar && hasCalendarPermission()) {
            WorkoutSummary summary = WorkoutSummary.of(db(), doc.getId(), workoutName,
                    customWorkoutId, scheduledAt, recurrence);
            Long eventId = summary == null ? null : DeviceCalendar.putEvent(context(), null, summary);
            if (eventId != null) new ScheduledWorkoutRepository(db()).setCalendarEventId(doc.getId(), eventId);
        }
        succeed(callback, doc.getId());
    }

    /** cache/ics/workout-{id}.ics, shared through the FileProvider declared in the manifest. */
    private Uri writeIcs(WorkoutSummary summary) throws IOException {
        File dir = new File(context().getCacheDir(), ICS_DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        File file = new File(dir, "workout-" + summary.getScheduleId() + ".ics");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            out.write(summary.toIcs(now()));
        }
        return FileProvider.getUriForFile(context(), context().getPackageName() + ".fileprovider", file);
    }

    private CollectionReference schedules(String uid) {
        return userDoc(uid).collection("schedules");
    }
}
