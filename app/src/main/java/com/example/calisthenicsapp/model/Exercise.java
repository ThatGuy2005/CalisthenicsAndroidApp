package com.example.calisthenicsapp.model;

/**
 * One exercise of the library (FR-3.1, FR-3.2), a row of the Exercise table. The library is
 * premade: it ships in seed.sql and is the same for every user, so it is never synced. The name is
 * unique and is how workouts and logs refer to an exercise.
 *
 * An exercise is either timed (a plank: hold for defaultSeconds) or rep-based (push-ups:
 * defaultReps per set); see {@link #isTimed()}.
 */
public class Exercise {
    /**
     * defaultSeconds is used when isTimed is true, defaultReps when it is false.
     * The other one is null (the database column is NULL).
     * isPerSide: the reps or seconds are per side (each leg or arm), as in lunges (FR-4.2).
     * equipment: what it needs besides the floor; null means none. isMultiMuscle: it works
     * several muscle groups rather than mainly one (FR-3.2).
     */
    public Exercise(String name, String instructions, boolean isTimed,
                    Integer defaultSeconds, Integer defaultReps, double met,
                    String animationFile, String muscleGroup, Difficulty difficulty,
                    boolean isPerSide, Equipment equipment, boolean isMultiMuscle) {
        this.name = name;
        this.instructions = instructions;
        this.isTimed = isTimed;
        this.defaultSeconds = defaultSeconds;
        this.defaultReps = defaultReps;
        this.met = met;
        this.animationFile = animationFile;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
        this.isPerSide = isPerSide;
        this.equipment = equipment == null ? Equipment.NONE : equipment;
        this.isMultiMuscle = isMultiMuscle;
    }
    /** The unique name, e.g. "Push-Up". */
    public String getName() {
        return this.name;
    }
    /** How to do it, step by step, with a tip (FR-3.3). */
    public String getInstructions() {
        return this.instructions;
    }
    /** True when a set is held for a time, false when it is counted in reps. */
    public boolean isTimed() {
        return this.isTimed;
    }
    /** Suggested seconds per set for a timed exercise; null for a rep-based one. */
    public Integer getDefaultSeconds() {
        return this.defaultSeconds;
    }
    /** Suggested reps per set for a rep-based exercise; null for a timed one. */
    public Integer getDefaultReps() {
        return this.defaultReps;
    }
    /**
     * The metabolic equivalent (MET): how many times the resting energy use the exercise takes. The
     * session uses it for the calorie estimate (FR-5.4).
     */
    public double getMet() {
        return this.met;
    }
    /**
     * Path of the demonstration animation in the assets, e.g. "animations/push_up.webp" (animated
     * WebP or .lottie); null if there is none (FR-3.3).
     */
    public String getAnimationFile() {
        return this.animationFile;
    }
    /** The main muscle group it trains, e.g. "Chest" (FR-3.2). */
    public String getMuscleGroup() {
        return this.muscleGroup;
    }
    /** Null if the exercise has none. */
    public Difficulty getDifficulty() {
        return this.difficulty;
    }
    /**
     * True when the reps or seconds are per side, e.g. 12 lunges with each leg: one set does
     * both sides (FR-4.2).
     */
    public boolean isPerSide() {
        return this.isPerSide;
    }
    /** What it needs besides the floor; Equipment.NONE, never null. */
    public Equipment getEquipment() {
        return this.equipment;
    }
    /**
     * True for a compound movement that works several muscle groups (push-ups, burpees), false
     * for one that mainly works getMuscleGroup() (crunches).
     */
    public boolean isMultiMuscle() {
        return this.isMultiMuscle;
    }
    private String name;
    private String instructions;
    private boolean isTimed;
    private Integer defaultSeconds;
    private Integer defaultReps;
    private double met;
    private String animationFile;
    private String muscleGroup;
    private Difficulty difficulty;
    private boolean isPerSide;
    private Equipment equipment;
    private boolean isMultiMuscle;
}