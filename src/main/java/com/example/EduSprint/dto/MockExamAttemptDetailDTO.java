package com.example.EduSprint.dto;

import java.time.Instant;
import java.util.List;

public class MockExamAttemptDetailDTO {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private String examSubtitle;
    private Short examYear;
    private String examTerm;
    private Instant startedAt;
    private Instant submittedAt;
    private Short totalScore;
    private Short maxScore;
    private String gradingStatus;
    private List<MockExamAnswerResultDTO> answers;

    public MockExamAttemptDetailDTO(Long attemptId, Long examId, String examTitle, String examSubtitle,
                                    Short examYear, String examTerm, Instant startedAt,
                                    Instant submittedAt, Short totalScore, Short maxScore,
                                    String gradingStatus, List<MockExamAnswerResultDTO> answers) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.examSubtitle = examSubtitle;
        this.examYear = examYear;
        this.examTerm = examTerm;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        this.gradingStatus = gradingStatus;
        this.answers = answers;
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

    public String getExamSubtitle() {
        return examSubtitle;
    }

    public Short getExamYear() {
        return examYear;
    }

    public String getExamTerm() {
        return examTerm;
    }

    public Instant getStartedAt() {
        return startedAt;
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

    public List<MockExamAnswerResultDTO> getAnswers() {
        return answers;
    }
}
