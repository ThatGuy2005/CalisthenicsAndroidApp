package com.example.calisthenicsapp.Session;

import android.os.SystemClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * A workout in progress (FR-5.1, FR-5.3). Pure logic with no UI: the screen calls the action
 * methods, reads the getters, and calls {@link #tick()} about once a second so that timed sets
 * and rests end on their own. Build one with WorkoutSessionService; it starts right away.
 *
 * Every set of every exercise is a WORK step, followed by a REST step: the exercise's rest between
 * sets, or after its last set its rest before the next exercise (no rest after the very last set,
 * none if the rest time is 0). During a REST step,
 * {@link #getCurrentExercise()} and {@link #getCurrentSet()} already show the set that comes next.
 * Rep-based sets end when the user taps done ({@link #completeSet()}). Timed sets end on their
 * own when the time is up, or earlier with completeSet. Rests end on their own or with
 * {@link #skipRest()}.
 *
 * Time comes from SystemClock.elapsedRealtime, which keeps counting while the app is in the
 * background or its process is killed, so a restored session carries on where it was. Paused
 * time is not counted anywhere. Calories only count active (WORK) time of completed sets (FR-5.6).
 *
 * To survive process death (FR-5.2), save it in onSaveInstanceState with putSerializable and
 * read it back with getSerializable.
 */
public class WorkoutSession implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_PARTIAL = "Partial";

    public enum Phase { WORK, REST, FINISHED, CANCELLED }

    /** One exercise of the workout: what to do, and how much of it has been done. */
    public static final class SessionExercise implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final String instructions;
        private final String animationFile;
        private final boolean timed;
        private final Integer targetReps;      // per set; null for timed exercises
        private final Integer targetSeconds;   // per set; null for rep-based exercises
        private final int sets;
        private final int restSeconds;         // between its sets
        private final Integer restAfterSeconds;    // before the next exercise; null = restSeconds
        private final boolean perSide;         // reps or seconds per side; false in older saves
        private final double met;

        private int completedSets;
        private long activeMillis;

        /** The same rest between the sets and before the next exercise. */
        public SessionExercise(String name, String instructions, String animationFile,
                               boolean timed, Integer targetReps, Integer targetSeconds,
                               int sets, int restSeconds, double met) {
            this(name, instructions, animationFile, timed, targetReps, targetSeconds, sets,
                    restSeconds, restSeconds, met);
        }

        public SessionExercise(String name, String instructions, String animationFile,
                               boolean timed, Integer targetReps, Integer targetSeconds,
                               int sets, int restSeconds, int restAfterSeconds, double met) {
            this(name, instructions, animationFile, timed, targetReps, targetSeconds, sets,
                    restSeconds, restAfterSeconds, false, met);
        }

        /** perSide: the reps or seconds are per side, and one set does both sides (FR-4.2). */
        public SessionExercise(String name, String instructions, String animationFile,
                               boolean timed, Integer targetReps, Integer targetSeconds,
                               int sets, int restSeconds, int restAfterSeconds, boolean perSide,
                               double met) {
            this.name = name;
            this.instructions = instructions;
            this.animationFile = animationFile;
            this.timed = timed;
            this.targetReps = targetReps;
            this.targetSeconds = targetSeconds;
            this.sets = sets;
            this.restSeconds = restSeconds;
            this.restAfterSeconds = restAfterSeconds;
            this.perSide = perSide;
            this.met = met;
        }

        public String getName() { return name; }
        public String getInstructions() { return instructions; }
        public String getAnimationFile() { return animationFile; }
        public boolean isTimed() { return timed; }
        public Integer getTargetReps() { return targetReps; }
        /** Per side when {@link #isPerSide()}. */
        public Integer getTargetSeconds() { return targetSeconds; }
        /** Show the target as "12 reps per side" or "30 s per side". */
        public boolean isPerSide() { return perSide; }
        /**
         * How long a timed set runs: the target seconds, twice for a per-side exercise (one
         * countdown for both sides). Null for rep-based exercises.
         */
        public Integer getWorkSeconds() {
            if (!timed || targetSeconds == null) return null;
            return perSide ? 2 * targetSeconds : targetSeconds;
        }
        public int getSets() { return sets; }
        /** The rest between the sets of this exercise. */
        public int getRestSeconds() { return restSeconds; }
        /**
         * The rest after its last set, before the next exercise. A session saved by an older
         * app version has none stored, and uses the rest between sets.
         */
        public int getRestAfterSeconds() {
            return restAfterSeconds == null ? restSeconds : restAfterSeconds;
        }
        public double getMet() { return met; }
        public int getCompletedSets() { return completedSets; }
        public int getActiveSeconds() { return (int) (activeMillis / 1000L); }

        /**
         * MET x kcalPerMetHour x active hours. kcalPerMetHour is the session's
         * {@link WorkoutSession#getKcalPerMetHour()}: the user's BMR / 24, or the body weight in
         * kg (the 1 kcal/kg/h shortcut).
         */
        public double getKcal(double kcalPerMetHour) {
            return met * kcalPerMetHour * (activeMillis / 3_600_000.0);
        }
    }

    private final String workoutName;
    private final List<SessionExercise> exercises;
    private final double weightKg;
    private Double kcalPerMetHour;             // null in sessions saved before BMR: weightKg
    private final long startedAt;              // epoch seconds, for the log

    private Phase phase = Phase.WORK;
    private int exerciseIndex = 0;
    private int setNumber = 1;                 // 1-based, within the current exercise
    private int restTargetSeconds;             // length of the current REST step
    private boolean paused;

    private long runningSince;                 // clock time the current stretch started
    private long stepMillis;                   // current step, up to runningSince
    private long totalMillis;                  // whole workout, up to runningSince
    private String logId;                      // set once saved, so it is never logged twice
    private String customWorkoutId;            // the custom workout it was started from, or null

    private transient LongSupplier clock;

    /**
     * customWorkoutId: the custom workout this runs, or null for a premade one.
     * kcalPerMetHour: kcal burned per MET per hour, see {@link Bmr#kcalPerMetHour}; null uses
     * weightKg (1 kcal per kg per hour).
     */
    public WorkoutSession(String workoutName, String customWorkoutId, List<SessionExercise> exercises,
                          double weightKg, Double kcalPerMetHour) {
        this(workoutName, exercises, weightKg, kcalPerMetHour, SystemClock::elapsedRealtime,
                System.currentTimeMillis() / 1000L);
        this.customWorkoutId = customWorkoutId;
    }

    /** For tests: clock returns milliseconds and must never go backwards. */
    WorkoutSession(String workoutName, List<SessionExercise> exercises, double weightKg,
                   LongSupplier clock, long startedAt) {
        this(workoutName, exercises, weightKg, null, clock, startedAt);
    }

    /** For tests, with a calorie rate. */
    WorkoutSession(String workoutName, List<SessionExercise> exercises, double weightKg,
                   Double kcalPerMetHour, LongSupplier clock, long startedAt) {
        if (exercises == null || exercises.isEmpty()) {
            throw new IllegalArgumentException("A workout needs at least one exercise.");
        }
        this.workoutName = workoutName;
        this.exercises = new ArrayList<>(exercises);
        this.weightKg = weightKg;
        this.kcalPerMetHour = kcalPerMetHour;
        this.clock = clock;
        this.startedAt = startedAt;
        this.runningSince = clock.getAsLong();
    }

    // ---------- Actions ----------

    /**
     * Ends the current WORK set as done and moves on. Returns false outside a WORK step.
     * Like the other step actions, it also resumes a paused session.
     */
    public boolean completeSet() {
        if (phase != Phase.WORK) return false;
        long active = stepElapsedMillis();
        SessionExercise current = getCurrentExercise();
        if (current.getWorkSeconds() != null) {
            active = Math.min(active, current.getWorkSeconds() * 1000L);
        }
        finishWorkStep(active);
        return true;
    }

    /** Ends the current rest early. Returns false outside a REST step. */
    public boolean skipRest() {
        if (phase != Phase.REST) return false;
        startStep(Phase.WORK);
        return true;
    }

    /**
     * Skips the remaining sets of the exercise currently shown (during a rest, that is the
     * exercise coming up) and goes straight to the next exercise, or finishes the workout if
     * it was the last one. The workout is then logged as Partial.
     */
    public boolean skipExercise() {
        if (!isActive()) return false;
        if (exerciseIndex + 1 >= exercises.size()) {
            end(Phase.FINISHED);
        } else {
            exerciseIndex++;
            setNumber = 1;
            startStep(Phase.WORK);
        }
        return true;
    }

    public void pause() {
        if (!isRunning()) return;
        long elapsed = now() - runningSince;
        stepMillis += elapsed;
        totalMillis += elapsed;
        paused = true;
    }

    public void resume() {
        if (!paused || !isActive()) return;
        runningSince = now();
        paused = false;
    }

    /** Stops the workout. A set in progress is not counted. */
    public void cancel() {
        if (isActive()) end(Phase.CANCELLED);
    }

    /**
     * Call about once a second while the session screen is shown. Ends a timed set or a rest
     * whose time is up. Returns true when the step changed, which is the moment for a sound or
     * vibration cue (FR-5.2).
     */
    public boolean tick() {
        if (!isRunning()) return false;
        long elapsed = stepElapsedMillis();
        if (phase == Phase.WORK) {
            SessionExercise current = getCurrentExercise();
            if (current.getWorkSeconds() == null) return false;
            long target = current.getWorkSeconds() * 1000L;
            if (elapsed < target) return false;
            finishWorkStep(target);    // time beyond the target is not active time
            return true;
        }
        if (elapsed < restTargetSeconds * 1000L) return false;
        startStep(Phase.WORK);
        return true;
    }

    // ---------- State for the screen ----------

    public Phase getPhase() { return phase; }
    public boolean isPaused() { return paused; }

    /** WORK or REST: the workout is still going (possibly paused). */
    public boolean isActive() {
        return phase == Phase.WORK || phase == Phase.REST;
    }

    /** FINISHED or CANCELLED. */
    public boolean isOver() {
        return !isActive();
    }

    public String getWorkoutName() { return workoutName; }
    /** The custom workout it was started from, or null for a premade workout. */
    public String getCustomWorkoutId() { return customWorkoutId; }
    public long getStartedAt() { return startedAt; }
    /** kcal per MET per hour: the user's BMR / 24 when known, otherwise the weight in kg. */
    public double getKcalPerMetHour() {
        return kcalPerMetHour == null ? weightKg : kcalPerMetHour;
    }

    /** The exercise being done, or during a rest the one coming next. Null once it is over. */
    public SessionExercise getCurrentExercise() {
        return isActive() ? exercises.get(exerciseIndex) : null;
    }

    /** 1-based set number of the current exercise (during a rest, the set coming next). */
    public int getCurrentSet() { return setNumber; }

    /** The exercise after the current one, for a "next up" preview. Null if there is none. */
    public SessionExercise getNextExercise() {
        if (!isActive() || exerciseIndex + 1 >= exercises.size()) return null;
        return exercises.get(exerciseIndex + 1);
    }

    /** 1-based position of the current exercise, for "Exercise 2 of 6". */
    public int getCurrentExerciseNumber() { return exerciseIndex + 1; }

    public List<SessionExercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }

    /**
     * Seconds left in a timed set or a rest, rounded up so the display reaches 0 exactly when
     * the step ends. Null for a rep-based set and once the workout is over.
     */
    public Integer getRemainingSeconds() {
        long targetMillis;
        if (phase == Phase.REST) {
            targetMillis = restTargetSeconds * 1000L;
        } else if (phase == Phase.WORK && getCurrentExercise().getWorkSeconds() != null) {
            targetMillis = getCurrentExercise().getWorkSeconds() * 1000L;
        } else {
            return null;
        }
        long left = Math.max(0L, targetMillis - stepElapsedMillis());
        return (int) ((left + 999L) / 1000L);
    }

    /** Workout duration so far, rests included, pauses excluded. */
    public int getTotalSeconds() {
        return (int) (totalElapsedMillis() / 1000L);
    }

    public int getCompletedSets() {
        int sum = 0;
        for (SessionExercise e : exercises) sum += e.completedSets;
        return sum;
    }

    public double getTotalKcal() {
        double sum = 0;
        for (SessionExercise e : exercises) sum += e.getKcal(getKcalPerMetHour());
        return sum;
    }

    /**
     * Completed if it finished with every set done, Partial otherwise (skipped or cancelled).
     * Only meaningful once the workout is over.
     */
    public String getStatus() {
        if (phase != Phase.FINISHED) return STATUS_PARTIAL;
        for (SessionExercise e : exercises) {
            if (e.completedSets < e.sets) return STATUS_PARTIAL;
        }
        return STATUS_COMPLETED;
    }

    /** Over with at least one completed set (FR-5.3); otherwise there is nothing to log. */
    public boolean isLoggable() {
        return isOver() && getCompletedSets() > 0;
    }

    /** The id of the workout log this session was saved as, or null if not saved yet. */
    public String getLogId() { return logId; }

    /** Called by WorkoutLogService when it saves the session. */
    public void setLogId(String logId) { this.logId = logId; }

    // ---------- Internals ----------

    private void finishWorkStep(long activeMillis) {
        SessionExercise current = getCurrentExercise();
        current.completedSets++;
        current.activeMillis += activeMillis;

        if (setNumber < current.sets) {
            setNumber++;
            restTargetSeconds = current.restSeconds;
        } else if (exerciseIndex + 1 < exercises.size()) {
            exerciseIndex++;
            setNumber = 1;
            restTargetSeconds = current.getRestAfterSeconds();
        } else {
            end(Phase.FINISHED);
            return;
        }
        startStep(restTargetSeconds > 0 ? Phase.REST : Phase.WORK);
    }

    /** Starts a new step now, folding the time so far into the total. Also un-pauses. */
    private void startStep(Phase next) {
        long now = now();
        if (isRunning()) totalMillis += now - runningSince;
        stepMillis = 0;
        runningSince = now;
        paused = false;
        phase = next;
    }

    private void end(Phase finalPhase) {
        if (isRunning()) totalMillis += now() - runningSince;
        stepMillis = 0;
        paused = false;
        phase = finalPhase;
    }

    private boolean isRunning() {
        return isActive() && !paused;
    }

    private long stepElapsedMillis() {
        return stepMillis + (isRunning() ? now() - runningSince : 0L);
    }

    private long totalElapsedMillis() {
        return totalMillis + (isRunning() ? now() - runningSince : 0L);
    }

    private long now() {
        return clock.getAsLong();
    }

    /** For tests: a deserialized session gets the real clock, which unit tests cannot call. */
    void setClock(LongSupplier clock) {
        this.clock = clock;
    }

    /** The clock is not serializable; a restored session uses the real one again. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        clock = SystemClock::elapsedRealtime;
    }
}
