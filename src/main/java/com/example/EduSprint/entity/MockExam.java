package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "mock_exam")
public class MockExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id", unique = true, nullable = false)
    private Long examId;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "year")
    private Short year;

    @Column(name = "term")
    private String term;

    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Column(name = "total_points", nullable = false)
    private Short totalPoints;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MockExam() {
    }

    public Long getExamId() {
        return examId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Short getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Short durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Short getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Short totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean published) {
        this.isPublished = published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
