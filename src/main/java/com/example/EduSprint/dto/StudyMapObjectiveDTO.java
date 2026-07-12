package com.example.EduSprint.dto;

public class StudyMapObjectiveDTO {
    private Long objectiveId;
    private String objectiveName;
    private Long subfieldId;
    private String subfieldName;
    private boolean unlocked;
    private boolean isMastered;

    public StudyMapObjectiveDTO(Long objectiveId, String objectiveName, Long subfieldId, String subfieldName,
                                boolean unlocked, boolean isMastered) {
        this.objectiveId = objectiveId;
        this.objectiveName = objectiveName;
        this.subfieldId = subfieldId;
        this.subfieldName = subfieldName;
        this.unlocked = unlocked;
        this.isMastered = isMastered;
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

    public Long getSubfieldId() {
        return subfieldId;
    }

    public void setSubfieldId(Long subfieldId) {
        this.subfieldId = subfieldId;
    }

    public String getSubfieldName() {
        return subfieldName;
    }

    public void setSubfieldName(String subfieldName) {
        this.subfieldName = subfieldName;
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
