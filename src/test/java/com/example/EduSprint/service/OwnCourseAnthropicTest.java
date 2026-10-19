package com.example.EduSprint.service;

import com.anthropic.core.http.Headers;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.errors.UnexpectedStatusCodeException;
import com.anthropic.core.JsonValue;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnCourseAnthropicTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    private OwnCourseAnthropic anthropic(String key, BigDecimal spent) {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(BigDecimal.class))).thenReturn(spent);
        return new OwnCourseAnthropic(jdbc, key, "claude-sonnet-5-5", "high", "openai/gpt-6.1-sol",
                new BigDecimal("100"), new BigDecimal("2"));
    }

    @Test
    void costCountsCacheWritesAndReadsAtTheirOwnPrice() {
        // 1M ulaza 2 $, 1M izlaza 10 $, 1M zapisa u cache 2,5 $, 1M čitanja iz cachea 0,2 $
        assertEquals(new BigDecimal("14.700000"), OwnCourseAnthropic.cost(1_000_000, 1_000_000, 1_000_000, 1_000_000));
        assertEquals(new BigDecimal("0.010000"), OwnCourseAnthropic.cost(0, 1_000, 0, 0));
    }

    @Test
    void solIsReplacedOnlyWhileCreditIsAboveTheMinimum() {
        assertTrue(anthropic("sk-ant-test", new BigDecimal("97.99")).replaces("openai/gpt-6.1-sol"));
        assertFalse(anthropic("sk-ant-test", new BigDecimal("98.00")).replaces("openai/gpt-6.1-sol"));
        assertFalse(anthropic("sk-ant-test", BigDecimal.ZERO).replaces("anthropic/claude-haiku-5.5"));
    }

    @Test
    void withoutKeyEverythingStaysOnOpenRouter() {
        assertFalse(anthropic("", BigDecimal.ZERO).available());
    }

    @Test
    void creditAndKeyErrorsTurnAnthropicOffOtherErrorsOnlyAffectTheCall() {
        assertEquals("nema kredita", OwnCourseAnthropic.disablingReason(status(402)));
        assertEquals("ključ ne vrijedi", OwnCourseAnthropic.disablingReason(
                UnauthorizedException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build()));
        assertEquals("nema kredita", OwnCourseAnthropic.disablingReason(
                BadRequestException.builder().headers(Headers.builder().build())
                        .body(JsonValue.from("Your credit balance is too low to access the Anthropic API.")).build()));
        assertNull(OwnCourseAnthropic.disablingReason(
                RateLimitException.builder().headers(Headers.builder().build()).body(JsonValue.from("x")).build()));
    }

    private static AnthropicServiceException status(int code) {
        return UnexpectedStatusCodeException.builder().statusCode(code).headers(Headers.builder().build())
                .body(JsonValue.from("x")).build();
    }
}
