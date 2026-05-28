package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "mock_exam_question")
public class MockExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id", unique = true, nullable = false)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private MockExam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private MockExamQuestion parent;

    @Column(name = "question_number", nullable = false)
    private String questionNumber;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "points", nullable = false)
    private Short points;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(name = "option_a", columnDefinition = "TEXT")
    private String optionA;

    @Column(name = "option_b", columnDefinition = "TEXT")
    private String optionB;

    @Column(name = "option_c", columnDefinition = "TEXT")
    private String optionC;

    @Column(name = "option_d", columnDefinition = "TEXT")
    private String optionD;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "correct_option", length = 1)
    private String correctOption;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "answer_notes", columnDefinition = "TEXT")
    private String answerNotes;

    @Column(name = "solution_explanation", columnDefinition = "TEXT")
    private String solutionExplanation;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<MockExamQuestion> subQuestions;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<MockExamQuestionImage> images;

    public MockExamQuestion() {
    }

    public Long getQuestionId() {
        return questionId;
    }

    public MockExam getExam() {
        return exam;
    }

    public void setExam(MockExam exam) {
        this.exam = exam;
    }

    public MockExamQuestion getParent() {
        return parent;
    }

    public void setParent(MockExamQuestion parent) {
        this.parent = parent;
    }

    public String getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Short getPoints() {
        return points;
    }

    public void setPoints(Short points) {
        this.points = points;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getAnswerNotes() {
        return answerNotes;
    }

    public void setAnswerNotes(String answerNotes) {
        this.answerNotes = answerNotes;
    }

    public String getSolutionExplanation() {
        return solutionExplanation;
    }

    public void setSolutionExplanation(String solutionExplanation) {
        this.solutionExplanation = solutionExplanation;
    }

    public List<MockExamQuestion> getSubQuestions() {
        return subQuestions;
    }

    public List<MockExamQuestionImage> getImages() {
        return images;
    }
}
