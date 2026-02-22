package com.example.EduSprint.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserCourseGoalId implements Serializable {
    private Long account;   // matches the name of the @Id field "account"
    private Long course;    // matches the name of the @Id field "course"

    public UserCourseGoalId() {}

    public UserCourseGoalId(Long account, Long course) {
        this.account = account;
        this.course = course;
    }

    // Getters and setters
    public Long getAccount() { return account; }
    public void setAccount(Long account) { this.account = account; }

    public Long getCourse() { return course; }
    public void setCourse(Long course) { this.course = course; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCourseGoalId that = (UserCourseGoalId) o;
        return Objects.equals(account, that.account) &&
                Objects.equals(course, that.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, course);
    }
}