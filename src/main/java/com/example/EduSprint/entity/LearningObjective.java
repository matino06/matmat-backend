package com.example.EduSprint.entity;

import jakarta.persistence.*;

import javax.security.auth.Subject;

@Entity
public class LearningObjective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objective_id", nullable = false, unique = true)
    private Long objectiveId;

    @ManyToOne
    @JoinColumn(name = "subfield_id", nullable = false)
    private SubfieldOfStudy subfield;

    @Column(name = "objective_name", unique = true, nullable = false)
    private String objectiveName;

    public LearningObjective() {
    }

    public Long getObjectiveId() {
        return objectiveId;
    }

    public SubfieldOfStudy getSubfield() {
        return subfield;
    }

    public void setSubfield(SubfieldOfStudy subfield) {
        this.subfield = subfield;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public void setObjectiveName(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    @Override
    public String toString() {
        return subfield.toString() + " LearningObjective=" + objectiveName;
    }
}
