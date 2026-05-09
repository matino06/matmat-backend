package com.example.EduSprint.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "mock_exam_attempt")
public class MockExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id", unique = true, nullable = false)
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private MockExam exam;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    @Column(name = "total_score")
    private Short totalScore;

    @Column(name = "max_score")
    private Short maxScore;

    @Column(name = "score_part1")
    private Short scorePart1;

    @Column(name = "score_part2")
    private Short scorePart2;

    @Column(name = "score_part3")
    private Short scorePart3;

    public MockExamAttempt() {
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public MockExam getExam() {
        return exam;
    }

    public void setExam(MockExam exam) {
        this.exam = exam;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean completed) {
        this.isCompleted = completed;
    }

    public Short getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Short totalScore) {
        this.totalScore = totalScore;
    }

    public Short getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Short maxScore) {
        this.maxScore = maxScore;
    }

    public Short getScorePart1() {
        return scorePart1;
    }

    public void setScorePart1(Short scorePart1) {
        this.scorePart1 = scorePart1;
    }

    public Short getScorePart2() {
        return scorePart2;
    }

    public void setScorePart2(Short scorePart2) {
        this.scorePart2 = scorePart2;
    }

    public Short getScorePart3() {
        return scorePart3;
    }

    public void setScorePart3(Short scorePart3) {
        this.scorePart3 = scorePart3;
    }
}
