package com.example.EduSprint.dto;

import java.time.LocalDate;

public class ObjectiveDTO {
    private String title;
    private LocalDate dueDate;
    private Short lastQ;

    public ObjectiveDTO(String title, LocalDate dueDate, Short lastQ) {
        this.title = title;
        this.dueDate = dueDate;
        this.lastQ = lastQ;
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

    public Short getLastQ() {
        return lastQ;
    }

    public void setLastQ(Short lastQ) {
        this.lastQ = lastQ;
    }

    @Override
    public String toString() {
        return "TaskDTO{" +
                "title='" + title + '\'' +
                ", dueDate=" + dueDate +
                ", lastQ=" + lastQ +
                '}';
    }
}
