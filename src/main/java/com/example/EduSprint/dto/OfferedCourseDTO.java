package com.example.EduSprint.dto;

/** An official course the apps offer, with how they show it (sql/2026-10-28-predmeti-iz-baze.sql). */
public record OfferedCourseDTO(long courseId, String courseName, String subject, String shortName, String maturaLevel,
                               String levelHint, String badge, Integer hue, String tagline, String formulaPdf,
                               boolean published) {
}
