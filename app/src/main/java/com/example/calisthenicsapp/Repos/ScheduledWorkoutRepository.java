package com.example.calisthenicsapp.Repos;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;

import java.util.ArrayList;
import java.util.List;

public class ScheduledWorkoutRepository implements Repository<ScheduledWorkout> {
    private final SQLiteDatabase db;

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
        return Repository.updateDB(db, "ScheduledWorkout", cv,
                "Id = ?", s.getId());
    }

    @Override
    public boolean delete(ScheduledWorkout s) {
        if (s.getId() == null) return false;
        return Repository.deleteDB(db, "ScheduledWorkout",
                "Id = ?", s.getId());
    }

    @Override
    public List<ScheduledWorkout> getAll() {
        return query(null, null);
    }

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

    // ---------- Calendar events (local only, see CalendarEvent in schema.sql) ----------

    /** Works before the schedule row itself exists. */
    public void setCalendarEventId(String scheduleId, long eventId) {
        ContentValues cv = new ContentValues();
        cv.put("ScheduleId", scheduleId);
        cv.put("EventId", eventId);
        db.insertWithOnConflict("CalendarEvent", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Long getCalendarEventId(String scheduleId) {
        try (Cursor c = db.query("CalendarEvent", new String[]{"EventId"}, "ScheduleId = ?",
                new String[]{scheduleId}, null, null, null)) {
            return c.moveToFirst() ? c.getLong(0) : null;
        }
    }

    public void clearCalendarEventId(String scheduleId) {
        db.delete("CalendarEvent", "ScheduleId = ?", new String[]{scheduleId});
    }

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
            int eventCol = c.getColumnIndexOrThrow("CalendarEventId");
            while (c.moveToNext()) {
                list.add(new ScheduledWorkout(
                        c.getString(idCol),
                        c.getString(uidCol),
                        c.getString(nameCol),
                        c.getString(customCol),
                        c.getLong(atCol),
                        c.getString(ruleCol),
                        c.isNull(eventCol) ? null : c.getLong(eventCol)));
            }
        }
        return list;
    }

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
        return cv;
    }
}
