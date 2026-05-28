package com.example.EduSprint.dto;

public class MockExamAnswerSubmissionDTO {
    private Long questionId;
    private String selectedOption;
    private String answerText;
    private String imagePartName;

    public MockExamAnswerSubmissionDTO() {
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getImagePartName() {
        return imagePartName;
    }

    public void setImagePartName(String imagePartName) {
        this.imagePartName = imagePartName;
    }
}
