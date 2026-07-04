package com.example.EduSprint.dto;

public class AdminCourseDTO {
    private Long courseId;
    private String courseName;
    private String courseDescription;
    private long objectiveCount;
    private long taskCount;

    public AdminCourseDTO(Long courseId, String courseName, String courseDescription, long objectiveCount, long taskCount) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.objectiveCount = objectiveCount;
        this.taskCount = taskCount;
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

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public long getObjectiveCount() {
        return objectiveCount;
    }

    public void setObjectiveCount(long objectiveCount) {
        this.objectiveCount = objectiveCount;
    }

    public long getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }
}
