package com.example.EduSprint.dto;

// Matura na admin popisu; za razliku od MockExamSummaryDTO uključuje i neobjavljene.
public record AdminMockExamSummaryDTO(
        Long examId,
        String title,
        String subtitle,
        Short year,
        String term,
        Short durationMinutes,
        Short totalPoints,
        Boolean isPublished) {
}
