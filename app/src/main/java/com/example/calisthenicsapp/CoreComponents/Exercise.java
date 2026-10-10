package com.example.calisthenicsapp.CoreComponents;

public class Exercise {
    public Exercise(String name, String instructions, boolean isTimed,
                    int secIndex, int repsIndex, double met, String animationFile) {
        this.name = name;
        this.instructions = instructions;
        this.isTimed = isTimed;
        this.secIndex = secIndex;
        this.repsIndex = repsIndex;
        this.met = met;
        this.animationFile = animationFile;
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
    public int getDefaultSeconds() {
        return this.secIndex;
    }
    public int getDefaultReps() {
        return this.repsIndex;
    }
    public double getMet() {
        return this.met;
    }
    public String getAnimationFile() {
        return this.animationFile;
    }
    private String name;
    private String instructions;
    private boolean isTimed;
    private int secIndex;
    private int repsIndex;
    private double met;
    private String animationFile;

}
