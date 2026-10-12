# Calisthenics Workout App - Software Requirements Specification

**Platform:** Android (Android Studio, Java)[cite: 1]
**Local data:** SQLite[cite: 1]
**Project type:** University project (team of two: front end / back end & database)[cite: 1]
**Version:** 1.1 (Updated)

---

## 1. INTRODUCTION

### 1.1 Purpose
This document describes what the Calisthenics Workout App must do[cite: 1]. It defines the functional and non-functional requirements that guide design, implementation and testing[cite: 1].

### 1.2 Scope
The app helps users train with bodyweight (calisthenics) exercises[cite: 1]. Users create an account, follow premade exercises and workouts, build their own workouts, track their training history (time and estimated calories burned), and schedule workouts in their calendar with an email reminder[cite: 1].

### 1.3 Definitions
- **Exercise:** A single movement (e.g. push-up, pull-up) with instructions and an animation[cite: 1]. Categorized by intensity (MET value), rep-based vs. timed, required equipment, single vs. multiple muscle groups, and a difficulty enum.
- **Workout:** An ordered list of exercises with sets/repetitions or durations[cite: 1]. Categorized by target goal, difficulty, and estimated duration.
- **Premade workout:** A workout supplied with the app, not editable by users[cite: 1].
- **Custom workout:** A workout created and owned by a user[cite: 1].
- **Workout log:** A record of a completed workout session[cite: 1].
- **Partial workout:** A workout session terminated before completion, preserving logged sets.

### 1.4 User Roles
- **User:** Registers, logs in, trains, creates custom workouts, views logs and schedules workouts[cite: 1].
- **Admin:** Has all User rights, and can manage the premade exercises and workouts[cite: 1].

---

## 2. FUNCTIONAL REQUIREMENTS

### 2.1 Authentication and Accounts
- **FR-1.1:** The system shall allow a new user to sign up with a unique username, an email address and a password[cite: 1]. Usernames must be 3–20 alphanumeric characters, case-insensitive.
- **FR-1.2:** The system shall collect optional profile data at sign-up: height, weight and gender[cite: 1]. Weight is used to estimate calories burned[cite: 1]. Gender shall be used exclusively alongside height and weight to calculate Basal Metabolic Rate (BMR) for accurate Metabolic Equivalent of Task (MET) calorie calculations. 
- **FR-1.3:** The system shall reject sign-up if the username or email is already in use, the email format is invalid, or the password is too weak (minimum 8 characters)[cite: 1].
- **FR-1.4:** The system shall allow a registered user to log in with username and password[cite: 1]. The system shall support "stay logged in" behavior using a persistent local encrypted session token.
- **FR-1.5:** The system shall display a clear error message on failed login without revealing whether the username or the password was wrong[cite: 1]. After 5 consecutive failed login attempts, the account shall be locked out for 15 minutes (throttling).
- **FR-1.6:** The system shall allow a logged-in user to log out[cite: 1].
- **FR-1.7:** The system shall allow a user to view and edit their profile (email, height, weight, gender, password)[cite: 1]. Users shall have a global toggle for units (kg/lb, cm/ft).
- **FR-1.8:** The system shall record the account creation date and the last update date for each user[cite: 1].
- **FR-1.9:** Because offline email recovery is impossible, password recovery shall be handled via two local security questions established during sign-up. 
- **FR-1.10:** The system shall provide a full account deletion option, which permanently wipes all user-associated local data.

### 2.2 Password Security (Hashing)
- **FR-2.1:** Passwords shall NEVER be stored in plain text[cite: 1]. Only a salted hash of the password shall be stored in the database[cite: 1].
- **FR-2.2:** A strong, purpose-built password hashing algorithm shall be used (bcrypt, PBKDF2 or Argon2)[cite: 1]. Fast general-purpose hashes such as plain MD5 or SHA-1 are not acceptable[cite: 1]. Argon2id is preferred; if falling back to JDK-native PBKDF2, use HMAC-SHA256. 
- **FR-2.3:** Parameters: Minimum 16-byte salt, 32-byte output length, and iterations tuned to require ~500ms processing time on an average target device. Each password shall be hashed with a unique, randomly generated salt[cite: 1].
- **FR-2.4:** At login, the system shall hash the entered password with the stored salt and compare it to the stored hash, using a constant-time comparison[cite: 1].
- **FR-2.5:** Passwords and hashes shall never be written to logs or shown in the user interface[cite: 1].

