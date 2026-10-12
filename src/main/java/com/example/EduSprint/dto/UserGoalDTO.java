package com.example.EduSprint.dto;

import java.time.LocalDate;
import java.util.List;

public class UserGoalDTO {

    private int dailyGoal;
    private int todayGoal;
    private int todayCompleted;
    private int currentStreak;
    private int longestStreak;
    private int streakMinTasks;
    private LocalDate registrationDate;
    private List<DailyGoalDTO> calendarDays;

    public UserGoalDTO(int dailyGoal, int todayGoal, int todayCompleted, int currentStreak, int longestStreak, int streakMinTasks, LocalDate registrationDate, List<DailyGoalDTO> calendarDays) {
        this.dailyGoal = dailyGoal;
        this.todayGoal = todayGoal;
        this.todayCompleted = todayCompleted;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.streakMinTasks = streakMinTasks;
        this.registrationDate = registrationDate;
        this.calendarDays = calendarDays;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }

    public void setDailyGoal(int dailyGoal) {
        this.dailyGoal = dailyGoal;
    }

    public int getTodayGoal() {
        return todayGoal;
    }

    public void setTodayGoal(int todayGoal) {
        this.todayGoal = todayGoal;
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

    public int getStreakMinTasks() {
        return streakMinTasks;
    }

    public void setStreakMinTasks(int streakMinTasks) {
        this.streakMinTasks = streakMinTasks;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public List<DailyGoalDTO> getCalendarDays() {
        return calendarDays;
    }

    public void setCalendarDays(List<DailyGoalDTO> calendarDays) {
        this.calendarDays = calendarDays;
    }
}
