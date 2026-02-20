package com.example.EduSprint.dto;

import java.time.LocalDate;
import java.util.List;

public class UserGoalDTO {

    private int dailyGoal;
    private LocalDate registrationDate;
    private List<DailyGoalDTO> calendarDays;

    public UserGoalDTO(int dailyGoal, LocalDate registrationDate, List<DailyGoalDTO> calendarDays) {
        this.dailyGoal = dailyGoal;
        this.registrationDate = registrationDate;
        this.calendarDays = calendarDays;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }

    public void setDailyGoal(int dailyGoal) {
        this.dailyGoal = dailyGoal;
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
