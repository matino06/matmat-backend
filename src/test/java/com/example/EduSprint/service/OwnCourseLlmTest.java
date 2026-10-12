package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OwnCourseLlmTest {

    private final OpenRouterClient client = mock(OpenRouterClient.class);
    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final OwnCourseLlm llm = new OwnCourseLlm(client, queue);
    private final BuildJob job = new BuildJob(1, 2, 3, "read_page", "str. 1", 1, true, 1, null);

    @Test
    void anthropicSystemPromptIsMarkedForCaching() {
        ObjectNode body = llm.body("anthropic/claude-haiku-5.5", "SYS", "USER", OwnCourseLlm.Options.of("low", 4000));
        var system = body.path("messages").path(0);
        assertEquals("SYS", system.path("content").path(0).path("text").asText());
        assertEquals("ephemeral", system.path("content").path(0).path("cache_control").path("type").asText());
        assertEquals("low", body.path("reasoning").path("effort").asText());
        assertEquals("json_object", body.path("response_format").path("type").asText());
        assertTrue(body.path("usage").path("include").asBoolean());
    }

    @Test
    void openAiSystemPromptIsPlainTextBecauseItCachesByItself() {
        ObjectNode body = llm.body("openai/gpt-6.1-sol", "SYS", "USER", new OwnCourseLlm.Options(null, 1000, false));
        assertEquals("SYS", body.path("messages").path(0).path("content").asText());
        assertEquals("USER", body.path("messages").path(1).path("content").asText());
        assertTrue(body.path("reasoning").isMissingNode());
        assertTrue(body.path("response_format").isMissingNode());
    }

    @Test
    void everyCallRecordsItsCost() throws Exception {
        when(client.complete(any())).thenReturn(new ObjectMapper().readTree("""
                {"choices":[{"message":{"content":"{\\"ok\\":true}"}}],
                 "usage":{"cost":0.0123,"prompt_tokens_details":{"cached_tokens":900}}}"""));

        OwnCourseLlm.Result result = llm.chat(job, "openai/gpt-6.1-sol", "S", "U", OwnCourseLlm.Options.of("medium", 1000));

        assertEquals("{\"ok\":true}", result.content());
        assertEquals(900, result.cachedTokens());
        verify(queue).recordCost(eq(job), eq("openai/gpt-6.1-sol"), eq(new BigDecimal("0.0123")));
    }

    @Test
    void keyLimitBecomesAGlobalLimit() {
        when(client.complete(any())).thenThrow(new RuntimeException("OpenRouter 403: {\"error\":{\"message\":\"Key limit exceeded\"}}"));
        assertThrows(OwnCourseLlm.GlobalLimitException.class,
                () -> llm.chat(job, "openai/gpt-6.1-sol", "S", "U", OwnCourseLlm.Options.of("low", 100)));
        verify(queue, never()).recordCost(any(), any(), any());
    }

    @Test
    void globalLimitDetection() {
        assertTrue(OwnCourseLlm.isGlobalLimit("OpenRouter 402: insufficient credits"));
        assertTrue(OwnCourseLlm.isGlobalLimit("OpenRouter 403: Budget limit exceeded (daily limit)"));
        assertFalse(OwnCourseLlm.isGlobalLimit("OpenRouter 403: forbidden model"));
        assertFalse(OwnCourseLlm.isGlobalLimit("OpenRouter 500: upstream error"));
        assertFalse(OwnCourseLlm.isGlobalLimit(null));
    }
}
