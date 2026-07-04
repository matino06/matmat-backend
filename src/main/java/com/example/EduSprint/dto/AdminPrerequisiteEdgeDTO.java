package com.example.EduSprint.dto;

public class AdminPrerequisiteEdgeDTO {
    // The objective depends on the prerequisite (prerequisiteId must be learned before objectiveId).
    private Long objectiveId;
    private Long prerequisiteId;

    public AdminPrerequisiteEdgeDTO(Long objectiveId, Long prerequisiteId) {
        this.objectiveId = objectiveId;
        this.prerequisiteId = prerequisiteId;
    }

    public Long getObjectiveId() {
        return objectiveId;
    }

    public void setObjectiveId(Long objectiveId) {
        this.objectiveId = objectiveId;
    }

    public Long getPrerequisiteId() {
        return prerequisiteId;
    }

    public void setPrerequisiteId(Long prerequisiteId) {
        this.prerequisiteId = prerequisiteId;
    }
}
