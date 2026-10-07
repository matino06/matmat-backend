package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenRouterClientTest {

    @Test
    void blankReasoningEffortMeansModelDefault() {
        assertNull(OpenRouterClient.reasoningEffort(null));
        assertNull(OpenRouterClient.reasoningEffort(""));
        assertNull(OpenRouterClient.reasoningEffort("  "));
    }

    @Test
    void reasoningEffortIsNormalised() {
        assertEquals("high", OpenRouterClient.reasoningEffort(" High "));
        assertEquals("none", OpenRouterClient.reasoningEffort("none"));
    }

    @Test
    void unknownReasoningEffortIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> OpenRouterClient.reasoningEffort("maximum"));
    }
}
