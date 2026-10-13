package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OwnCourseVariationServiceTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final OwnCourseVariationService service = new OwnCourseVariationService(jdbc, queue, mock(TaskService.class));
    private final Account student = account();

    private static Account account() {
        Account a = new Account("u@x.hr", "U", null);
        ReflectionTestUtils.setField(a, "accountId", 7L);
        return a;
    }

    private void ownTask(int variation) {
        when(jdbc.queryForList(contains("owner_account_id IS NOT NULL"), any(SqlParameterSource.class)))
                .thenReturn(List.of(Map.of("course_id", 5L, "owner_account_id", 7L, "objective_id", 11L, "variation", variation)));
    }

    private void counts(int maxVariation, int solvedThis, int unsolved, String jobStatus) {
        when(jdbc.queryForObject(contains("max(variation)"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(maxVariation);
        when(jdbc.queryForObject(contains("task_id = :t"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(solvedThis);
        when(jdbc.queryForObject(contains("NOT EXISTS"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(unsolved);
        when(jdbc.queryForObject(contains("FROM build_job"), any(SqlParameterSource.class), eq(String.class))).thenReturn(jobStatus);
    }

    @Test
    void officialTasksAreIgnored() {
        when(jdbc.queryForList(contains("owner_account_id IS NOT NULL"), any(SqlParameterSource.class))).thenReturn(List.of());
        service.onSolved(3);
        assertEquals(OwnCourseVariationService.NONE, service.nextVariation(student, 3));
        verifyNoInteractions(queue);
    }

    @Test
    void solvingQueuesTheNextMissingVariationWithoutQuota() {
        ownTask(1);
        counts(1, 1, 0, BuildJobQueue.WAITING);
        service.onSolved(3);
        verify(queue).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), eq("v2 11"), eq(BuildJobQueue.PRIORITY_WAITED_ON), eq(false), contains("\"variation\":2"));
    }

    @Test
    void noFifthVariation() {
        ownTask(4);
        counts(4, 1, 0, null);
        service.onSolved(3);
        assertEquals(OwnCourseVariationService.NONE, service.nextVariation(student, 3));
        verifyNoInteractions(queue);
    }

    @Test
    void repeatedTaskWaitsForNewVariationButNewTaskDoesNot() {
        ownTask(1);
        counts(1, 0, 0, BuildJobQueue.RUNNING);
        assertEquals(OwnCourseVariationService.NONE, service.nextVariation(student, 3));
        counts(1, 1, 0, BuildJobQueue.RUNNING);
        assertEquals(OwnCourseVariationService.PENDING, service.nextVariation(student, 3));
        counts(2, 1, 1, BuildJobQueue.DONE);
        assertEquals(OwnCourseVariationService.READY, service.nextVariation(student, 3));
    }

    @Test
    void failedGenerationIsRestartedAndStudentGetsThePreviousVariation() {
        ownTask(1);
        counts(1, 1, 0, BuildJobQueue.FAILED);
        assertEquals(OwnCourseVariationService.FAILED, service.nextVariation(student, 3));
        verify(jdbc).update(contains("SET status = 'waiting', attempts = 0"), any(SqlParameterSource.class));
    }
}
