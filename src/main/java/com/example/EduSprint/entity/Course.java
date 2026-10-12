package com.example.EduSprint.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "course_description")
    private String courseDescription;

    @Column(name = "subject", length = 30)
    private String subject;

    // Vlastiti predmet: vlasnik je kreator; null znači službeni predmet. Samo id, da se
    // predmet može vraćati kao JSON bez povlačenja cijelog računa.
    @Column(name = "owner_account_id")
    private Long ownerAccountId;

    // Stanje izrade vlastitog predmeta (nov | izrada | nacrt | potvrden); null za službene.
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "learning_mode", length = 20)
    private String learningMode;

    // Razina vlastitog predmeta (os78 | ss); null za službene.
    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "exam_date")
    private LocalDate examDate;

    public Course(String courseName, String courseDescription) {
        this.courseName = courseName;
        this.courseDescription = courseDescription;
    }

    public Course() {
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Long getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(Long ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLearningMode() {
        return learningMode;
    }

    public void setLearningMode(String learningMode) {
        this.learningMode = learningMode;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public boolean isOwnedBy(Long accountId) {
        return ownerAccountId != null && ownerAccountId.equals(accountId);
    }
}
