package com.example.EduSprint.dto;

public class MockExamSubmitResponseDTO {
    private Long attemptId;
    private String gradingStatus;

    public MockExamSubmitResponseDTO(Long attemptId, String gradingStatus) {
        this.attemptId = attemptId;
        this.gradingStatus = gradingStatus;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }
}
