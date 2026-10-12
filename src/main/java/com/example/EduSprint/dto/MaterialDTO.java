package com.example.EduSprint.dto;

// Materijal vlastitog predmeta kako ga vidi kreator prije pokretanja izrade.
public record MaterialDTO(long materialId, String originalName, String contentType, long sizeBytes,
                          int pageCount, boolean availableOnExam) {
}
