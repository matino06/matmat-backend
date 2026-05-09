package com.example.EduSprint.dto;

import java.util.List;

public class MockExamQuestionDTO {
    private Long questionId;
    private String questionNumber;
    private String questionType;
    private String questionText;
    private Short points;
    private Short sortOrder;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private List<MockExamQuestionImageDTO> images;
    private List<MockExamQuestionDTO> subQuestions;

    public MockExamQuestionDTO(Long questionId, String questionNumber, String questionType,
                               String questionText, Short points, Short sortOrder,
                               String optionA, String optionB, String optionC, String optionD,
                               List<MockExamQuestionImageDTO> images,
                               List<MockExamQuestionDTO> subQuestions) {
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionType = questionType;
        this.questionText = questionText;
        this.points = points;
        this.sortOrder = sortOrder;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.images = images;
        this.subQuestions = subQuestions;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public Short getPoints() {
        return points;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public List<MockExamQuestionImageDTO> getImages() {
        return images;
    }

    public List<MockExamQuestionDTO> getSubQuestions() {
        return subQuestions;
    }
}
