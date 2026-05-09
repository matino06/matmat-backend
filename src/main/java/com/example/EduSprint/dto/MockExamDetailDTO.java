package com.example.EduSprint.dto;

import java.util.List;

public class MockExamDetailDTO {
    private Long examId;
    private String title;
    private String subtitle;
    private Short year;
    private String term;
    private Short durationMinutes;
    private Short totalPoints;
    private List<MockExamQuestionDTO> questions;

    public MockExamDetailDTO(Long examId, String title, String subtitle, Short year, String term,
                             Short durationMinutes, Short totalPoints,
                             List<MockExamQuestionDTO> questions) {
        this.examId = examId;
        this.title = title;
        this.subtitle = subtitle;
        this.year = year;
        this.term = term;
        this.durationMinutes = durationMinutes;
        this.totalPoints = totalPoints;
        this.questions = questions;
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

    public List<MockExamQuestionDTO> getQuestions() {
        return questions;
    }
}
