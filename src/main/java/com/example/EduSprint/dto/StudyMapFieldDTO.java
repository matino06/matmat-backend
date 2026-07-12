package com.example.EduSprint.dto;

import java.util.List;

public class StudyMapFieldDTO {
    private Long fieldId;
    private String fieldName;
    private boolean unlocked;
    private List<StudyMapObjectiveDTO> objectives;

    public StudyMapFieldDTO(Long fieldId, String fieldName, boolean unlocked, List<StudyMapObjectiveDTO> objectives) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.unlocked = unlocked;
        this.objectives = objectives;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public List<StudyMapObjectiveDTO> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<StudyMapObjectiveDTO> objectives) {
        this.objectives = objectives;
    }
}
