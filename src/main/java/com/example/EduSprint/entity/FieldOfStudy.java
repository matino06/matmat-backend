package com.example.EduSprint.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "field_of_study")
public class FieldOfStudy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_id", unique = true, nullable = false)
    private Long fieldId;

    @Column(name = "field_name", unique = true, nullable = false)
    private String fieldName;

    public Long getId() {
        return fieldId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setId(Long id) {
        this.fieldId = id;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return "FieldOfStudy=" + fieldName;
    }
}