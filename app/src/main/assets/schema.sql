PRAGMA foreign_keys = ON;

-- ========== USERS ==========
-- Mirrors users/{uid} from Firestore (the source of truth). Firebase Authentication
-- owns the password, so there is no PasswordHash column, and there is no admin role,
-- so there is no Mode column.
CREATE TABLE IF NOT EXISTS Users (
    Uid      TEXT PRIMARY KEY NOT NULL,     -- Firebase Auth uid
    Email    TEXT NOT NULL UNIQUE COLLATE NOCASE,
    Username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    Height        REAL,
    Weight        REAL,
    Gender        TEXT,
    CreatedAt     INTEGER NOT NULL
                  DEFAULT (CAST(strftime('%s', 'now') AS INTEGER)),  -- Unix epoch, seconds
    UpdatedAt     INTEGER NOT NULL
                  DEFAULT (CAST(strftime('%s', 'now') AS INTEGER))   -- Unix epoch, seconds
);

-- Bumps UpdatedAt on any update. The WHEN guard stops the trigger
-- from re-firing on its own UPDATE and lets you set UpdatedAt manually.
CREATE TRIGGER IF NOT EXISTS trg_Users_UpdatedAt
AFTER UPDATE ON Users
FOR EACH ROW
WHEN NEW.UpdatedAt = OLD.UpdatedAt
BEGIN
    UPDATE Users
    SET UpdatedAt = CAST(strftime('%s', 'now') AS INTEGER)
    WHERE Uid = NEW.Uid;
END;

-- ========== EXERCISE ==========
CREATE TABLE IF NOT EXISTS Exercise (
    Name            TEXT PRIMARY KEY,                     -- unique
    Instructions    TEXT,
    IsTimed         INTEGER NOT NULL DEFAULT 0
                    CHECK (IsTimed IN (0, 1)),            -- 1 = seconds (plank), 0 = reps
    DefaultSeconds  INTEGER CHECK (DefaultSeconds > 0),   -- used when IsTimed = 1
    DefaultReps     INTEGER CHECK (DefaultReps > 0),      -- used when IsTimed = 0
    Met             REAL NOT NULL DEFAULT 3.8
                    CHECK (Met > 0),                      -- for calorie estimates
    AnimationFile   TEXT,                                  -- e.g. 'pushup.json'; NULL = placeholder
    MuscleGroup TEXT,
    Difficulty TEXT CHECK (Difficulty IN ('Beginner','Intermediate','Advanced'))
);

-- ========== WORKOUT ==========
CREATE TABLE IF NOT EXISTS Workout (
    Name TEXT PRIMARY KEY,                                 -- unique
    Level TEXT
);

-- ========== EXERCISE ORDER (Workout <-> Exercise) ==========
CREATE TABLE IF NOT EXISTS ExerciseOrder (
    WorkoutName   TEXT NOT NULL,
    ExerciseName  TEXT NOT NULL,
    Position      INTEGER NOT NULL CHECK (Position > 0),
    Sets        INTEGER NOT NULL DEFAULT 3 CHECK (Sets > 0),
    Reps        INTEGER CHECK (Reps > 0),
    Seconds     INTEGER CHECK (Seconds > 0),
    RestSeconds INTEGER NOT NULL DEFAULT 60 CHECK (RestSeconds >= 0),
    PRIMARY KEY (WorkoutName, Position),
    FOREIGN KEY (WorkoutName)  REFERENCES Workout(Name)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (ExerciseName) REFERENCES Exercise(Name)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ========== CUSTOM WORKOUT ==========
-- Id, like every per-user table's Id below, is a Firestore document id (TEXT), not an
-- autoincrement integer: the id is assigned by Firestore and mirrored in here as-is.
CREATE TABLE IF NOT EXISTS CustomWorkout (
    Id    TEXT PRIMARY KEY NOT NULL,
    Name  TEXT NOT NULL,
    Uid   TEXT NOT NULL,
    UNIQUE (Uid, Name),                                   -- unique per user, not globally
    FOREIGN KEY (Uid) REFERENCES Users(Uid)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS CustomExerciseOrder (
    CustomWorkoutId TEXT NOT NULL,
    ExerciseName    TEXT NOT NULL,
    Position        INTEGER NOT NULL CHECK (Position > 0),
    Sets        INTEGER NOT NULL DEFAULT 3 CHECK (Sets > 0),
    Reps        INTEGER CHECK (Reps > 0),
    Seconds     INTEGER CHECK (Seconds > 0),
    RestSeconds INTEGER NOT NULL DEFAULT 60 CHECK (RestSeconds >= 0),
    PRIMARY KEY (CustomWorkoutId, Position),
    FOREIGN KEY (CustomWorkoutId) REFERENCES CustomWorkout(Id)
        ON DELETE CASCADE,
    FOREIGN KEY (ExerciseName) REFERENCES Exercise(Name)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_CustomWorkout_Uid ON CustomWorkout(Uid);

CREATE TABLE IF NOT EXISTS WorkoutLog (
    Id              TEXT PRIMARY KEY NOT NULL,
    Uid             TEXT NOT NULL,
    WorkoutName     TEXT NOT NULL,                        -- plain text snapshot, no FK, so the log survives workout deletion
    StartedAt       INTEGER NOT NULL,                     -- Unix epoch, seconds
    DurationSeconds INTEGER NOT NULL CHECK (DurationSeconds >= 0),
    Kcal            REAL NOT NULL CHECK (Kcal >= 0),
    Status          TEXT NOT NULL DEFAULT 'Completed'
                    CHECK (Status IN ('Completed', 'Partial')),
    Snapshot        TEXT,                                 -- JSON array of LoggedExercise: the exercises as done
    FOREIGN KEY (Uid) REFERENCES Users(Uid)
        ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_WorkoutLog_Uid ON WorkoutLog(Uid, StartedAt);

CREATE TABLE IF NOT EXISTS ScheduledWorkout (
    Id              TEXT PRIMARY KEY NOT NULL,
    Uid             TEXT NOT NULL,
    WorkoutName     TEXT,                 -- premade workout
    CustomWorkoutId TEXT,                 -- custom workout
    ScheduledAt     INTEGER NOT NULL,     -- Unix epoch, seconds
    CHECK ((WorkoutName IS NOT NULL) + (CustomWorkoutId IS NOT NULL) = 1),
    FOREIGN KEY (Uid) REFERENCES Users(Uid)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (WorkoutName) REFERENCES Workout(Name)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (CustomWorkoutId) REFERENCES CustomWorkout(Id)
        ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_Scheduled_Uid ON ScheduledWorkout(Uid, ScheduledAt);

-- The device calendar event of a scheduled workout (FR-6.2). Local only, never synced: an event
-- id means nothing on another device. A separate table, not a ScheduledWorkout column, because
-- the event is added right when the schedule is created, possibly before SyncManager has
-- mirrored the schedule row, and because sync upserts must not overwrite it. No foreign key,
-- so the link also survives the cache being cleared on sign-out.
CREATE TABLE IF NOT EXISTS CalendarEvent (
    ScheduleId TEXT PRIMARY KEY NOT NULL,  -- ScheduledWorkout.Id (Firestore document id)
    EventId    INTEGER NOT NULL            -- CalendarContract.Events._ID
);