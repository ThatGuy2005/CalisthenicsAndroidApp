# Calisthenics Workout App - Software Requirements Specification

**Platform:** Android (Android Studio, Java)
**Accounts:** Firebase Authentication (email + password)
**Data:** Cloud Firestore (per-user data, source of truth) + local SQLite (premade content and offline cache)
**Project type:** University project (team of two: front end / back end & database)
**Version:** 1.2

---

## 1. INTRODUCTION

### 1.1 Purpose
This document describes what the Calisthenics Workout App must do. It defines the functional and non-functional requirements that guide design, implementation and testing.

### 1.2 Scope
The app helps users train with bodyweight (calisthenics) exercises. Users create an account, follow premade exercises and workouts, build their own workouts, track their training history (time and estimated calories burned), and schedule workouts in their calendar with an email reminder.

Accounts use Firebase Authentication. Per-user data is stored in Cloud Firestore and cached in a local SQLite database, so the app keeps working offline and syncs when a connection is available.

### 1.3 Definitions
- **Exercise:** A single movement (e.g. push-up, pull-up) with instructions and an animation. Categorized by intensity (MET value), rep-based vs. timed, required equipment, single vs. multiple muscle groups, and a difficulty enum.
- **Workout:** An ordered list of exercises with sets/repetitions or durations. Categorized by target goal, difficulty, and estimated duration.
- **Premade workout:** A workout supplied with the app, not editable by users.
- **Custom workout:** A workout created and owned by a user.
- **Workout log:** A record of a completed workout session.
- **Partial workout:** A workout session terminated before completion, preserving logged sets.

### 1.4 User Roles
- **User:** Registers, logs in, trains, creates custom workouts, views logs and schedules workouts.

There is no admin role. Premade exercises and workouts ship with the app and cannot be edited in the app.

---

## 2. FUNCTIONAL REQUIREMENTS

### 2.1 Authentication and Accounts
- **FR-1.1:** The system shall allow a new user to sign up with a unique username, an email address and a password. Usernames must be 3–20 alphanumeric characters, case-insensitive.
- **FR-1.2:** The system shall collect optional profile data at sign-up: height, weight and gender. Weight is used to estimate calories burned. Gender shall be used exclusively alongside height and weight to calculate Basal Metabolic Rate (BMR) for accurate Metabolic Equivalent of Task (MET) calorie calculations.
- **FR-1.3:** The system shall reject sign-up if the username or email is already in use, the email format is invalid, or the password is too weak (minimum 8 characters).
- **FR-1.4:** The system shall allow a registered user to log in with email and password. Firebase Authentication shall keep the user signed in across app restarts until they log out.
- **FR-1.5:** The system shall display a clear error message on failed login without revealing whether the email or the password was wrong. Repeated failed attempts are throttled by Firebase Authentication.
- **FR-1.6:** The system shall allow a logged-in user to log out.
- **FR-1.7:** The system shall allow a user to view and edit their profile (email, height, weight, gender, password). Changing the email or the password requires the user to sign in again first. Users shall have a global toggle for units (kg/lb, cm/ft).
- **FR-1.8:** The system shall record the account creation date and the last update date for each user.
- **FR-1.9:** Password recovery shall use Firebase Authentication's password reset email.
- **FR-1.10:** The system shall provide a full account deletion option, which permanently deletes the Firebase account, all of the user's data in Firestore, and the local cache on the device.

### 2.2 Password Security
- **FR-2.1:** Passwords shall be handled only by Firebase Authentication. The app shall never store, hash or transmit passwords anywhere other than to Firebase Authentication.
- **FR-2.2:** Passwords shall never be written to logs or shown in the user interface.

