package com.example.EduSprint.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyGoalDTO {
    private LocalDate date;
    private boolean goalMet;
    private boolean partial;
    private int completed;
    private int goal;
    // The day counts towards the streak (enough tasks solved in any course), independent of goalMet.
    private boolean streakDay;
    private List<CourseDayCountDTO> courses;

    public DailyGoalDTO(LocalDate date, boolean goalMet, boolean partial, int completed, int goal, boolean streakDay, List<CourseDayCountDTO> courses) {
        this.date = date;
        this.goalMet = goalMet;
        this.partial = partial;
        this.completed = completed;
        this.goal = goal;
        this.streakDay = streakDay;
        this.courses = courses;
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

    public int getGoal() {
        return goal;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }

    public boolean isStreakDay() {
        return streakDay;
    }

    public void setStreakDay(boolean streakDay) {
        this.streakDay = streakDay;
    }

    public List<CourseDayCountDTO> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseDayCountDTO> courses) {
        this.courses = courses;
    }
}
