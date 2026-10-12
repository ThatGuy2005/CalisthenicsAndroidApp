PRAGMA foreign_keys = ON;

-- ========== USERS ==========
CREATE TABLE IF NOT EXISTS Users (
    Username TEXT PRIMARY KEY COLLATE NOCASE,
    Email    TEXT NOT NULL UNIQUE COLLATE NOCASE,
    PasswordHash  TEXT NOT NULL,
    Height        REAL,
    Weight        REAL,
    Gender        TEXT,
    Mode          TEXT NOT NULL DEFAULT 'User'
                  CHECK (Mode IN ('Admin', 'User')),
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
    WHERE Username = NEW.Username;
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
CREATE TABLE IF NOT EXISTS CustomWorkout (
    Id        INTEGER PRIMARY KEY AUTOINCREMENT,
    Name      TEXT NOT NULL,
    Username  TEXT NOT NULL,
    UNIQUE (Username, Name),                              -- unique per user, not globally
    FOREIGN KEY (Username) REFERENCES Users(Username)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS CustomExerciseOrder (
    CustomWorkoutId INTEGER NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_CustomWorkout_Username ON CustomWorkout(Username);

CREATE TABLE IF NOT EXISTS WorkoutLog (
    Id              INTEGER PRIMARY KEY AUTOINCREMENT,
    Username        TEXT NOT NULL,
    WorkoutName     TEXT NOT NULL,                        -- plain text snapshot, no FK, so the log survives workout deletion
    StartedAt       INTEGER NOT NULL,                     -- Unix epoch, seconds
    DurationSeconds INTEGER NOT NULL CHECK (DurationSeconds >= 0),
    Kcal            REAL NOT NULL CHECK (Kcal >= 0),
    FOREIGN KEY (Username) REFERENCES Users(Username)
        ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_WorkoutLog_User ON WorkoutLog(Username, StartedAt);

CREATE TABLE IF NOT EXISTS ScheduledWorkout (
    Id              INTEGER PRIMARY KEY AUTOINCREMENT,
    Username        TEXT NOT NULL,
    WorkoutName     TEXT,                 -- premade workout
    CustomWorkoutId INTEGER,              -- custom workout
    ScheduledAt     INTEGER NOT NULL,     -- Unix epoch, seconds
    CHECK ((WorkoutName IS NOT NULL) + (CustomWorkoutId IS NOT NULL) = 1),
    FOREIGN KEY (Username) REFERENCES Users(Username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (WorkoutName) REFERENCES Workout(Name)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (CustomWorkoutId) REFERENCES CustomWorkout(Id)
        ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_Scheduled_User ON ScheduledWorkout(Username, ScheduledAt);