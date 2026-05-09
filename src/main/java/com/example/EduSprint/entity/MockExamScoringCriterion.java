package com.example.EduSprint.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mock_exam_scoring_criterion")
public class MockExamScoringCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterion_id", unique = true, nullable = false)
    private Long criterionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private MockExamQuestion question;

    @Column(name = "criterion_order", nullable = false)
    private Short criterionOrder;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "points", nullable = false)
    private Short points;

    public MockExamScoringCriterion() {
    }

    public Long getCriterionId() {
        return criterionId;
    }

    public MockExamQuestion getQuestion() {
        return question;
    }

    public void setQuestion(MockExamQuestion question) {
        this.question = question;
    }

    public Short getCriterionOrder() {
        return criterionOrder;
    }

    public void setCriterionOrder(Short criterionOrder) {
        this.criterionOrder = criterionOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Short getPoints() {
        return points;
    }

    public void setPoints(Short points) {
        this.points = points;
    }
}
