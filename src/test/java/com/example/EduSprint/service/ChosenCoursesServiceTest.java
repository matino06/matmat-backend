package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChosenCoursesServiceTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ChosenCoursesService service = new ChosenCoursesService(jdbc);

    private void officialCount(int count) {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(count);
    }

    @Test
    void savesTheChoiceInOrder() {
        officialCount(2);

        assertEquals(List.of(4L, 1L), service.set(7L, List.of(4L, 1L)));

        verify(jdbc).update(anyString(), argThat((MapSqlParameterSource p) ->
                Arrays.equals((Long[]) p.getValue("ids"), new Long[]{4L, 1L}) && p.getValue("id").equals(7L)));
    }

    @Test
    void anEmptyChoiceIsRefused() {
        assertBadRequest(List.of());
        assertBadRequest(null);
    }

    @Test
    void aRepeatedOrMissingCourseIsRefused() {
        assertBadRequest(List.of(1L, 1L));
        assertBadRequest(Arrays.asList(1L, null));
    }

    @Test
    void anOwnOrUnknownCourseIsRefused() {
        officialCount(1);
        assertBadRequest(List.of(1L, 99L));
    }

    private void assertBadRequest(List<Long> ids) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.set(7L, ids));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }
}
