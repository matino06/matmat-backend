package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VariationBatchPollerTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final OwnCourseLlm llm = mock(OwnCourseLlm.class);
    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final PlanSupport plan = new PlanSupport(jdbc, "sol", "haiku");
    private final VariationBatchPoller poller = new VariationBatchPoller(jdbc, llm, queue, plan, true);
    private final ObjectMapper mapper = new ObjectMapper();

    private void batch(String json) throws Exception {
        when(llm.getBatch("b1")).thenReturn(mapper.readTree(json));
        when(jdbc.update(contains("status = 'obrada'"), any(SqlParameterSource.class))).thenReturn(1);
        Map<String, Object> row = new HashMap<>(Map.of("course_id", 5L, "account_id", 7L, "job_id", 9L, "objective_ids", "[11,12]"));
        when(jdbc.queryForMap(contains("FROM variation_batch"), any(SqlParameterSource.class))).thenReturn(row);
    }

    @Test
    void completedBatchQueuesDraftsAndFallsBackForMissingLessons() throws Exception {
        batch("""
                {"status":"completed","usage":{"cost":0.5},"results":[
                  {"custom_id":"o11","response":{"status_code":200,"body":{"choices":[{"message":{"content":"{\\"zadatak\\":\\"Z\\",\\"objasnjenje\\":\\"O\\"}"}}]}}},
                  {"custom_id":"o12","response":null,"error":{"message":"x"}}]}""");
        poller.check("b1");
        verify(queue).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), eq("v1 11"), anyInt(), eq(false), contains("\"draft\":{\"zadatak\":\"Z\""));
        verify(queue).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), eq("v1 12"), anyInt(), eq(false), argThat(in -> !in.contains("draft")));
        verify(queue).recordCost(any(), eq("batch"), argThat(c -> c.doubleValue() == 0.5));
    }

    @Test
    void unfinishedBatchIsLeftAlone() throws Exception {
        batch("{\"status\":\"in_progress\"}");
        poller.check("b1");
        verifyNoInteractions(queue);
    }

    @Test
    void expiredBatchSendsEveryLessonToRegularWriter() throws Exception {
        batch("{\"status\":\"expired\",\"results\":null}");
        poller.check("b1");
        verify(queue, times(2)).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), anyString(), anyInt(), eq(false), argThat(in -> !in.contains("draft")));
        verify(jdbc).update(contains("finished_at = now()"), argThat((SqlParameterSource p) -> "neuspio".equals(p.getValue("s"))));
    }
}
