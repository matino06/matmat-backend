package com.example.EduSprint.dto;

import java.time.Instant;

/**
 * One report in the admin list. {@code kind} is "chat" (the chat fields are set) or "mock_exam" (the mock exam
 * fields are set).
 */
public record AiReportDTO(
        Long reportId,
        String kind,
        String reason,
        String note,
        String source,
        Instant createdAt,
        String accountEmail,
        // chat
        Long messageId,
        Long conversationId,
        Long taskId,
        String model,
        String question,
        String answer,
        // mock exam
        Long attemptId,
        Long questionId,
        String questionText,
        String answerText,
        String answerImageFilename,
        Short scoreAwarded,
        Short maxPoints,
        String aiFeedback) {
}
