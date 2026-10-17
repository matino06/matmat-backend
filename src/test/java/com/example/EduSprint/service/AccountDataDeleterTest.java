package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountDataDeleterTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final OwnCourseService ownCourses = mock(OwnCourseService.class);
    private final AccountDataDeleter deleter = new AccountDataDeleter(jdbc, ownCourses);

    @Test
    void everyTableOfTheAccountIsClearedOwnCoursesFirstAndTheAccountLast() {
        when(jdbc.queryForList(contains("FROM mock_exam_attempt"), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(31L));

        assertEquals(List.of(31L), deleter.deleteRows(7));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        InOrder order = inOrder(ownCourses, jdbc);
        order.verify(ownCourses).deleteRowsOfAllOwnedBy(7);
        order.verify(jdbc, atLeastOnce()).update(sql.capture(), params.capture());
        List<String> statements = sql.getAllValues().stream().map(s -> s.replaceAll("\\s+", " ").trim()).toList();

        for (String table : List.of("ai_message", "ai_conversation", "mock_exam_criterion_score", "mock_exam_answer",
                "mock_exam_attempt", "solved_task", "account_objective", "weak_objectives", "user_course_goal",
                "user_daily_tasks", "email_log", "variation_batch", "build_job", "build_cost")) {
            assertTrue(statements.stream().anyMatch(s -> s.startsWith("DELETE FROM " + table + " ")), table);
        }
        assertTrue(statements.contains("UPDATE ai_settings SET updated_by = NULL WHERE updated_by = :a"));
        assertEquals("DELETE FROM account WHERE account_id = :a", statements.get(statements.size() - 1));
        // Children before their parents.
        assertTrue(statements.indexOf(first(statements, "DELETE FROM ai_message")) < statements.indexOf(first(statements, "DELETE FROM ai_conversation")));
        assertTrue(statements.indexOf(first(statements, "DELETE FROM mock_exam_criterion_score")) < statements.indexOf(first(statements, "DELETE FROM mock_exam_answer")));
        assertTrue(statements.indexOf(first(statements, "DELETE FROM mock_exam_answer")) < statements.indexOf(first(statements, "DELETE FROM mock_exam_attempt")));
        params.getAllValues().forEach(p -> assertEquals(7L, p.getValue("a")));
    }

    private static String first(List<String> statements, String prefix) {
        return statements.stream().filter(s -> s.startsWith(prefix)).findFirst().orElseThrow();
    }
}
