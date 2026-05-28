package com.example.EduSprint.dto;

import java.util.List;

public class MockExamSubmitRequestDTO {
    private List<MockExamAnswerSubmissionDTO> answers;

    public MockExamSubmitRequestDTO() {
    }

    public List<MockExamAnswerSubmissionDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<MockExamAnswerSubmissionDTO> answers) {
        this.answers = answers;
    }
}
