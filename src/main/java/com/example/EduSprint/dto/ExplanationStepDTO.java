package com.example.EduSprint.dto;

public class ExplanationStepDTO {

    private String explanation;
    private Short stepNumber;
    private String imageName;

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Short getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Short stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public String toString() {
        return "ExplanationStepDTO{" +
                "explanation='" + explanation + '\'' +
                ", stepNumber=" + stepNumber +
                ", imageName='" + imageName + '\'' +
                '}';
    }
}
