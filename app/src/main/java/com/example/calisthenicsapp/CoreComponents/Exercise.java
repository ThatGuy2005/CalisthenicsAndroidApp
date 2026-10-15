package com.example.calisthenicsapp.CoreComponents;

public class Exercise {
    /**
     * defaultSeconds is used when isTimed is true, defaultReps when it is false.
     * The other one is null (the database column is NULL).
     */
    public Exercise(String name, String instructions, boolean isTimed,
                    Integer defaultSeconds, Integer defaultReps, double met,
                    String animationFile, String muscleGroup, Difficulty difficulty) {
        this(name, instructions, isTimed, defaultSeconds, defaultReps, met, animationFile,
                muscleGroup, difficulty, false);
    }
    /** isPerSide: the reps or seconds are per side (each leg or arm), as in lunges (FR-4.2). */
    public Exercise(String name, String instructions, boolean isTimed,
                    Integer defaultSeconds, Integer defaultReps, double met,
                    String animationFile, String muscleGroup, Difficulty difficulty,
                    boolean isPerSide) {
        this(name, instructions, isTimed, defaultSeconds, defaultReps, met, animationFile,
                muscleGroup, difficulty, isPerSide, Equipment.NONE, false);
    }
    /**
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
    public String getName() {
        return this.name;
    }
    public String getInstructions() {
        return this.instructions;
    }
    public boolean isTimed() {
        return this.isTimed;
    }
    public Integer getDefaultSeconds() {
        return this.defaultSeconds;
    }
    public Integer getDefaultReps() {
        return this.defaultReps;
    }
    public double getMet() {
        return this.met;
    }
    public String getAnimationFile() {
        return this.animationFile;
    }
    public String getMuscleGroup() {
        return this.muscleGroup;
    }
    /** "Beginner", "Intermediate", "Advanced", or null. */
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