package com.example.EduSprint.dto;

public class MockExamSummaryDTO {
    private Long examId;
    private String title;
    private String subtitle;
    private Short year;
    private String term;
    private Short durationMinutes;
    private Short totalPoints;

    public MockExamSummaryDTO(Long examId, String title, String subtitle, Short year, String term,
                              Short durationMinutes, Short totalPoints) {
        this.examId = examId;
        this.title = title;
        this.subtitle = subtitle;
        this.year = year;
        this.term = term;
        this.durationMinutes = durationMinutes;
        this.totalPoints = totalPoints;
    }

    public Long getExamId() {
        return examId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Short getYear() {
        return year;
    }

    public String getTerm() {
        return term;
    }

    public Short getDurationMinutes() {
        return durationMinutes;
    }

    public Short getTotalPoints() {
        return totalPoints;
    }
}