### 2.3 Exercises
- **FR-3.1:** The app shall include a library of premade exercises.
- **FR-3.2:** Each exercise shall have: a name, a text description with step-by-step instructions, an animation demonstrating correct form, a target muscle group, and a difficulty level. They shall also define intensity (MET value), rep-based vs. timed behavior, required equipment, and specify if they target single or multiple muscle groups. Difficulty shall use a strict enum (e.g., BEGINNER, INTERMEDIATE, ADVANCED).
- **FR-3.3:** The user shall be able to browse the exercise list and open an exercise's detail screen.
- **FR-3.4:** The user shall be able to search or filter exercises by name, muscle group and difficulty.
- **FR-3.5:** The animation shall play on the exercise detail screen and during the workout. Animations shall be in `.lottie` or optimized `.webp` format to minimize APK size, sourced from open-source/CC0 repositories with proper licensing attribution in the app settings.

### 2.4 Workouts
- **FR-4.1:** The app shall include a set of premade workouts (e.g. beginner, intermediate, advanced, or by muscle group). Workouts shall include a general category, difficulty rating, and estimated duration.
- **FR-4.2:** Each workout shall list its exercises in order, with the number of sets and repetitions or the duration for each, and the rest time between them. ExerciseOrder models shall feature a discriminator to distinguish reps vs. duration, define whether rest is between sets or between distinct exercises, and flag per-side exercises (e.g., lunges).
- **FR-4.3:** Premade workouts shall be read-only. Users shall be able to duplicate a premade workout to instantiate an editable custom copy.
- **FR-4.4:** The user shall be able to create a custom workout by choosing a name and adding exercises from the library.
- **FR-4.5:** The user shall be able to set the order, sets, repetitions or duration, and rest time for each exercise in a custom workout.
- **FR-4.6:** The user shall be able to edit and delete their own custom workouts, and shall not be able to see or modify other users' custom workouts.

### 2.5 In-Workout Behavior & Logs
- **FR-5.1:** The user shall be able to start any workout and be guided through its exercises (current exercise, instructions, animation, timer, and next exercise).
- **FR-5.2:** During an active workout, the app shall keep the screen awake (WakeLock), provide sound/vibration cues at timer boundaries, maintain state if backgrounded, automatically pause on phone call interruptions, and resume cleanly after process death via saved instance state.
- **FR-5.3:** The user shall be able to pause, resume, skip or cancel a workout in progress. Cancelled/skipped workouts where at least one set was completed shall be logged as "Partial".
- **FR-5.4:** When a workout is completed or partially saved, the system shall automatically save a log entry containing: the workout, the date and time, the total duration, and the estimated calories burned.
- **FR-5.5:** To preserve historical integrity, logs shall snapshot the workout data (Cascade/Soft delete rule). If a user edits or deletes a custom workout, existing logs referencing it shall remain intact.
- **FR-5.6:** The estimated calories burned shall be computed from the workout duration, the exercise intensity and the user's body weight. For partial workouts, this is computed based on actual active time and specific MET values of completed exercises.
- **FR-5.7:** The user shall be able to view a history of all their past workouts, sorted from newest to oldest. Summary statistics shall show total workouts, total training time and total calories burned over a selected period (week, month, all time).
- **FR-5.8:** The user shall be able to delete their own log entries.

### 2.6 Scheduling
- **FR-6.1:** The user shall be able to schedule a workout for a chosen date and time. Schedules shall support recurrence rules (e.g., weekly) and maintain an internal status flag (pending/completed/missed).
- **FR-6.2:** The system shall add the scheduled workout as an event in the device's calendar app, saving the resulting calendar event ID in the local database to support downstream updates.
- **FR-6.3:** The user shall be able to send the scheduled workout to their email address, including the workout name, date, time and list of exercises (optionally as a calendar invitation file, .ics).
- **FR-6.4:** The user shall be able to view, edit and cancel scheduled workouts.
- **FR-6.5:** The system shall ask for the required calendar permission and handle a denied permission gracefully.

---

## 3. DATA REQUIREMENTS

