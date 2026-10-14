package com.example.calisthenicsapp.CoreComponents;

public class Exercise {
    /**
     * defaultSeconds is used when isTimed is true, defaultReps when it is false.
     * The other one is null (the database column is NULL).
     */
    public Exercise(String name, String instructions, boolean isTimed,
                    Integer defaultSeconds, Integer defaultReps, double met,
                    String animationFile, String muscleGroup, Difficulty difficulty) {
        this.name = name;
        this.instructions = instructions;
        this.isTimed = isTimed;
        this.defaultSeconds = defaultSeconds;
        this.defaultReps = defaultReps;
        this.met = met;
        this.animationFile = animationFile;
        this.muscleGroup = muscleGroup;
        this.difficulty = difficulty;
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
    private String name;
    private String instructions;
    private boolean isTimed;
    private Integer defaultSeconds;
    private Integer defaultReps;
    private double met;
    private String animationFile;
    private String muscleGroup;
    private Difficulty difficulty;
}