package com.example.calisthenicsapp.Scheduling;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;

/**
 * A schedule's event in the device calendar (FR-6.2) together with the event id kept for it.
 * The id is stored only in this device's SQLite (see ScheduledWorkout.getCalendarEventId()),
 * so the event and the id change together here: ScheduleService uses this for the user's own
 * calendar actions, SyncManager when a schedule changes on another device.
 *
 * Every method reads or writes SQLite, so call them on a background thread.
 */
public class ScheduleCalendar {
    private final Context context;
    private final SQLiteDatabase db;
    private final ScheduledWorkoutRepository schedules;

    public ScheduleCalendar(Context context, SQLiteDatabase db) {
        this.context = context.getApplicationContext();
        this.db = db;
        this.schedules = new ScheduledWorkoutRepository(db);
    }

    public boolean hasPermission() {
        return DeviceCalendar.hasPermission(context);
    }

    /**
     * The schedule's event id, or null if it has no event. Looked up by id, not through the
     * row: deleting a custom workout may already have cascaded the row away.
     */
    public Long getEventId(String scheduleId) {
        return schedules.getCalendarEventId(scheduleId);
    }

    /**
     * Adds the summary's schedule to the calendar, or refreshes eventId if that event is still
     * there, and keeps the id. Works before the schedule row itself exists.
     * Returns the event id, or null if the event could not be added.
     */
    public Long put(WorkoutSummary summary, Long eventId) {
        Long added = DeviceCalendar.putEvent(context, eventId, summary);
        if (added != null) schedules.setCalendarEventId(summary.getScheduleId(), added);
        return added;
    }

    /**
     * Moves the event to the schedule's new time or repeat rule. If the user deleted the event
     * in their calendar app, the id is dropped instead, so the event does not come back.
     */
    public void update(ScheduledWorkout schedule, long eventId) {
        WorkoutSummary summary = WorkoutSummary.of(db, schedule);
        if (summary == null) return;
        if (!DeviceCalendar.updateEvent(context, eventId, summary)) {
            schedules.clearCalendarEventId(schedule.getId());
        }
    }

    /** Deletes the schedule's event, if it has one, and its id. Returns whether there was one. */
    public boolean remove(String scheduleId) {
        Long eventId = getEventId(scheduleId);
        if (eventId == null) return false;
        DeviceCalendar.deleteEvent(context, eventId);
        schedules.clearCalendarEventId(scheduleId);
        return true;
    }
}