### 2.3 Exercises
- **FR-3.1:** The app shall include a library of premade exercises[cite: 1].
- **FR-3.2:** Each exercise shall have: a name, a text description with step-by-step instructions, an animation demonstrating correct form, a target muscle group, and a difficulty level[cite: 1]. They shall also define intensity (MET value), rep-based vs. timed behavior, required equipment, and specify if they target single or multiple muscle groups. Difficulty shall use a strict enum (e.g., BEGINNER, INTERMEDIATE, ADVANCED).
- **FR-3.3:** The user shall be able to browse the exercise list and open an exercise's detail screen[cite: 1].
- **FR-3.4:** The user shall be able to search or filter exercises by name, muscle group and difficulty[cite: 1].
- **FR-3.5:** The animation shall play on the exercise detail screen and during the workout[cite: 1]. Animations shall be in `.lottie` or optimized `.webp` format to minimize APK size, sourced from open-source/CC0 repositories with proper licensing attribution in the app settings.

### 2.4 Workouts
- **FR-4.1:** The app shall include a set of premade workouts (e.g. beginner, intermediate, advanced, or by muscle group)[cite: 1]. Workouts shall include a general category, difficulty rating, and estimated duration.
- **FR-4.2:** Each workout shall list its exercises in order, with the number of sets and repetitions or the duration for each, and the rest time between them[cite: 1]. ExerciseOrder models shall feature a discriminator to distinguish reps vs. duration, define whether rest is between sets or between distinct exercises, and flag per-side exercises (e.g., lunges).
- **FR-4.3:** Premade workouts shall be read-only for regular users[cite: 1]. Users shall be able to duplicate a premade workout to instantiate an editable custom copy.
- **FR-4.4:** The user shall be able to create a custom workout by choosing a name and adding exercises from the library[cite: 1].
- **FR-4.5:** The user shall be able to set the order, sets, repetitions or duration, and rest time for each exercise in a custom workout[cite: 1].
- **FR-4.6:** The user shall be able to edit and delete their own custom workouts, and shall not be able to see or modify other users' custom workouts[cite: 1].

### 2.5 In-Workout Behavior & Logs
- **FR-5.1:** The user shall be able to start any workout and be guided through its exercises (current exercise, instructions, animation, timer, and next exercise)[cite: 1].
- **FR-5.2:** During an active workout, the app shall keep the screen awake (WakeLock), provide sound/vibration cues at timer boundaries, maintain state if backgrounded, automatically pause on phone call interruptions, and resume cleanly after process death via saved instance state.
- **FR-5.3:** The user shall be able to pause, resume, skip or cancel a workout in progress[cite: 1]. Cancelled/skipped workouts where at least one set was completed shall be logged as "Partial".
- **FR-5.4:** When a workout is completed or partially saved, the system shall automatically save a log entry containing: the workout, the date and time, the total duration, and the estimated calories burned[cite: 1]. 
- **FR-5.5:** To preserve historical integrity, logs shall snapshot the workout data (Cascade/Soft delete rule). If a user edits or deletes a custom workout, existing logs referencing it shall remain intact.
- **FR-5.6:** The estimated calories burned shall be computed from the workout duration, the exercise intensity and the user's body weight[cite: 1]. For partial workouts, this is computed based on actual active time and specific MET values of completed exercises.
- **FR-5.7:** The user shall be able to view a history of all their past workouts, sorted from newest to oldest[cite: 1]. Summary statistics shall show total workouts, total training time and total calories burned over a selected period (week, month, all time)[cite: 1].
- **FR-5.8:** The user shall be able to delete their own log entries[cite: 1].

### 2.6 Scheduling
- **FR-6.1:** The user shall be able to schedule a workout for a chosen date and time[cite: 1]. Schedules shall support recurrence rules (e.g., weekly) and maintain an internal status flag (pending/completed/missed).
- **FR-6.2:** The system shall add the scheduled workout as an event in the device's calendar app[cite: 1], saving the resulting calendar event ID in the local database to support downstream updates.
- **FR-6.3:** The user shall be able to send the scheduled workout to their email address, including the workout name, date, time and list of exercises (optionally as a calendar invitation file, .ics)[cite: 1].
- **FR-6.4:** The user shall be able to view, edit and cancel scheduled workouts[cite: 1].
- **FR-6.5:** The system shall ask for the required calendar permission and handle a denied permission gracefully[cite: 1].

