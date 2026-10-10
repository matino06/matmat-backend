package com.example.EduSprint.dto;

import java.time.Instant;
import java.util.List;

public class MockExamSubmitRequestDTO {
    private List<MockExamAnswerSubmissionDTO> answers;
    private Instant startedAt;

    public MockExamSubmitRequestDTO() {
    }

    public List<MockExamAnswerSubmissionDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<MockExamAnswerSubmissionDTO> answers) {
        this.answers = answers;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
}
