package com.example.EduSprint.dto;

import java.util.List;

// Cijela matura za admin uređivanje, s rješenjima koja učenik ne vidi tijekom ispita.
public record AdminMockExamDetailDTO(
        Long examId,
        String title,
        String subtitle,
        Short year,
        String term,
        Short durationMinutes,
        Short totalPoints,
        Boolean isPublished,
        List<AdminMockExamQuestionDTO> questions) {
}