---

## 3. DATA REQUIREMENTS

- **DR-1:** Data shall be stored locally in an SQLite database[cite: 1].
- **DR-2:** The database shall contain at least the following entities[cite: 1]:
  - **Users:** username (unique), email, password hash and salt, height, weight, gender, role (Admin or User), created date, updated date[cite: 1].
  - **Exercise:** name, description/instructions, animation reference, muscle group, difficulty[cite: 1], intensity (MET), rep_or_timed flag, equipment, single_multi_muscle flag.
  - **Workout:** name, description, type (premade or custom), owner (for custom workouts)[cite: 1], category, difficulty_enum, estimated_duration.
  - **ExerciseOrder:** links exercises to a workout with order, sets, repetitions/duration and rest time[cite: 1], discriminator_type, rest_type (inter-set vs inter-exercise), per_side_flag.
  - **Workout log:** user, workout, date and time, duration, calories[cite: 1], status (completed/partial), snapshot_data.
  - **Schedule:** user, workout, planned date and time[cite: 1], recurrence_rule, calendar_event_id, status.
- **DR-3:** Dates and times shall be stored as Unix epoch timestamps[cite: 1] in milliseconds. Created timestamps shall be implemented on all tables.
- **DR-4:** The database shall enforce data integrity with primary keys, foreign keys and unique constraints[cite: 1]. `ON DELETE RESTRICT` shall be used for static exercise dictionaries, and `ON DELETE CASCADE` for user-owned dependencies, excluding Logs which rely on snapshots.
- **DR-5:** The database shall use triggers to automatically update the "updated" timestamp when a record is modified[cite: 1].
- **DR-6:** All database queries shall use parameterized statements to prevent SQL injection[cite: 1].
- **DR-7:** Premade exercises and workouts shall be loaded into the database when the app is first installed[cite: 1]. DB versioning shall be managed via Room migrations (or SQLiteOpenHelper). Future app versions shall update seed data via schema migrations without overwriting custom user modifications.

---

## 4. NON-FUNCTIONAL REQUIREMENTS

### 4.1 Security & Privacy
- **NFR-1:** Passwords shall be hashed and salted (see section 2.2)[cite: 1].
- **NFR-2:** All user input shall be validated before it is processed or stored[cite: 1].
- **NFR-3:** A user shall only be able to access their own personal data[cite: 1].
- **NFR-4:** **Threat Model:** Because the SQLite file is readable on a rooted device, SQLCipher is required to encrypt the local database, protecting user health metrics and credentials.
- **NFR-5:** **Privacy (GDPR):** As emails and health data are stored locally, a clear privacy policy must be presented. The app shall provide a "Download my data" (JSON/CSV export) and a "Delete Account" function to comply with the right-to-access and right-to-be-forgotten.

### 4.2 Usability
- **NFR-6:** The interface shall be simple and consistent, and usable by a person with no training[cite: 1].
- **NFR-7:** Common actions (start a workout, view the log) shall take at most three taps from the main screen[cite: 1].
- **NFR-8:** Error messages shall be clear and understandable[cite: 1].

### 4.3 Performance & Reliability
- **NFR-9:** Screens shall load in under 2 seconds on a typical device[cite: 1].
- **NFR-10:** Animations shall play smoothly, without noticeable stutter[cite: 1].
- **NFR-11:** Database operations shall run off the main (UI) thread so that the interface stays responsive[cite: 1].
- **NFR-12:** Workout data shall not be lost if the app is closed or crashes during use (logs shall be saved as soon as a workout is finished)[cite: 1]. 
- **NFR-13:** The app shall handle missing permissions or no internet access without crashing[cite: 1]. Only email sending requires a connection[cite: 1].

### 4.4 Compatibility and Maintainability
- **NFR-14:** The app shall run on Android devices from a defined minimum API level (to be decided by the team)[cite: 1].
- **NFR-15:** The code shall be organized in separate layers (user interface, business logic, database access) with a dedicated database manager class, so that the front end and back end can be developed independently[cite: 1].
- **NFR-16:** The code shall be documented and follow consistent naming conventions[cite: 1].