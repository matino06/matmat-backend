package com.example.EduSprint.dto;

// Novi vlastiti predmet: naziv, predmet (matematika, fizika, …, ostalo) i razina (os78 | ss).
public record OwnCourseCreateDTO(String name, String subject, String level) {
}
