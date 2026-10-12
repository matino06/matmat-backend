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
    private int streakFreezes;
    private int maxStreakFreezes;
    private int freezeEveryStreakDays;
    private int daysToNextFreeze;
    // Missed days on which a streak freeze kept the streak alive
    private List<LocalDate> frozenDays;
    private LocalDate registrationDate;
    private List<DailyGoalDTO> calendarDays;

    public UserGoalDTO(int dailyGoal, int todayGoal, int todayCompleted, int currentStreak, int longestStreak, int streakMinTasks,
                       int streakFreezes, int maxStreakFreezes, int freezeEveryStreakDays, int daysToNextFreeze, List<LocalDate> frozenDays,
                       LocalDate registrationDate, List<DailyGoalDTO> calendarDays) {
        this.dailyGoal = dailyGoal;
        this.todayGoal = todayGoal;
        this.todayCompleted = todayCompleted;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.streakMinTasks = streakMinTasks;
        this.streakFreezes = streakFreezes;
        this.maxStreakFreezes = maxStreakFreezes;
        this.freezeEveryStreakDays = freezeEveryStreakDays;
        this.daysToNextFreeze = daysToNextFreeze;
        this.frozenDays = frozenDays;
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

    public int getStreakFreezes() {
        return streakFreezes;
    }

    public void setStreakFreezes(int streakFreezes) {
        this.streakFreezes = streakFreezes;
    }

    public int getMaxStreakFreezes() {
        return maxStreakFreezes;
    }

    public void setMaxStreakFreezes(int maxStreakFreezes) {
        this.maxStreakFreezes = maxStreakFreezes;
    }

    public int getFreezeEveryStreakDays() {
        return freezeEveryStreakDays;
    }

    public void setFreezeEveryStreakDays(int freezeEveryStreakDays) {
        this.freezeEveryStreakDays = freezeEveryStreakDays;
    }

    public int getDaysToNextFreeze() {
        return daysToNextFreeze;
    }

    public void setDaysToNextFreeze(int daysToNextFreeze) {
        this.daysToNextFreeze = daysToNextFreeze;
    }

    public List<LocalDate> getFrozenDays() {
        return frozenDays;
    }

    public void setFrozenDays(List<LocalDate> frozenDays) {
        this.frozenDays = frozenDays;
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
