package com.example.EduSprint.dto;

import java.util.List;

public class MockExamAnswerResultDTO {
    private Long questionId;
    private String questionNumber;
    private String questionType;
    private String questionText;
    private Short maxPoints;
    private List<MockExamQuestionImageDTO> questionImages;
    private String selectedOption;
    private String correctOption;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answerText;
    private String correctAnswer;
    private List<MockExamQuestionImageDTO> correctAnswerImages;
    private String answerImageUrl;
    private Short scoreAwarded;
    private Boolean isCorrect;
    private String aiGradingStatus;
    private String aiFeedback;
    private String solutionExplanation;
    private List<MockExamCriterionScoreDTO> criterionScores;

    public MockExamAnswerResultDTO(Long questionId, String questionNumber, String questionType,
                                   String questionText, Short maxPoints,
                                   List<MockExamQuestionImageDTO> questionImages,
                                   String selectedOption, String correctOption,
                                   String optionA, String optionB, String optionC, String optionD,
                                   String answerText, String correctAnswer,
                                   List<MockExamQuestionImageDTO> correctAnswerImages,
                                   String answerImageUrl, Short scoreAwarded, Boolean isCorrect,
                                   String aiGradingStatus, String aiFeedback,
                                   String solutionExplanation,
                                   List<MockExamCriterionScoreDTO> criterionScores) {
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionType = questionType;
        this.questionText = questionText;
        this.maxPoints = maxPoints;
        this.questionImages = questionImages;
        this.selectedOption = selectedOption;
        this.correctOption = correctOption;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.answerText = answerText;
        this.correctAnswer = correctAnswer;
        this.correctAnswerImages = correctAnswerImages;
        this.answerImageUrl = answerImageUrl;
        this.scoreAwarded = scoreAwarded;
        this.isCorrect = isCorrect;
        this.aiGradingStatus = aiGradingStatus;
        this.aiFeedback = aiFeedback;
        this.solutionExplanation = solutionExplanation;
        this.criterionScores = criterionScores;
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

    public Short getMaxPoints() {
        return maxPoints;
    }

    public List<MockExamQuestionImageDTO> getQuestionImages() {
        return questionImages;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public String getCorrectOption() {
        return correctOption;
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

    public String getAnswerText() {
        return answerText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<MockExamQuestionImageDTO> getCorrectAnswerImages() {
        return correctAnswerImages;
    }

    public String getAnswerImageUrl() {
        return answerImageUrl;
    }

    public Short getScoreAwarded() {
        return scoreAwarded;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public String getAiGradingStatus() {
        return aiGradingStatus;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public String getSolutionExplanation() {
        return solutionExplanation;
    }

    public List<MockExamCriterionScoreDTO> getCriterionScores() {
        return criterionScores;
    }
}
