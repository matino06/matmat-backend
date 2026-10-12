package com.example.EduSprint.dto;

public class CourseDayCountDTO {
    private Long courseId;
    private String courseName;
    private int completed;

    public CourseDayCountDTO(Long courseId, String courseName, int completed) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.completed = completed;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }
}
