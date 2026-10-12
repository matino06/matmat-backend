package com.example.EduSprint.dto;

import com.example.EduSprint.entity.Course;

// Vlastiti predmet kako ga vidi kreator na /moji-predmeti i u sidebaru.
public record OwnCourseDTO(Long courseId, String courseName, String subject, String level,
                           String status, String learningMode) {

    public static OwnCourseDTO of(Course c) {
        return new OwnCourseDTO(c.getCourseId(), c.getCourseName(), c.getSubject(), c.getLevel(),
                c.getStatus(), c.getLearningMode());
    }
}
