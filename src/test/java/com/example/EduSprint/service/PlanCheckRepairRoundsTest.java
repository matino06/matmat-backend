package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Lekcija „Gustoća“ (1.) koristi pojam koji uči tek „Srednja gustoća“ (2.), a ta joj je preduvjet: premještanje
// bi stvorilo krug. Model je isti pojam prijavio dvaput, drugim riječima.
class PlanCheckRepairRoundsTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final PlanFlow flow = mock(PlanFlow.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final PlanCheckRepairStep step = new PlanCheckRepairStep(jdbc, mock(OwnCourseLlm.class), new PlanSupport(jdbc, "sol", "haiku"),
            tx, mock(OwnCourseMailer.class), flow);

    @SuppressWarnings("unchecked")
    private void plan() throws Exception {
        doAnswer(inv -> { ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(null); return null; }).when(tx).executeWithoutResult(any());
        when(jdbc.queryForMap(contains("FROM course WHERE"), any(SqlParameterSource.class)))
                .thenReturn(Map.of("course_name", "Fizika", "subject", "fizika", "level", "os78", "owner_account_id", 7L));
        when(jdbc.queryForList(contains("lo.objective_name FROM course_objective"), any(SqlParameterSource.class))).thenReturn(List.of(
                Map.of("objective_id", 10L, "objective_name", "Gustoća"), Map.of("objective_id", 20L, "objective_name", "Srednja gustoća")));
        doAnswer(inv -> { row(inv.getArgument(2), 20L, 10L); return null; })
                .when(jdbc).query(contains("FROM objective_prerequisite"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        doAnswer(inv -> { row(inv.getArgument(2), 100L, 10L); row(inv.getArgument(2), 200L, 20L); return null; })
                .when(jdbc).query(contains("FROM knowledge_item"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        when(jdbc.queryForList(contains("FROM zero_finding"), any(SqlParameterSource.class))).thenReturn(List.of(
                finding("homogeno tijelo/tvar"), finding("homogena tvar/tijelo")));
    }

    private static Map<String, Object> finding(String concept) {
        return new java.util.HashMap<>(Map.of("objective_id", 10L, "concept", concept, "taught_by", "[\"200\"]"));
    }

    private static void row(RowCallbackHandler h, long a, long b) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(a);
        when(rs.getLong(2)).thenReturn(b);
        h.processRow(rs);
    }

    private BuildJob job(String input) {
        return new BuildJob(1, 5, 7, PlanCheckRepairStep.KIND, "provjera", 0, true, 1, input);
    }

    @Test
    void sameConceptInOtherWordsHasTheSameKey() {
        assertEquals(PlanCheckRepairStep.conceptKey("homogeno tijelo/tvar"), PlanCheckRepairStep.conceptKey("homogena tvar/tijelo"));
        assertNotEquals(PlanCheckRepairStep.conceptKey("gustoća"), PlanCheckRepairStep.conceptKey("masa"));
    }

    @Test
    void unresolvedSeriousFindingGetsAnotherRoundBeforeTheCreatorSeesThePlan() throws Exception {
        plan();
        step.run(job(null));
        verify(flow).repairRound(5L, 7L, 1, List.of(10L));
        verify(jdbc, never()).update(contains("INSERT INTO plan_warning"), any(SqlParameterSource.class));
        verify(jdbc, never()).update(contains("UPDATE course SET status"), any(SqlParameterSource.class));
    }

    @Test
    void afterTheLastRoundWhatRemainsIsOneWarningPerLessonPair() throws Exception {
        plan();
        step.run(job("{\"repair\":" + PlanCheckRepairStep.REPAIR_ROUNDS + "}"));
        verify(flow, never()).repairRound(anyLong(), anyLong(), anyInt(), any());
        verify(jdbc, times(1)).update(contains("INSERT INTO plan_warning"), argThat((SqlParameterSource p) ->
                String.valueOf(p.getValue("t")).contains("pojam „homogeno tijelo/tvar“, koji se uči tek u lekciji „Srednja gustoća“")));
        verify(jdbc).update(contains("UPDATE course SET status"), any(SqlParameterSource.class));
    }
}
