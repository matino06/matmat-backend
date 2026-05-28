package com.example.EduSprint.dto;

import java.time.Instant;

public class MockExamAttemptSummaryDTO {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private Short examYear;
    private String examTerm;
    private Instant submittedAt;
    private Short totalScore;
    private Short maxScore;
    private String gradingStatus;

    public MockExamAttemptSummaryDTO(Long attemptId, Long examId, String examTitle, Short examYear,
                                     String examTerm, Instant submittedAt, Short totalScore,
                                     Short maxScore, String gradingStatus) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.examYear = examYear;
        this.examTerm = examTerm;
        this.submittedAt = submittedAt;
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        this.gradingStatus = gradingStatus;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public Short getExamYear() {
        return examYear;
    }

    public String getExamTerm() {
        return examTerm;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Short getTotalScore() {
        return totalScore;
    }

    public Short getMaxScore() {
        return maxScore;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }
}
