package com.example.EduSprint.dto;

// Nakon uploada na S3 frontend prijavi materijal; backend provjeri veličinu i broj stranica.
public record MaterialRegisterRequest(String key, String originalName, Boolean availableOnExam) {
}
