# CalisthenicsApp

An Android app (Java, minSdk 24) for bodyweight training: premade and custom workouts, a guided
workout session, training history with statistics, and scheduled workouts in the device calendar.

- **Requirements (SRS):** [app/src/main/java/com/example/calisthenicsapp/README.md](app/src/main/java/com/example/calisthenicsapp/README.md).
  Ids like FR-6.1 below refer to it.
- **This file:** how to build it, and a guide to the back end for whoever builds the screens.

## Building and testing

Open the project in Android Studio, or use Gradle:

| Command | What it does |
|---|---|
| `./gradlew assembleDebug` | Builds the debug APK. |
| `./gradlew testDebugUnitTest` | Runs the unit tests: plain JVM tests plus Robolectric tests for the database. |
| `./gradlew lintDebug` | Android lint. It should report 0 errors. |

`app/google-services.json` connects the app to the Firebase project `thecalisthenicsapp`. Its
Firestore database is `(default)` in `eur3` (Europe). The security rules are in
[`firestore.rules`](firestore.rules); after changing them (and running the rules tests below),
publish them in one of two ways:

- **Firebase console:** Firestore Database → Rules, paste the whole file, Publish.
- **Command line:** [`firebase.json`](firebase.json) and [`.firebaserc`](.firebaserc) point the
  Firebase CLI at the rules file and the project. With Node.js 18 or newer, from the repository root:
  ```
  npx firebase-tools@14.27.0 login
  npx firebase-tools@14.27.0 deploy --only firestore:rules
  ```
  `login` opens the browser once per computer; use a Google account with access to the project.

### Testing the Firestore rules

[`firestore-tests/`](firestore-tests) checks `firestore.rules` against the local Firestore
emulator. It covers every write the app makes, and many writes the rules must reject. It needs
Node.js 18 or newer and Java 11 or newer, and makes no changes to the real Firebase project:

```
cd firestore-tests
npm install
npm test
```

Each check prints PASS or FAIL, and `npm test` fails if any check does. Run it after every change
to `firestore.rules`, and add a check when the app starts writing something new.

## How the back end works

- **Firebase Authentication** owns the accounts.
- **Cloud Firestore** stores each user's data under `users/{uid}` and is the source of truth.
- **SQLite** is a local copy. `Sync/SyncManager` mirrors Firestore into it as long as someone is
  signed in. It is started once in `CalisthenicsApplication`; the screens never deal with it.
- **Premade exercises and workouts** live only in SQLite. They are loaded from
  `app/src/main/assets/seed.sql` on the first start.

| Package | Contents |
|---|---|
| `Service` | **The API for the screens.** One class per area; see below. |
| `CoreComponents` | The data classes the services return (`Exercise`, `Workout`, `WorkoutLog`, `ScheduledWorkout`, ...). |
| `Session` | `WorkoutSession`, a workout in progress. |
| `Scheduling` | Repeat rules, schedule occurrences and their status, the device calendar, the email and its `.ics` file. Used by `ScheduleService`. |
| `Export` | The JSON and CSV data export. Used by `AccountService`. |
| `Repos`, `Database`, `Sync`, `Verificate` | Storage, sync and validation. Use them through the services. |

### Rules for every service call

1. **Create services with any `Context`.** For example `new ExerciseService(this)` in an
   Activity. They keep only the application context, so they are safe to hold in a field.
   Screens only need this constructor. The longer ones take the database, the `SessionManager`
   and Firestore as parameters, for tests (see `ServiceDependenciesTest`).
2. **Every call is asynchronous** and reports back through a callback:
   ```java
   new CustomWorkoutService(this).getMyCustomWorkouts(new ServiceCallback<List<CustomWorkout>>() {
       @Override public void onSuccess(List<CustomWorkout> workouts) { adapter.submit(workouts); }
       @Override public void onError(String message) { showError(message); }
   });
   ```
   **Callbacks always run on the main thread**, so you can update views directly. Database and
   network work never runs on the main thread (NFR-11).
3. **Error messages are written for the user.** Show `onError`'s message as it is, for example
   in a Snackbar (NFR-8).
4. **Reads come from SQLite**, so they are fast and work offline.
5. **Writes go to Firestore.** `onSuccess` fires as soon as Firestore has the change, which also
   works offline; it is sent to the server later.
   - **A read right after a write may not show the change yet:** `SyncManager` copies it into
     SQLite a moment later. Don't reload in `onSuccess`; use `DataChanges` (below), which calls
     you once the change can be read.
6. **Times are Unix epoch seconds** (`long`), never milliseconds. Height is in cm, weight in kg.
7. **Most calls need a signed-in user** and fail with "You are not signed in." otherwise.
   Exceptions: `ExerciseService` and `WorkoutService` (premade content), and the sign-in calls.

### Keeping screens up to date

