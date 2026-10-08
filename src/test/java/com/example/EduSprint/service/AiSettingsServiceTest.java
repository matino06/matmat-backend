package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiSettingsUpdateDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiSettingsServiceTest {

    private static AiSettingsUpdateDTO settings(String chatModel, Integer dailyLimit, Integer chatMaxTokens,
                                                String chatEffort, String gradingEffort) {
        return new AiSettingsUpdateDTO(chatModel, dailyLimit, chatMaxTokens, chatEffort,
                "anthropic/claude-haiku-5.5", "anthropic/claude-haiku-5.5", 4096, gradingEffort);
    }

    @Test
    void validSettingsAreNormalised() {
        AiSettingsUpdateDTO valid = AiSettingsService.validate(
                settings("  anthropic/claude-haiku-5.5 ", 0, 10000, " High ", ""));
        assertEquals("anthropic/claude-haiku-5.5", valid.chatModel());
        assertEquals(0, valid.chatDailyLimit());
        assertEquals("high", valid.chatReasoningEffort());
        assertNull(valid.gradingReasoningEffort());
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(ResponseStatusException.class, () -> AiSettingsService.validate(null));
        assertThrows(ResponseStatusException.class,
                () -> AiSettingsService.validate(settings(" ", 30, 10000, "medium", "medium")));
        assertThrows(ResponseStatusException.class,
                () -> AiSettingsService.validate(settings("anthropic/claude haiku", 30, 10000, "medium", "medium")));
        assertThrows(ResponseStatusException.class,
                () -> AiSettingsService.validate(settings("anthropic/claude-haiku-5.5", -1, 10000, "medium", "medium")));
        assertThrows(ResponseStatusException.class,
                () -> AiSettingsService.validate(settings("anthropic/claude-haiku-5.5", 30, 0, "medium", "medium")));
        assertThrows(ResponseStatusException.class,
                () -> AiSettingsService.validate(settings("anthropic/claude-haiku-5.5", 30, null, "medium", "medium")));
        assertThrows(ResponseStatusException.class,
                () -> AiSettingsService.validate(settings("anthropic/claude-haiku-5.5", 30, 10000, "maximum", "medium")));
    }
}
