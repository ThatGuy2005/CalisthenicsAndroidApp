package com.example.calisthenicsapp.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.model.ScheduledWorkout;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * The users' planned workouts (the ScheduledWorkout table), keyed by their Firestore id, and the
 * device calendar events added for them (the CalendarEvent table, which exists only on this device
 * and is never synced). Only SyncManager writes the schedules, as it mirrors Firestore;
 * ScheduleCalendar writes the calendar events.
 */
public class ScheduledWorkoutRepository implements Repository<ScheduledWorkout> {
    private final SQLiteDatabase db;

    /** Works on an open database; it does not close it. */
    public ScheduledWorkoutRepository(SQLiteDatabase db) { this.db = db; }

    @Override
    public boolean insert(ScheduledWorkout s) {             // Id is the Firestore document id
        try {
            return db.insertOrThrow("ScheduledWorkout", null, toContentValues(s)) != -1;
        } catch (SQLiteConstraintException e) {
            return false;   // both or neither workout set, or unknown user/workout
        }
    }

    /** Changes the workout, the time and/or the repeat rule. The owner (Uid) and the calendar event stay. */
    @Override
    public boolean update(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        ContentValues cv = toContentValues(s);
        cv.remove("Id");
        cv.remove("Uid");
        return SqlHelpers.updateDB(db, "ScheduledWorkout", cv,
                "Id = ?", s.getId());
    }

    @Override
    public boolean delete(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        return SqlHelpers.deleteDB(db, "ScheduledWorkout",
                "Id = ?", s.getId());
    }

    @Override
    public List<ScheduledWorkout> getAll() {
        return query(null, null);
    }

    /** The schedule with this id, or null. */
    public ScheduledWorkout getOne(String id) {
        List<ScheduledWorkout> r = query("Id = ?", new String[]{id});
        return r.isEmpty() ? null : r.get(0);
    }

    /** All of a user's scheduled workouts, soonest first. */
    public List<ScheduledWorkout> getByUid(String uid) {
        return query("Uid = ?", new String[]{uid});
    }

    /**
     * A user's schedules that still have an occurrence at or after fromEpochSeconds: one-off
     * schedules from then on, and every repeating one. Sorted by their first occurrence, so
     * sort by the next one where that matters.
     */
    public List<ScheduledWorkout> getUpcoming(String uid, long fromEpochSeconds) {
        return query("Uid = ? AND (ScheduledAt >= ? OR RecurrenceRule IS NOT NULL)",
                new String[]{uid, String.valueOf(fromEpochSeconds)});
    }

    /**
     * A user's schedules that can have an occurrence in [fromEpochSeconds, toEpochSeconds):
     * one-off schedules inside it, and repeating ones that started before its end.
     */
    public List<ScheduledWorkout> getBetween(String uid, long fromEpochSeconds, long toEpochSeconds) {
        return query("Uid = ? AND ScheduledAt < ? AND (ScheduledAt >= ? OR RecurrenceRule IS NOT NULL)",
                new String[]{uid, String.valueOf(toEpochSeconds), String.valueOf(fromEpochSeconds)});
    }

    // ---------- Calendar events (local only, see CalendarEvent in schema.sql) ----------

    /** Works before the schedule row itself exists. */
    public void setCalendarEventId(String scheduleId, long eventId) {
        ContentValues cv = new ContentValues();
        cv.put("ScheduleId", scheduleId);
        cv.put("EventId", eventId);
        db.insertWithOnConflict("CalendarEvent", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** The calendar event added on this device for the schedule, or null. */
    public Long getCalendarEventId(String scheduleId) {
        try (Cursor c = db.query("CalendarEvent", new String[]{"EventId"}, "ScheduleId = ?",
                new String[]{scheduleId}, null, null, null)) {
            return c.moveToFirst() ? c.getLong(0) : null;
        }
    }

    /** Forgets the schedule's calendar event, e.g. after it was deleted from the calendar. */
    public void clearCalendarEventId(String scheduleId) {
        db.delete("CalendarEvent", "ScheduleId = ?", new String[]{scheduleId});
    }

    /**
     * The schedules matching where (all for null), soonest first, each with its calendar event id
     * from the LEFT JOIN (null when this device has none).
     */
    private List<ScheduledWorkout> query(String where, String[] args) {
        List<ScheduledWorkout> list = new ArrayList<>();
        String sql = "SELECT s.*, e.EventId AS CalendarEventId FROM ScheduledWorkout s"
                + " LEFT JOIN CalendarEvent e ON e.ScheduleId = s.Id"
                + (where == null ? "" : " WHERE " + where)
                + " ORDER BY s.ScheduledAt ASC";
        try (Cursor c = db.rawQuery(sql, args)) {
            int idCol = c.getColumnIndexOrThrow("Id");
            int uidCol = c.getColumnIndexOrThrow("Uid");
            int nameCol = c.getColumnIndexOrThrow("WorkoutName");
            int customCol = c.getColumnIndexOrThrow("CustomWorkoutId");
            int atCol = c.getColumnIndexOrThrow("ScheduledAt");
            int ruleCol = c.getColumnIndexOrThrow("RecurrenceRule");
            int completedCol = c.getColumnIndexOrThrow("Completed");
            int eventCol = c.getColumnIndexOrThrow("CalendarEventId");
            while (c.moveToNext()) {
                list.add(new ScheduledWorkout(
                        c.getString(idCol),
                        c.getString(uidCol),
                        c.getString(nameCol),
                        c.getString(customCol),
                        c.getLong(atCol),
                        c.getString(ruleCol),
                        completedFromJson(c.getString(completedCol)),
                        c.isNull(eventCol) ? null : c.getLong(eventCol)));
            }
        }
        return list;
    }

    /**
     * The schedule as a row. The completed occurrences are stored as a JSON array of epoch seconds,
     * or NULL when there are none; the calendar event is not part of the row.
     */
    private ContentValues toContentValues(ScheduledWorkout s) {
        ContentValues cv = new ContentValues();
        if (s.getId() != null) cv.put("Id", s.getId());
        cv.put("Uid", s.getUid());
        if (s.getWorkoutName() != null) cv.put("WorkoutName", s.getWorkoutName());
        else cv.putNull("WorkoutName");
        if (s.getCustomWorkoutId() != null) cv.put("CustomWorkoutId", s.getCustomWorkoutId());
        else cv.putNull("CustomWorkoutId");
        cv.put("ScheduledAt", s.getScheduledAt());
        if (s.getRecurrenceRule() != null) cv.put("RecurrenceRule", s.getRecurrenceRule());
        else cv.putNull("RecurrenceRule");
        if (s.getCompletedOccurrences().isEmpty()) cv.putNull("Completed");
        else cv.put("Completed", new JSONArray(s.getCompletedOccurrences()).toString());
        return cv;
    }

    /** Reads back the Completed column written by toContentValues. */
    private static List<Long> completedFromJson(String json) {
        List<Long> completed = new ArrayList<>();
        if (json == null) return completed;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) completed.add(array.getLong(i));
        } catch (JSONException e) {
            // Only this class writes the column; an unreadable value counts as none.
        }
        return completed;
    }
}