`DataChanges` tells a screen when its data changed: after this device's own writes (once they can
be read) and when changes arrive from another device. Call `observe` once in `onCreate`:

```java
DataChanges.observe(this, kind -> loadWorkouts(), DataChanges.Kind.CUSTOM_WORKOUTS);
```

- **It loads and reloads:** the listener runs on every `onStart`, so it is also the first load,
  and after every change while the screen is visible. It unsubscribes by itself on `onStop`.
- **Kinds:** `PROFILE`, `CUSTOM_WORKOUTS`, `WORKOUT_LOGS` and `SCHEDULES`. Pass several to watch
  several. `SCHEDULES` also fires when a calendar event is added or removed.
- **Main thread:** listeners run on the main thread. Changes that come in close together arrive
  as one call.
- **Sign-in and sign-out:** every kind fires on sign-out, and as the data arrives after sign-in.

**What needs a connection (NFR-13):** sign-up, sign-in, password reset, changing the username,
email or password, deleting the account, and sending email. Everything else works offline and
syncs later.

## Services by screen

### Sign-up, sign-in and account (FR-1)

`AuthService` (callbacks: `AuthService.AuthCallback<T>`, also on the main thread)

| Method | Notes |
|---|---|
| `signUp(username, email, password, height, weight, gender, birthYear, cb)` | Height, weight, gender and birth year may be null. The username must be unique; "That username is taken." otherwise. |
| `signIn(email, password, cb)` | |
| `sendPasswordReset(email, cb)` | |
| `signOut()` | Also clears the local copy of the user's data. |
| `getCurrentUser()` | Null when nobody is signed in. |

`SessionManager`: `getCurrentUid()`, and `addAuthStateListener(...)` to react to sign-in and
sign-out.

`ProfileService`

| Method | Notes |
|---|---|
| `getMyProfile(cb)` | A `User`. **Can be null** right after sign-in, until the profile has synced; observe `PROFILE` to get it when it arrives. |
| `updateProfile(username, height, weight, gender, birthYear, cb)` | Pass the current value for anything unchanged. Changing the username needs a connection. |

`AccountService` (all of these ask for the current password first)

| Method | Notes |
|---|---|
| `changePassword(currentPassword, newPassword, cb)` | |
| `changeEmail(currentPassword, newEmail, cb)` | Firebase sends a confirmation link to the new address. The profile shows the new email after the next sign-in. |
| `deleteAccount(password, cb)` | Deletes everything permanently and signs out (FR-1.10), then removes the schedules' events from this device's calendar and clears Firestore's offline copy from the device. |
| `exportMyData(cb)` | All of the user's data as a JSON string (NFR-5), e.g. to share or save as a file. |
| `exportMyDataCsv(uri, cb)` | All of the user's data as CSV files in one ZIP (NFR-5): profile, custom workouts, logs, log exercises and schedules. Writes to `uri`; see below. |

"Download my data" as CSV: let the user pick where to save, then write there.

```java
ActivityResultLauncher<String> saveCsv = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/zip"),
        uri -> { if (uri != null) account.exportMyDataCsv(uri, callback); });   // null: cancelled
saveCsv.launch("calisthenics-data.zip");
```

The limits: username 3-20 letters or digits (FR-1.1); password 8-64 characters with a letter
and a digit; height 50-300 cm; weight 20-500 kg; gender up to 30 characters; birth year from 1900
to this year.
`Verificate/UserValidator` has the same checks, for validating a form as the user types.

**Units (FR-1.7).** The services always take and return kg and cm. The kg/lb, cm/ft toggle is a
setting of the device: save `UnitSystem.name()` (for example in `SharedPreferences`) and read it
back with `UnitSystem.fromName(saved)`, which gives `METRIC` when nothing is saved. The screens
already do this through `UnitPreference.get` / `set` (Settings has the toggle), and sign-up and
the profile take a height through `HeightInput` (cm, or ft and in fields).
`CoreComponents/Units` does the rest:

- Fill a form field: `weightFromKg(kg, system)` and `cmToFeetInches(cm)` (use two fields for ft and in).
- Before calling a service: `weightToKg(typed, system)` and `feetInchesToCm(feet, inches)`.
  Convert a height back only if the user changed it, since whole inches are coarser than cm.
- Check a typed value: convert it, use `UserValidator.isValidHeight` / `isValidWeight`, and show
  `heightError(system, locale)` / `weightError(system, locale)`, which state the range in the
  user's units.

### Exercises and premade workouts (FR-3, FR-4.1, FR-4.2)

`ExerciseService`: `getAllExercises`, `getExercise(name)`, `getMuscleGroups` (for a filter),
`searchExercises(nameQuery, muscleGroup, difficulty, equipment, cb)`.

- **Search filters:** pass null or "" to skip a filter. The name matches anywhere in it and case
  is ignored.
