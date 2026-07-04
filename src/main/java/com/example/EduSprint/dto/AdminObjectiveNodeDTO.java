package com.example.EduSprint.dto;

public class AdminObjectiveNodeDTO {
    private Long objectiveId;
    private String objectiveName;
    private Long subfieldId;
    private String subfieldName;
    private Long fieldId;
    private String fieldName;
    private long taskCount;

    public AdminObjectiveNodeDTO(Long objectiveId, String objectiveName, Long subfieldId, String subfieldName,
                                 Long fieldId, String fieldName, long taskCount) {
        this.objectiveId = objectiveId;
        this.objectiveName = objectiveName;
        this.subfieldId = subfieldId;
        this.subfieldName = subfieldName;
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.taskCount = taskCount;
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

    public long getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }
}
