package com.example.EduSprint.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "mock_exam_criterion_score",
    uniqueConstraints = @UniqueConstraint(columnNames = {"answer_id", "criterion_id"})
)
public class MockExamCriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterion_score_id", unique = true, nullable = false)
    private Long criterionScoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private MockExamAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private MockExamScoringCriterion criterion;

    @Column(name = "points_awarded", nullable = false)
    private Short pointsAwarded;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    public MockExamCriterionScore() {
    }

    public Long getCriterionScoreId() {
        return criterionScoreId;
    }

    public MockExamAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(MockExamAnswer answer) {
        this.answer = answer;
    }

    public MockExamScoringCriterion getCriterion() {
        return criterion;
    }

    public void setCriterion(MockExamScoringCriterion criterion) {
        this.criterion = criterion;
    }

    public Short getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Short pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }
}
