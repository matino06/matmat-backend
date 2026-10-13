package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminOwnCourseServiceTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final AdminOwnCourseService service = new AdminOwnCourseService(jdbc);

    private static HttpStatus status(Runnable r) {
        return HttpStatus.valueOf(assertThrows(ResponseStatusException.class, r::run).getStatusCode().value());
    }

    @Test
    void quotaMustBeSensibleAndAccountMustExist() {
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.updateCreator("k@x.hr", true, new BigDecimal("-1"))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.updateCreator(" ", true, null)));
        when(jdbc.update(contains("UPDATE account"), any(SqlParameterSource.class))).thenReturn(0);
        assertEquals(HttpStatus.NOT_FOUND, status(() -> service.updateCreator("nema@x.hr", true, null)));
    }

    @Test
    void omittedFieldsStayAsTheyAre() {
        when(jdbc.update(contains("UPDATE account"), any(SqlParameterSource.class))).thenReturn(1);
        service.updateCreator(" K@x.hr ", null, new BigDecimal("25"));
        verify(jdbc).update(contains("COALESCE(:c, is_creator)"), argThat((SqlParameterSource p) ->
                p.getValue("c") == null && new BigDecimal("25").equals(p.getValue("q")) && "K@x.hr".equals(p.getValue("e"))));
    }
}
