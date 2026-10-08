package com.example.EduSprint.dto;

import java.time.Instant;

// Trenutne AI postavke; *ReasoningEffort null = model razmišlja po svom zadanom.
public record AiSettingsDTO(
        String chatModel,
        Integer chatDailyLimit,
        Integer chatMaxTokens,
        String chatReasoningEffort,
        String gradingModelText,
        String gradingModelVision,
        Integer gradingMaxTokens,
        String gradingReasoningEffort,
        Instant updatedAt,
        String updatedByEmail) {
}
