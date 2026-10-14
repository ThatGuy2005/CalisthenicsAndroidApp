package com.example.calisthenicsapp.Scheduling;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.TimeZone;

/**
 * Scheduled workouts as events in the device's calendar (FR-6.2), through the calendar
 * provider. A repeating schedule is one repeating event (FR-6.1). Needs {@link #PERMISSIONS}; the screen asks for them (FR-6.5). Every method checks
 * them first, so a denied permission never crashes. Call these on a background thread.
 *
 * Events go into the primary writable calendar. A device with no calendar account at all
 * (e.g. a fresh emulator) gets a local "Calisthenics" calendar, created once.
 */
public final class DeviceCalendar {
    public static final String[] PERMISSIONS = {
            Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
    };

    private static final String TAG = "DeviceCalendar";
    private static final String LOCAL_ACCOUNT = "CalisthenicsApp";

    private DeviceCalendar() {
    }

    public static boolean hasPermission(Context context) {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates the event if it still exists, otherwise adds a new one with a reminder.
     * Returns the event id, or null if there is no permission or no calendar to add it to.
     */
    public static Long putEvent(Context context, Long eventId, WorkoutSummary summary) {
        if (!hasPermission(context)) return null;
        ContentResolver resolver = context.getContentResolver();
        try {
            if (eventId != null && exists(resolver, eventId)) {
                resolver.update(eventUri(eventId), eventValues(summary), null, null);
                return eventId;
            }
            Long calendarId = findWritableCalendar(resolver);
            if (calendarId == null) calendarId = createLocalCalendar(resolver);
            if (calendarId == null) return null;

            ContentValues values = eventValues(summary);
            values.put(Events.CALENDAR_ID, calendarId);
            Uri uri = resolver.insert(Events.CONTENT_URI, values);
            if (uri == null) return null;
            long newId = ContentUris.parseId(uri);

            ContentValues reminder = new ContentValues();
            reminder.put(Reminders.EVENT_ID, newId);
            reminder.put(Reminders.MINUTES, WorkoutSummary.REMINDER_MINUTES);
            reminder.put(Reminders.METHOD, Reminders.METHOD_ALERT);
            resolver.insert(Reminders.CONTENT_URI, reminder);
            return newId;
        } catch (RuntimeException e) {   // SecurityException, or no calendar provider at all
            Log.w(TAG, "Adding schedule " + summary.getScheduleId() + " to the calendar failed", e);
            return null;
        }
    }

    /**
     * Moves an existing event to the summary's time and refreshes its text. Never adds one.
     * Returns false only if the event is gone (e.g. the user deleted it in the calendar app).
     */
    public static boolean updateEvent(Context context, long eventId, WorkoutSummary summary) {
        if (!hasPermission(context)) return true;   // cannot tell; keep the link
        ContentResolver resolver = context.getContentResolver();
        try {
            if (!exists(resolver, eventId)) return false;
            resolver.update(eventUri(eventId), eventValues(summary), null, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Updating calendar event " + eventId + " failed", e);
        }
        return true;
    }

    public static void deleteEvent(Context context, long eventId) {
        if (!hasPermission(context)) return;
        try {
            context.getContentResolver().delete(eventUri(eventId), null, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "Deleting calendar event " + eventId + " failed", e);
        }
    }

    /**
     * A repeating event needs RRULE and DURATION instead of DTEND; the provider rejects an
     * event with both. The unused ones are set to null so an update can switch between the two.
     */
    private static ContentValues eventValues(WorkoutSummary summary) {
        ContentValues values = new ContentValues();
        values.put(Events.TITLE, summary.getTitle());
        values.put(Events.DESCRIPTION, summary.getDescription());
        values.put(Events.DTSTART, summary.getStartSeconds() * 1000L);
        if (summary.getRecurrence() == null) {
            values.put(Events.DTEND, summary.getEndSeconds() * 1000L);
            values.putNull(Events.RRULE);
            values.putNull(Events.DURATION);
        } else {
            values.putNull(Events.DTEND);
            values.put(Events.RRULE, summary.getRecurrence().toRule());
            values.put(Events.DURATION, "PT" + summary.getEventSeconds() + "S");
        }
        values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        values.put(Events.HAS_ALARM, 1);
        return values;
    }

    private static Uri eventUri(long eventId) {
        return ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
    }

    /** Deleted events can linger in the provider with DELETED = 1 until the next sync. */
    private static boolean exists(ContentResolver resolver, long eventId) {
        try (Cursor c = resolver.query(Events.CONTENT_URI, new String[]{Events._ID},
                Events._ID + " = ? AND " + Events.DELETED + " = 0",
                new String[]{String.valueOf(eventId)}, null)) {
            return c != null && c.moveToFirst();
        }
    }

    /** A visible calendar the app may add events to, the primary one first. */
    private static Long findWritableCalendar(ContentResolver resolver) {
        try (Cursor c = resolver.query(Calendars.CONTENT_URI, new String[]{Calendars._ID},
                Calendars.VISIBLE + " = 1 AND " + Calendars.CALENDAR_ACCESS_LEVEL + " >= ?",
                new String[]{String.valueOf(Calendars.CAL_ACCESS_CONTRIBUTOR)},
                Calendars.IS_PRIMARY + " DESC, " + Calendars._ID + " ASC")) {
            return c != null && c.moveToFirst() ? c.getLong(0) : null;
        }
    }

    /** Creating a calendar is only allowed as a sync adapter, here for a local account. */
    private static Long createLocalCalendar(ContentResolver resolver) {
        Uri uri = Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
        ContentValues values = new ContentValues();
        values.put(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT);
        values.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(Calendars.NAME, "Calisthenics");
        values.put(Calendars.CALENDAR_DISPLAY_NAME, "Calisthenics");
        values.put(Calendars.CALENDAR_COLOR, 0xFF2E7D32);
        values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        values.put(Calendars.OWNER_ACCOUNT, LOCAL_ACCOUNT);
        values.put(Calendars.VISIBLE, 1);
        values.put(Calendars.SYNC_EVENTS, 1);
        values.put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());
        Uri created = resolver.insert(uri, values);
        return created == null ? null : ContentUris.parseId(created);
    }
}
