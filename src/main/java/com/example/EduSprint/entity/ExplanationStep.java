package com.example.EduSprint.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "explanation_step")
public class ExplanationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id", unique = true, nullable = false)
    private Long stepId;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @Column(name = "step_number", nullable = false)
    private Short stepNumber;

    @Column(name = "step_description")
    private String explanation;

    @Column(name = "image_name")
    private String imageName;

    public ExplanationStep() {
    }

    public Long getStepId() {
        return stepId;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Short getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Short stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public String toString() {
        return "ExplanationStep{" +
                "stepId=" + stepId +
                ", task=" + task +
                ", stepNumber=" + stepNumber +
                ", explanation='" + explanation + '\'' +
                ", imageName='" + imageName + '\'' +
                '}';
    }
}
