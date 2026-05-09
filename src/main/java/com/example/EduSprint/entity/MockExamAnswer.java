package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
    name = "mock_exam_answer",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"})
)
public class MockExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id", unique = true, nullable = false)
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private MockExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private MockExamQuestion question;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "answer_image_filename")
    private String answerImageFilename;

    @Column(name = "score_awarded", nullable = false)
    private Short scoreAwarded;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "ai_grading_status", nullable = false)
    private String aiGradingStatus;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    public MockExamAnswer() {
    }

    public Long getAnswerId() {
        return answerId;
    }

    public MockExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(MockExamAttempt attempt) {
        this.attempt = attempt;
    }

    public MockExamQuestion getQuestion() {
        return question;
    }

    public void setQuestion(MockExamQuestion question) {
        this.question = question;
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

    public String getAnswerImageFilename() {
        return answerImageFilename;
    }

    public void setAnswerImageFilename(String answerImageFilename) {
        this.answerImageFilename = answerImageFilename;
    }

    public Short getScoreAwarded() {
        return scoreAwarded;
    }

    public void setScoreAwarded(Short scoreAwarded) {
        this.scoreAwarded = scoreAwarded;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        this.isCorrect = correct;
    }

    public String getAiGradingStatus() {
        return aiGradingStatus;
    }

    public void setAiGradingStatus(String aiGradingStatus) {
        this.aiGradingStatus = aiGradingStatus;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
