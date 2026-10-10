package com.example.calisthenicsapp.CoreComponents;

public class Exercise {
    public Exercise(String name, String instructions, boolean isTimed,
                    Integer secIndex, Integer repsIndex, double met, String animationFile) {
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
    public Integer getDefaultSeconds() {
        return this.secIndex;
    }
    public Integer getDefaultReps() {
        return this.repsIndex;
    }
    public Double getMet() {
        return this.met;
    }
    public String getAnimationFile() {
        return this.animationFile;
    }
    private String name;
    private String instructions;
    private boolean isTimed;
    private Integer secIndex;
    private Integer repsIndex;
    private Double met;
    private String animationFile;

}
