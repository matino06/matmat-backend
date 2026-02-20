package com.example.EduSprint.dto;

import java.util.List;

public class UserGoalDTO {

    private int dailyGoal;
    private int todayCompleted;
    private int currentStreak;
    private int longestStreak;
    private int totalDaysActive;
    private List<DailyGoalDTO> calendarDays;

    public UserGoalDTO(int dailyGoal, int todayCompleted, int currentStreak, int longestStreak, int totalDaysActive, List<DailyGoalDTO> calendarDays) {
        this.dailyGoal = dailyGoal;
        this.todayCompleted = todayCompleted;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.totalDaysActive = totalDaysActive;
        this.calendarDays = calendarDays;
    }

    public UserGoalDTO(List<DailyGoalDTO> calendarDays) {
        this.calendarDays = calendarDays;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }

    public void setDailyGoal(int dailyGoal) {
        this.dailyGoal = dailyGoal;
    }

    public int getTodayCompleted() {
        return todayCompleted;
    }

    public void setTodayCompleted(int todayCompleted) {
        this.todayCompleted = todayCompleted;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public int getTotalDaysActive() {
        return totalDaysActive;
    }

    public void setTotalDaysActive(int totalDaysActive) {
        this.totalDaysActive = totalDaysActive;
    }

    public List<DailyGoalDTO> getCalendarDays() {
        return calendarDays;
    }

    public void setCalendarDays(List<DailyGoalDTO> calendarDays) {
        this.calendarDays = calendarDays;
    }
}
