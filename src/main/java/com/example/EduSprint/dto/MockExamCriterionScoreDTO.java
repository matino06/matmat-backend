package com.example.EduSprint.dto;

public class MockExamCriterionScoreDTO {
    private Long criterionId;
    private String description;
    private Short maxPoints;
    private Short pointsAwarded;
    private String aiFeedback;

    public MockExamCriterionScoreDTO(Long criterionId, String description, Short maxPoints,
                                     Short pointsAwarded, String aiFeedback) {
        this.criterionId = criterionId;
        this.description = description;
        this.maxPoints = maxPoints;
        this.pointsAwarded = pointsAwarded;
        this.aiFeedback = aiFeedback;
    }

    public Long getCriterionId() {
        return criterionId;
    }

    public String getDescription() {
        return description;
    }

    public Short getMaxPoints() {
        return maxPoints;
    }

    public Short getPointsAwarded() {
        return pointsAwarded;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }
}