- **Difficulty:** the `Difficulty` enum (BEGINNER, INTERMEDIATE, ADVANCED). `Exercise.getDifficulty()`
  returns it, and `searchExercises` takes it (null for any). For the filter options use
  `Difficulty.values()`, easiest first, and `getLabel()` for the text.
- **Animations:** `Exercise.getAnimationFile()` is a path under `app/src/main/assets`, such as
  `animations/push_up.webp` (animated WebP; `.lottie` also plays). Every premade exercise has
  one; on null, show a placeholder.
- **Equipment and muscle groups (FR-3.2):** `Exercise.getEquipment()` is the `Equipment` enum
  (NONE, WALL, BENCH, PULL_UP_BAR, PARALLEL_BARS; never null), with `getLabel()` for the text
  ("Pull-up bar") and `Equipment.values()` for the filter options; `searchExercises` takes it
  (null for any, NONE for exercises that need nothing). `isMultiMuscle()` is true for compound
  movements that work several muscle groups (push-ups), false for those that mainly work
  `getMuscleGroup()` (crunches).
- **Per side (FR-4.2):** `Exercise.isPerSide()` is true when the reps or seconds count for each
  side, as in lunges; show "12 reps per side". One set does both sides, and a timed per-side set
  counts down both sides in one go. The session's `SessionExercise.isPerSide()` says the same.

`WorkoutService`: `getAllWorkouts`, `getWorkout(name)`, `getExercises(workoutName)` (in order).
A premade workout's `getLevel()` is its difficulty rating, the same `Difficulty` enum (FR-4.1);
it is null for custom workouts. `getCategory()` is its general category (FR-4.1), the
`WorkoutCategory` enum: FULL_BODY, UPPER_BODY, LOWER_BODY, CORE or CARDIO, with `getLabel()` for
the text ("Full body") and `WorkoutCategory.values()` for a filter or grouping.
`getDescription()` is a sentence or two on what it trains and what equipment it needs (DR-2).
`getEstimatedSeconds()` is how long it is expected to take: every set plus the rests, counting
3 s per rep. It is the same estimate the calendar event uses (FR-4.1).

Each exercise has two rest times (FR-4.2): `getRestSeconds()` between its sets, and
`getRestAfterSeconds()` after its last set, before the next exercise. A null
`getRestAfterSeconds()` means the same as `getRestSeconds()`. The workout session uses both.

### Custom workouts (FR-4.3, FR-4.4)

`CustomWorkoutService`

| Method | Notes |
|---|---|
| `getMyCustomWorkouts(cb)`, `getExercises(id, cb)` | Each workout has `getEstimatedSeconds()`, worked out like a premade workout's. |
| `create(name, exercises, cb)`, `create(name, description, exercises, cb)` | Returns the new id. 1-50 exercises; the name is at most 40 characters and unique per user. The description is optional (null or blank for none), at most 200 characters. |
| `duplicatePremade(workoutName, newName, cb)` | Copies a premade workout to edit, description included. With a null name it reuses the premade name, made unique ("Name (2)"). |
| `rename(id, newName, cb)`, `setDescription(id, description, cb)`, `replaceExercises(id, exercises, cb)` | `getDescription()` reads it back; null or blank removes it. |
| `delete(id, cb)` | Also deletes the schedules that use it. |

Build the exercise list from `CustomExerciseOrderEntry` objects. `position` is set from the list
order. Leave `reps`/`seconds` null to use the exercise's own default, and leave `sets`/`restSeconds`
null for 3 sets and 60 s rest. `restSeconds` is the rest between the sets; `restAfterSeconds` (the
eight-argument constructor) is the rest after the last set, before the next exercise, and null means
the same as `restSeconds` (FR-4.2). Rests may be 0 but not negative.

### Doing a workout (FR-5.1 to FR-5.6)

1. Start it with `WorkoutSessionService.startPremade(name, cb)` or `startCustom(id, cb)`. The
   `WorkoutSession` you get back is already running.
2. While the screen shows it, call `session.tick()` about once a second (for example with a
   `Handler`). It returns true when a step changed, which is the moment for a sound or vibration
   cue.
3. Buttons call `completeSet()`, `skipRest()`, `skipExercise()`, `pause()`, `resume()` and
   `cancel()`.
4. Read the screen state from the getters:
   - `getPhase()` (WORK, REST, FINISHED, CANCELLED);
   - `getCurrentExercise()`, `getCurrentSet()` and `getNextExercise()`;
   - `getRemainingSeconds()`, which is null for a rep-based set (a per-side timed set counts
     down both sides);
   - `getCurrentExerciseNumber()`, `getTotalSeconds()` and `getTotalKcal()`.
   - Calories (FR-1.2, FR-5.6) are MET x the user's BMR / 24 x active hours, with the
     Mifflin-St Jeor BMR from weight, height, birth year and gender. Without height or birth
     year it is MET x weight x hours, and without a weight 70 kg is assumed. Gender is free text:
     "male", "man" or "m" and "female", "woman" or "f" (any case) pick the formula, anything
     else uses the average of the two, so a gender picker with Male / Female / Other fits best.
