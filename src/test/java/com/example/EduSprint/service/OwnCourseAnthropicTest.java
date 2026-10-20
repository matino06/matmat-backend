package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnCourseAnthropicTest {

    @Test
    void onlySolIsReplacedAndOnlyWhileAnthropicIsAvailable() {
        AnthropicApi api = mock(AnthropicApi.class);
        OwnCourseAnthropic anthropic = new OwnCourseAnthropic(api, "claude-sonnet-5-5", "high", "openai/gpt-6.1-sol");
        when(api.available()).thenReturn(true);
        assertTrue(anthropic.replaces("openai/gpt-6.1-sol"));
        assertFalse(anthropic.replaces("anthropic/claude-haiku-5.5"));
        when(api.available()).thenReturn(false);
        assertFalse(anthropic.replaces("openai/gpt-6.1-sol"));
    }
}
