package com.example.EduSprint.dto;

public class ObjectiveWithStatusDTO {
    private String fieldName;
    private String subfieldName;
    private Long objectiveId;
    private String objectiveName;
    private boolean unlocked;
    private boolean isMastered;

    // Getters & Setters
    public ObjectiveWithStatusDTO(String fieldName, String subfieldName, Long objectiveId, String objectiveName, boolean unlocked, boolean isMastered) {
        this.fieldName = fieldName;
        this.subfieldName = subfieldName;
        this.objectiveId = objectiveId;
        this.objectiveName = objectiveName;
        this.unlocked = unlocked;
        this.isMastered = isMastered;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
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

    public boolean getIsMastered() {
        return isMastered;
    }

    public void setIsMastered(boolean isMastered) {
        this.isMastered = isMastered;
    }
}