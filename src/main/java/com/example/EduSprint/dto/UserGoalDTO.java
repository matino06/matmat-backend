package com.example.EduSprint.dto;

import java.time.LocalDate;
import java.util.List;

public class UserGoalDTO {

    private int dailyGoal;
    private int todayGoal;
    private LocalDate registrationDate;
    private List<DailyGoalDTO> calendarDays;

    public UserGoalDTO(int dailyGoal, int todayGoal, LocalDate registrationDate, List<DailyGoalDTO> calendarDays) {
        this.dailyGoal = dailyGoal;
        this.todayGoal = todayGoal;
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
