package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReportServiceTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final AiReportService service = new AiReportService(jdbc);

    @Test
    void reportOfOwnAnswerIsSavedAndCountsAsThumbsDown() {
        when(jdbc.queryForObject(contains("FROM ai_message"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);

        service.reportMessage(7, 42, "incorrect", "  Krivi predznak u drugom koraku. ", "ios");

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(contains("INSERT INTO ai_report"), params.capture());
        MapSqlParameterSource p = params.getValue();
        assertEquals(7L, p.getValue("accountId"));
        assertEquals(42L, p.getValue("messageId"));
        assertEquals("incorrect", p.getValue("reason"));
        assertEquals("Krivi predznak u drugom koraku.", p.getValue("note"));
        assertEquals("ios", p.getValue("source"));
        verify(jdbc).update(contains("SET rating = -1"), any(MapSqlParameterSource.class));
    }

    @Test
    void someoneElsesOrUserMessageIs404AndNothingIsSaved() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(0);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.reportMessage(7, 42, "inappropriate", null, null));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void unknownReasonIs400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.reportMessage(7, 42, "boring", null, "ios"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void gradedMockExamAnswerIsSavedByItsAnswerId() {
        when(jdbc.queryForList(contains("ai_grading_status = 'graded'"), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(900L));

        service.reportMockExamAnswer(7, 3, 11, "incorrect", "", "web");

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(contains("mock_exam_answer_id"), params.capture());
        assertEquals(900L, params.getValue().getValue("answerId"));
        assertNull(params.getValue().getValue("note"));
        assertEquals("web", params.getValue().getValue("source"));
    }

    @Test
    void mockExamAnswerNotGradedByAiOrNotOwnIs404() {
        when(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(List.of());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.reportMockExamAnswer(7, 3, 11, "incorrect", null, "ios"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void noteIsTrimmedAndCut() {
        assertNull(AiReportService.cleanNote("   "));
        assertEquals("x", AiReportService.cleanNote(" x\n"));
        assertEquals(AiReportService.MAX_NOTE_CHARS, AiReportService.cleanNote("a".repeat(5000)).length());
        assertTrue(AiReportService.REASONS.contains(AiReportService.validReason("inappropriate")));
    }
}
