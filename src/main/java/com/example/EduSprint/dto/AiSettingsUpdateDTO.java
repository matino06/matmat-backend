package com.example.EduSprint.dto;

// Sve postavke se šalju odjednom; prazan ili null *ReasoningEffort = model razmišlja po svom zadanom.
public record AiSettingsUpdateDTO(
        String chatModel,
        Integer chatDailyLimit,
        Integer chatMaxTokens,
        String chatReasoningEffort,
        String gradingModelText,
        String gradingModelVision,
        Integer gradingMaxTokens,
        String gradingReasoningEffort) {
}
