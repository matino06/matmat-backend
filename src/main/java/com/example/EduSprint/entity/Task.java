package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id", unique = true, nullable = false)
    private Integer taskId;

    @ManyToOne
    @JoinColumn(name = "objective_id", nullable = false)
    private LearningObjective objective;

    @Column(name = "task_text")
    private String taskText;

    @Column(name = "explanation")
    private String explanation;

    public Task() {
    }

    public Integer getId() {
        return taskId;
    }

    public LearningObjective getObjective() {
        return objective;
    }

    public void setObjective(LearningObjective objective) {
        this.objective = objective;
    }

    public String getTaskText() {
        return taskText;
    }

    public void setTaskText(String taskText) {
        this.taskText = taskText;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }


    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                ", objective=" + objective +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}