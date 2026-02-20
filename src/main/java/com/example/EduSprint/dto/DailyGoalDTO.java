package com.example.EduSprint.dto;

import java.time.LocalDate;

public class DailyGoalDTO {
    private LocalDate date;
    private boolean goalMet;
    private boolean partial;
    private int completed;

    public DailyGoalDTO(LocalDate date, boolean goalMet, boolean partial, int completed) {
        this.date = date;
        this.goalMet = goalMet;
        this.partial = partial;
        this.completed = completed;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isGoalMet() {
        return goalMet;
    }

    public void setGoalMet(boolean goalMet) {
        this.goalMet = goalMet;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }
}
