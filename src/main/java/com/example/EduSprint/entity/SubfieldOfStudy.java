package com.example.EduSprint.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subfield_of_study")
public class SubfieldOfStudy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subfield_id", nullable = false)
    private Long subfieldId;

    @ManyToOne
    @JoinColumn(name = "field_id", nullable = false)
    private FieldOfStudy field;

    @Column(name = "subfield_name", unique = true, nullable = false)
    private String subfieldName;

    public SubfieldOfStudy() {
    }

    public Long getId() {
        return subfieldId;
    }

    public FieldOfStudy getField() {
        return field;
    }

    public void setField(FieldOfStudy field) {
        this.field = field;
    }

    public String getSubfieldName() {
        return subfieldName;
    }

    public void setSubfieldName(String subfieldName) {
        this.subfieldName = subfieldName;
    }

    @Override
    public String toString() {
        return field.toString() + " SubfieldOfStudy=" + subfieldName;
    }
}