package com.example.EduSprint.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@IdClass(UserCourseGoalId.class)
public class UserCourseGoal {
    @Id
    @ManyToOne
    @JoinColumn(name = "account_id")
    @JsonIgnore
    private Account account;

    @Id
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "daily_goal", nullable = false)
    private Integer dailyGoal;

    public UserCourseGoal(Account account, Course course, Integer dailyGoal) {
        this.account = account;
        this.course = course;
        this.dailyGoal = dailyGoal;
    }

    public UserCourseGoal() {}

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Integer getDailyGoal() {
        return dailyGoal;
    }

    public void setDailyGoal(Integer dailyGoal) {
        this.dailyGoal = dailyGoal;
    }

    @Override
    public String toString() {
        return "UserCourseGoal{" +
                "account=" + account +
                ", course=" + course +
                ", dailyGoal=" + dailyGoal +
                '}';
    }
}