- **DR-1:** Premade exercises and workouts shall be stored only in the local SQLite database. Per-user data (profile, custom workouts, workout logs, schedules) shall be stored in Cloud Firestore under users/{uid}, which is the source of truth. Firestore changes shall be mirrored into SQLite, and the app shall read from SQLite.
- **DR-2:** The database shall contain at least the following entities:
  - **Users:** uid (Firebase Authentication id), username (unique), email, height, weight, gender, created date, updated date.
  - **Exercise:** name, description/instructions, animation reference, muscle group, difficulty, intensity (MET), rep_or_timed flag, equipment, single_multi_muscle flag.
  - **Workout:** name, description, type (premade or custom), owner (for custom workouts), category, difficulty_enum, estimated_duration.
  - **ExerciseOrder:** links exercises to a workout with order, sets, repetitions/duration and rest time, discriminator_type, rest_type (inter-set vs inter-exercise), per_side_flag.
  - **Workout log:** id, user, workout, date and time, duration, calories, status (completed/partial), snapshot_data.
  - **Schedule:** id, user, workout, planned date and time, recurrence_rule, calendar_event_id, status.

  Per-user records are identified by their Firestore document id.
- **DR-3:** Dates and times shall be stored as Unix epoch timestamps in seconds. Users shall record their creation and last update time.
- **DR-4:** The database shall enforce data integrity with primary keys, foreign keys and unique constraints. `ON DELETE RESTRICT` shall be used for static exercise dictionaries, and `ON DELETE CASCADE` for user-owned dependencies, excluding Logs which rely on snapshots.
- **DR-5:** The database shall use triggers to automatically update the "updated" timestamp when a record is modified.
- **DR-6:** All database queries shall use parameterized statements to prevent SQL injection.
- **DR-7:** Premade exercises and workouts shall be loaded into the database when the app is first installed. DB versioning shall be managed via SQLiteOpenHelper. Future app versions shall update seed data via schema migrations without overwriting custom user modifications.

---

## 4. NON-FUNCTIONAL REQUIREMENTS

### 4.1 Security & Privacy
- **NFR-1:** Passwords shall be handled only by Firebase Authentication (see section 2.2).
- **NFR-2:** All user input shall be validated before it is processed or stored.
- **NFR-3:** A user shall only be able to access their own personal data. This shall be enforced by Firestore security rules that allow a signed-in user to read and write only users/{their uid}.
- **NFR-4:** **Threat Model:** Local data is protected by the Android app sandbox. Encrypting the local database (e.g. SQLCipher) is out of scope, because the Firestore offline cache on the device is not encrypted either.
- **NFR-5:** **Privacy (GDPR):** As emails and health data are stored in Firestore and cached on the device, a clear privacy policy must be presented. The app shall provide a "Download my data" (JSON/CSV export) and a "Delete Account" function to comply with the right-to-access and right-to-be-forgotten.

### 4.2 Usability
- **NFR-6:** The interface shall be simple and consistent, and usable by a person with no training.
- **NFR-7:** Common actions (start a workout, view the log) shall take at most three taps from the main screen.
- **NFR-8:** Error messages shall be clear and understandable.

### 4.3 Performance & Reliability
- **NFR-9:** Screens shall load in under 2 seconds on a typical device.
- **NFR-10:** Animations shall play smoothly, without noticeable stutter.
- **NFR-11:** Database and network operations shall run off the main (UI) thread so that the interface stays responsive.
- **NFR-12:** Workout data shall not be lost if the app is closed or crashes during use (logs shall be saved as soon as a workout is finished).
- **NFR-13:** The app shall handle missing permissions or no internet access without crashing. Sign-up, sign-in, password reset and email sending require a connection; everything else works offline and syncs when a connection is available.

### 4.4 Compatibility and Maintainability
- **NFR-14:** The app shall run on Android devices from a defined minimum API level (to be decided by the team).
- **NFR-15:** The code shall be organized in separate layers (user interface, business logic, database access) with a dedicated database manager class, so that the front end and back end can be developed independently.
- **NFR-16:** The code shall be documented and follow consistent naming conventions.
