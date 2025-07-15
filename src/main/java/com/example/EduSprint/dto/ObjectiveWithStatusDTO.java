package com.example.EduSprint.dto;

public class ObjectiveWithStatusDTO {
    private String subfieldName;
    private Long objectiveId;
    private String objectiveName;
    private boolean unlocked;

    // Getters & Setters
    public ObjectiveWithStatusDTO(String subfieldName, Long objectiveId, String objectiveName, boolean unlocked) {
        this.subfieldName = subfieldName;
        this.objectiveId = objectiveId;
        this.objectiveName = objectiveName;
        this.unlocked = unlocked;
    }

    public String getSubfieldName() {
        return subfieldName;
    }

    public void setSubfieldName(String subfieldName) {
        subfieldName = subfieldName;
    }

    public Long getObjectiveId() {
        return objectiveId;
    }

    public void setObjectiveId(Long objectiveId) {
        this.objectiveId = objectiveId;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public void setObjectiveName(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}