5. **Save it** with `WorkoutLogService.saveSession(session, cb)` once `isOver()` is true.
   - Saving twice does not create two logs.
   - A finished session that matches a scheduled workout marks that schedule as completed.
6. **Rotation and process death:** `WorkoutSession` is `Serializable`. Put it in
   `onSaveInstanceState` with `putSerializable` and read it back the same way.

**Left to the screen (FR-5.2):** keeping the screen on, sound or vibration cues, and pausing on a
phone call.

### History and statistics (FR-5.7)

- `WorkoutLogService`: `getMyLogs(period, cb)` (newest first) and `delete(logId, cb)`.
  - Pass the same `StatsPeriod` as the statistics, so the list shows exactly the logs the totals
    count. `getMyLogs(cb)` without a period lists them all.
  - Each `WorkoutLog` has its exercises as they were done (`getExercises()`). A
    `LoggedExercise` with `isPerSide()` shows "12 reps per side"; logs saved before the flag
    existed read as false.
  - The status is "Completed" or "Partial".
- `StatsService.getStats(StatsPeriod.WEEK | MONTH | ALL_TIME, cb)`: workouts, completed and
  partial counts, total time and kcal. A week starts on the first day of the week of the
  device's locale.

### Scheduled workouts (FR-6)

**Creating and editing schedules:**

| Method | Notes |
|---|---|
| `schedulePremade(name, at, recurrence, addToCalendar, cb)` and `scheduleCustom(id, at, recurrence, addToCalendar, cb)` | Return the schedule id. `recurrence` is null for a one-off workout, or e.g. `Recurrence.weekly()` or `new Recurrence(Recurrence.Frequency.DAILY, 2)`. |
| `reschedule(id, at, cb)`, `setRecurrence(id, recurrence, cb)`, `delete(id, cb)` | The calendar event follows these changes automatically. |

**Listing schedules:**
- `getUpcoming(cb)` lists schedules with an occurrence from now on, sorted by
  `nextOccurrence(...)`.
- `getMySchedules(cb)` lists all of the user's schedules.

**Agenda or calendar views (FR-6.1, FR-6.4):** `getOccurrences(from, to, cb)` returns one
`ScheduleOccurrence` for each date a schedule falls on. The period can be at most a year long.
- Each occurrence has `getSchedule()`, `getStartSeconds()` and `getStatus()`.
- The status is PENDING, COMPLETED, or MISSED (not done 12 hours after its start).
- Mark one done by hand with `markCompleted(scheduleId, occurrence.getStartSeconds(), cb)`, or
  undo it with `markNotCompleted(...)`.

**Device calendar (FR-6.2, FR-6.5):** ask for `ScheduleService.CALENDAR_PERMISSIONS` before
adding events:
```java
ActivityResultLauncher<String[]> askCalendar = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        granted -> schedules.schedulePremade(name, at, recurrence,
                schedules.hasCalendarPermission(), callback));   // denied: schedule without the calendar
askCalendar.launch(ScheduleService.CALENDAR_PERMISSIONS);
```
- **Existing schedules:** use `addToCalendar(id, cb)` and `removeFromCalendar(id, cb)`.
- **Is it in the calendar?** `getCalendarEventId() != null` means the schedule is in this device's
  calendar.
- **Event content:** each event has the workout's exercises and estimated length, plus a reminder
  30 minutes before.

**Email (FR-6.3):** `buildEmail(id, attachIcs, cb)` returns an `Intent`; call
`startActivity(intent)`.
- It opens the user's email app with the workout name, date, time and exercises, addressed to
  their own address.
- With `attachIcs`, it also attaches a calendar invitation file (`.ics`).
- If no email app is installed, the call fails with a message instead.
- The Schedule screen opens it, with the `.ics`, right after a workout is scheduled, and from the
  "Email invite" item of a scheduled workout's menu.

## Changing the database

1. Change `app/src/main/assets/schema.sql`. New installs run it.
2. Raise `SQLManager.DATABASE_VERSION`.
3. Add an `if (oldVersion < N)` step to `SQLManager.onUpgrade` that turns version N-1 into N,
   usually an `ALTER TABLE`.

Every upgrade also refreshes the premade content from `seed.sql` without touching the user's own
data, so a change to the seed alone only needs step 2. `SQLManagerTest` checks that an upgraded
database matches a fresh install.

## Known gaps

- **UI-only features:** the FR-5.2 items above belong to the screens.
