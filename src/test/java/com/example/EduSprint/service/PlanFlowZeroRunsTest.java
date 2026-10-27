package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Ponovna nulta pretpostavka promijenjene lekcije ide dvaput, kao pri izradi, jer popravak zbog pojma iz kasnije
// lekcije traži oba pokretanja; stari nalazi brišu se prije, da drugo pokretanje ne obriše nalaze prvoga.
class PlanFlowZeroRunsTest {

    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final PlanFlow flow = new PlanFlow(queue, jdbc);

    @Test
    void repairRoundRunsTheZeroCheckTwiceAfterClearingOldFindings() {
        flow.repairRound(5L, 7L, 1, List.of(10L));

        InOrder order = inOrder(jdbc, queue);
        order.verify(jdbc).update(contains("DELETE FROM zero_finding WHERE objective_id = :o"),
                argThat((SqlParameterSource p) -> Long.valueOf(10L).equals(p.getValue("o"))));
        order.verify(queue).enqueue(eq(5L), eq(7L), eq(PlanZeroStep.KIND), eq("nulta 10 popravak 1 1"), anyInt(), eq(true),
                eq("{\"objectiveId\":10,\"run\":1,\"repair\":1}"));
        order.verify(queue).enqueue(eq(5L), eq(7L), eq(PlanZeroStep.KIND), eq("nulta 10 popravak 1 2"), anyInt(), eq(true),
                eq("{\"objectiveId\":10,\"run\":2,\"repair\":1}"));
    }

    @Test
    void fixRunsTheZeroCheckTwiceForTheLesson() {
        flow.fix(5L, 7L, 3L, List.of(10L), true);

        verify(queue).enqueue(eq(5L), eq(7L), eq(PlanZeroStep.KIND), eq("nulta 10 r3 1"), anyInt(), eq(true),
                eq("{\"objectiveId\":10,\"run\":1,\"round\":3,\"fix\":[10]}"));
        verify(queue).enqueue(eq(5L), eq(7L), eq(PlanZeroStep.KIND), eq("nulta 10 r3 2"), anyInt(), eq(true),
                eq("{\"objectiveId\":10,\"run\":2,\"round\":3,\"fix\":[10]}"));
    }
}
