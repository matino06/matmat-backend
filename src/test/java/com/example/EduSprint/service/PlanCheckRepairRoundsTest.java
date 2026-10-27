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
        row(h, a, b, null);
    }

    private static void row(RowCallbackHandler h, long a, long b, String prerequisites) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(a);
        when(rs.getLong(2)).thenReturn(b);
        when(rs.getString(3)).thenReturn(prerequisites);
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

    // Model je rekao koja stavka (100) koristi pojam: ona seli u „Srednju gustoću“, gdje se pojam uči, i ostaje cijela.
    @Test
    void itemThatNeedsALaterConceptMovesToTheLessonThatTeachesIt() throws Exception {
        plan();
        doAnswer(inv -> {
            for (long item : new long[]{100L, 101L, 102L}) row(inv.getArgument(2), item, 10L);
            row(inv.getArgument(2), 200L, 20L);
            return null;
        }).when(jdbc).query(contains("FROM knowledge_item"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        Map<String, Object> f = finding("homogeno tijelo");
        f.put("item_id", 100L);
        when(jdbc.queryForList(contains("FROM zero_finding"), any(SqlParameterSource.class))).thenReturn(List.of(f));

        String result = step.run(job(null));

        assertTrue(result.contains("1 stavki premješteno"), result);
        verify(jdbc).update(contains("UPDATE knowledge_item SET objective_id"), argThat((SqlParameterSource p) ->
                Long.valueOf(100L).equals(p.getValue("i")) && Long.valueOf(20L).equals(p.getValue("t"))));
        verify(jdbc, never()).update(contains("UPDATE knowledge_item SET text"), any(SqlParameterSource.class));
        verify(flow).repairRound(5L, 7L, 1, List.of(10L, 20L));
    }

    // Model je istoj stavci prijavio dva pojma iz „Srednje gustoće“: seli jednom i ne prepravlja se zbog drugog pojma.
    @Test
    void itemWithTwoLaterConceptsMovesOnceAndIsNotRewritten() throws Exception {
        plan();
        doAnswer(inv -> {
            for (long item : new long[]{100L, 101L, 102L}) row(inv.getArgument(2), item, 10L);
            row(inv.getArgument(2), 200L, 20L);
            return null;
        }).when(jdbc).query(contains("FROM knowledge_item"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        Map<String, Object> a = finding("homogeno tijelo");
        a.put("item_id", 100L);
        Map<String, Object> b = finding("srednja gustoća");
        b.put("item_id", 100L);
        b.put("finding_id", 2L);
        when(jdbc.queryForList(contains("FROM zero_finding"), any(SqlParameterSource.class))).thenReturn(List.of(a, b));

        String result = step.run(job(null));

        assertTrue(result.contains("1 stavki premješteno, 0 stavki prepravljeno"), result);
        verify(jdbc, times(1)).update(contains("UPDATE knowledge_item SET objective_id"), any(SqlParameterSource.class));
        verify(jdbc).update(contains("WHERE finding_id = :f"), argThat((SqlParameterSource p) -> Long.valueOf(2L).equals(p.getValue("f"))));
    }

    // Lekcija s dvije stavke ne daje nijednu: ostala bi gotovo prazna, pa se stavka prepravlja kao i prije.
    @Test
    void lessonWithTooFewItemsKeepsItsItem() throws Exception {
        plan();
        doAnswer(inv -> {
            for (long item : new long[]{100L, 101L}) row(inv.getArgument(2), item, 10L);
            row(inv.getArgument(2), 200L, 20L);
            return null;
        }).when(jdbc).query(contains("FROM knowledge_item"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        Map<String, Object> f = finding("homogeno tijelo");
        f.put("item_id", 100L);
        when(jdbc.queryForList(contains("FROM zero_finding"), any(SqlParameterSource.class))).thenReturn(List.of(f));

        step.run(job(null));

        verify(jdbc, never()).update(contains("UPDATE knowledge_item SET objective_id"), any(SqlParameterSource.class));
    }

    // Lekcija s manje od MIN_ITEMS stavki (npr. nakon premještanja) dobiva upozorenje, da je kreator spoji sa susjednom.
    @Test
    void tooSmallLessonIsWarnedAboutAfterTheLastRound() throws Exception {
        plan();
        step.run(job("{\"repair\":" + PlanCheckRepairStep.REPAIR_ROUNDS + "}"));
        verify(jdbc).update(contains("INSERT INTO plan_warning"), argThat((SqlParameterSource p) ->
                "velicina".equals(p.getValue("k")) && String.valueOf(p.getValue("t")).equals(
                        "„Gustoća“ ima samo 1 stavku znanja; lekcija bi trebala imati barem " + PlanRepair.MIN_ITEMS + ".")));
    }

    // Na stavci 100 gradi stavka 101 (preduvjet): premještanjem bi je odvukla za sobom, pa se 100 prepravlja.
    @Test
    void itemOtherItemsBuildOnIsRewrittenNotMoved() throws Exception {
        plan();
        doAnswer(inv -> {
            row(inv.getArgument(2), 100L, 10L);
            row(inv.getArgument(2), 101L, 10L, "[100]");
            row(inv.getArgument(2), 102L, 10L);
            row(inv.getArgument(2), 200L, 20L);
            return null;
        }).when(jdbc).query(contains("FROM knowledge_item"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        Map<String, Object> f = finding("homogeno tijelo");
        f.put("item_id", 100L);
        when(jdbc.queryForList(contains("FROM zero_finding"), any(SqlParameterSource.class))).thenReturn(List.of(f));

        String result = step.run(job(null));

        assertTrue(result.contains("0 stavki premješteno"), result);
        verify(jdbc, never()).update(contains("UPDATE knowledge_item SET objective_id"), any(SqlParameterSource.class));
    }

    // Lekcija „Srednja gustoća“ (20) koristi pojam iz stavke 100 lekcije „Gustoća“ (10): i ona je zato „na kojoj se
    // gradi“, pa ne seli ni kad sama koristi pojam iz kasnije lekcije.
    @Test
    void itemAnotherLessonUsesIsNotMoved() throws Exception {
        plan();
        doAnswer(inv -> {
            for (long item : new long[]{100L, 101L, 102L}) row(inv.getArgument(2), item, 10L);
            row(inv.getArgument(2), 200L, 20L);
            return null;
        }).when(jdbc).query(contains("FROM knowledge_item"), any(SqlParameterSource.class), any(RowCallbackHandler.class));
        Map<String, Object> f = finding("homogeno tijelo");
        f.put("item_id", 100L);
        Map<String, Object> other = new java.util.HashMap<>(Map.of("objective_id", 20L, "concept", "gustoća", "taught_by", "[\"100\"]"));
        other.put("finding_id", 2L);
        when(jdbc.queryForList(contains("FROM zero_finding"), any(SqlParameterSource.class))).thenReturn(List.of(f, other));

        step.run(job(null));

        verify(jdbc, never()).update(contains("UPDATE knowledge_item SET objective_id"), any(SqlParameterSource.class));
    }
}
