package com.example.EduSprint.dto;

import java.time.LocalDate;

public class ObjectiveDTO {
    private String title;
    private LocalDate dueDate;
    private Short lastGrade;

    public ObjectiveDTO(String title, LocalDate dueDate, Short lastGrade) {
        this.title = title;
        this.dueDate = dueDate;
        this.lastGrade = lastGrade;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Short getLastGrade() {
        return lastGrade;
    }

    public void setLastGrade(Short lastGrade) {
        this.lastGrade = lastGrade;
    }

    @Override
    public String toString() {
        return "TaskDTO{" +
                "title='" + title + '\'' +
                ", dueDate=" + dueDate +
                ", lastGrade=" + lastGrade +
                '}';
    }
}
