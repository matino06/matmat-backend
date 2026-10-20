package com.example.EduSprint.service;

import com.anthropic.core.JsonValue;
import com.anthropic.core.http.Headers;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.errors.UnexpectedStatusCodeException;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnthropicApiTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private AnthropicApi api(String key, BigDecimal spent) {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(BigDecimal.class))).thenReturn(spent);
        return new AnthropicApi(jdbc, key, new BigDecimal("100"), new BigDecimal("2"));
    }

    @Test
    void costCountsCacheWritesAndReadsAtTheirOwnPrice() {
        // Sonnet 5.5: 1M ulaza 2 $, 1M izlaza 10 $, 1M zapisa u cache 2,5 $, 1M čitanja iz cachea 0,2 $
        assertEquals(new BigDecimal("14.700000"), AnthropicApi.cost("claude-sonnet-5-5", 1_000_000, 1_000_000, 1_000_000, 1_000_000));
        assertEquals(new BigDecimal("0.010000"), AnthropicApi.cost("claude-sonnet-5-5", 0, 1_000, 0, 0));
        // Haiku 5.5: 0,10 $ ulaz, 0,50 $ izlaz
        assertEquals(new BigDecimal("0.600000"), AnthropicApi.cost("claude-haiku-5-5", 1_000_000, 1_000_000, 0, 0));
    }

    @Test
    void openRouterClaudeIdsMapToAnthropicIdsOtherModelsStayOnOpenRouter() {
        assertEquals("claude-haiku-5-5", AnthropicApi.apiModel("anthropic/claude-haiku-5.5"));
        assertEquals("claude-sonnet-5-5", AnthropicApi.apiModel("anthropic/claude-sonnet-5.5:nitro"));
        assertEquals("claude-opus-4-8", AnthropicApi.apiModel("anthropic/claude-opus-4.8"));
        assertNull(AnthropicApi.apiModel("xiaomi/mimo-v2.6-pro"));
        assertNull(AnthropicApi.apiModel("openai/gpt-6.1-sol"));
        // bez cijene (i bez adaptive thinkinga) ostaje na OpenRouteru
        assertNull(AnthropicApi.apiModel("anthropic/claude-haiku-4.5"));
        assertNull(AnthropicApi.apiModel(null));
    }

    @Test
    void claudeGoesToAnthropicOnlyWhileCreditIsAboveTheMinimum() {
        assertTrue(api("sk-ant-test", new BigDecimal("97.99")).route("anthropic/claude-haiku-5.5").isPresent());
        assertTrue(api("sk-ant-test", new BigDecimal("98.00")).route("anthropic/claude-haiku-5.5").isEmpty());
        assertTrue(api("sk-ant-test", BigDecimal.ZERO).route("xiaomi/mimo-v2.6-pro").isEmpty());
    }

    @Test
    void withoutKeyEverythingStaysOnOpenRouter() {
        AnthropicApi api = api("", BigDecimal.ZERO);
        assertFalse(api.available());
        OpenRouterClient openRouter = mock(OpenRouterClient.class);
        ObjectNode body = mapper.createObjectNode().put("model", "anthropic/claude-haiku-5.5");
        JsonNode answer = mapper.createObjectNode().put("id", "or");
        when(openRouter.complete(body)).thenReturn(answer);
        when(openRouter.stream(body)).thenReturn(Flux.just(answer));
        assertSame(answer, api.complete(body, AnthropicApi.SOURCE_GRADING, openRouter));
        assertSame(answer, api.stream(body, AnthropicApi.SOURCE_CHAT, openRouter).blockFirst());
    }

    @Test
    void missingCostTableSendsEverythingToOpenRouter() {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(BigDecimal.class)))
                .thenThrow(new org.springframework.jdbc.BadSqlGrammarException("x", "SELECT", new java.sql.SQLException("no table")));
        assertFalse(new AnthropicApi(jdbc, "sk-ant-test", new BigDecimal("100"), new BigDecimal("2")).available());
    }

    @Test
    void creditAndKeyErrorsTurnAnthropicOffOtherErrorsOnlyAffectTheCall() {
        assertEquals("nema kredita", AnthropicApi.disablingReason(status(402)));
        assertEquals("ključ ne vrijedi", AnthropicApi.disablingReason(
                UnauthorizedException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build()));
        assertEquals("nema kredita", AnthropicApi.disablingReason(
                BadRequestException.builder().headers(Headers.builder().build())
                        .body(JsonValue.from("Your credit balance is too low to access the Anthropic API.")).build()));
        assertNull(AnthropicApi.disablingReason(
                RateLimitException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build()));

        AnthropicApi api = api("sk-ant-test", BigDecimal.ZERO);
        api.failed(RateLimitException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build());
        assertTrue(api.available());
        api.failed(status(402));
        assertFalse(api.available());
    }

    @Test
    void openRouterChatBodyBecomesAnthropicMessages() throws Exception {
        JsonNode body = mapper.readTree("""
                {"model": "anthropic/claude-haiku-5.5", "stream": true, "max_tokens": 20000,
                 "reasoning": {"exclude": true, "effort": "medium"},
                 "messages": [
                   {"role": "system", "content": [
                     {"type": "text", "text": "Upute", "cache_control": {"type": "ephemeral"}},
                     {"type": "text", "text": "Zadatak", "cache_control": {"type": "ephemeral"}}]},
                   {"role": "user", "content": "Prvo pitanje"},
                   {"role": "assistant", "content": [{"type": "text", "text": "Odgovor", "cache_control": {"type": "ephemeral"}}]},
                   {"role": "user", "content": [
                     {"type": "text", "text": "Drugo pitanje"},
                     {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,AAAA"}}]}]}""");
        MessageCreateParams params = api("sk-ant-test", BigDecimal.ZERO).params("claude-haiku-5-5", body);
        JsonNode sent = mapper.readTree(mapper.writeValueAsString(params._body()));

        assertEquals("claude-haiku-5-5", sent.path("model").asText());
        assertEquals(20000, sent.path("max_tokens").asInt());
        assertEquals("adaptive", sent.path("thinking").path("type").asText());
        assertEquals("medium", sent.path("output_config").path("effort").asText());
        assertEquals(2, sent.path("system").size());
        assertEquals("ephemeral", sent.path("system").path(1).path("cache_control").path("type").asText());
        assertEquals(3, sent.path("messages").size());
        assertEquals("assistant", sent.path("messages").path(1).path("role").asText());
        assertEquals("ephemeral", sent.path("messages").path(1).path("content").path(0).path("cache_control").path("type").asText());
        JsonNode image = sent.path("messages").path(2).path("content").path(1);
        assertEquals("base64", image.path("source").path("type").asText());
        assertEquals("image/jpeg", image.path("source").path("media_type").asText());
        assertEquals("AAAA", image.path("source").path("data").asText());
        assertFalse(sent.has("stream"));
    }

    @Test
    void gradingJsonSchemaBecomesStructuredOutput() throws Exception {
        JsonNode body = mapper.readTree("""
                {"model": "anthropic/claude-haiku-5.5", "max_tokens": 10000,
                 "messages": [{"role": "user", "content": [{"type": "text", "text": "Ocijeni"}]}],
                 "response_format": {"type": "json_schema", "json_schema": {"name": "grade", "strict": true,
                   "schema": {"type": "object", "properties": {"score": {"type": "integer"}},
                              "required": ["score"], "additionalProperties": false}}}}""");
        JsonNode sent = mapper.readTree(mapper.writeValueAsString(
                api("sk-ant-test", BigDecimal.ZERO).params("claude-haiku-5-5", body)._body()));
        JsonNode format = sent.path("output_config").path("format");
        assertEquals("json_schema", format.path("type").asText());
        assertEquals("integer", format.path("schema").path("properties").path("score").path("type").asText());
        assertFalse(format.path("schema").path("additionalProperties").asBoolean(true));
        assertFalse(sent.has("thinking"));
    }

    @Test
    void reasoningNoneTurnsThinkingOffWhereTheModelAllowsIt() {
        assertEquals(AnthropicApi.ThinkingMode.BETWEEN_TOOLS, AnthropicApi.thinking("claude-sonnet-5-5", "none").mode());
        assertEquals("low", AnthropicApi.thinking("claude-opus-5-5", "none").effort());
        assertEquals(AnthropicApi.ThinkingMode.DISABLED, AnthropicApi.thinking("claude-haiku-5-5", "none").mode());
        assertEquals("low", AnthropicApi.thinking("claude-haiku-5-5", "minimal").effort());
        assertEquals("xhigh", AnthropicApi.thinking("claude-sonnet-5-5", "xhigh").effort());
    }

    private static AnthropicServiceException status(int code) {
        return UnexpectedStatusCodeException.builder().statusCode(code).headers(Headers.builder().build())
                .body(JsonValue.from("x")).build();
    }
}
