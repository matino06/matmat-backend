package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskVariationStepTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final OwnCourseLlm llm = mock(OwnCourseLlm.class);
    private final VariationPrompt prompt = mock(VariationPrompt.class);
    private final VariationChecks checks = mock(VariationChecks.class);
    private final TooCloseCheck tooClose = mock(TooCloseCheck.class);
    private final TaskVariationStep step = new TaskVariationStep(jdbc, llm, new PlanSupport(jdbc, "sol", "haiku"), prompt, checks, tooClose,
            mock(OwnCourseMailer.class), "sol");
    private final BuildJob job = new BuildJob(1, 5, 7, TaskVariationStep.KIND, "v2 11", 0, false, 1, TaskVariationStep.input(11, 2));

    private void firstVariation(String answer) {
        when(jdbc.queryForObject(contains("SELECT count(*) FROM task"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForList(contains("ORDER BY variation"), any(SqlParameterSource.class))).thenReturn(List.of(
                Map.of("variation", 1, "task_text", "a) Auto 36 km/h …", "explanation", "x".repeat(1000), "results", "{\"a\":\"2 m/s²\"}")));
        when(llm.chat(any(), anyString(), any(), any(), any())).thenReturn(new OwnCourseLlm.Result(answer, BigDecimal.ZERO, 0));
        when(checks.problems(any())).thenReturn(List.of());
    }

    @Test
    void laterVariationIsWrittenByCheapModelFromTheFirstAsTemplate() throws Exception {
        firstVariation("{\"zadatak\":\"a) Bicikl 18 km/h …\",\"objasnjenje\":\"" + "y".repeat(950) + "\",\"rezultati\":{\"a\":\"1 m/s²\"}}");
        assertEquals("spremljeno", step.run(job));
        verify(llm).chat(eq(job), eq("haiku"), any(), any(), argThat(o -> "xhigh".equals(o.reasoning())));
        verify(prompt).templateUser(contains("Auto 36 km/h"), eq(List.of()));
        verify(jdbc).update(contains("INSERT INTO task"), argThat((SqlParameterSource p) -> Integer.valueOf(2).equals(p.getValue("v"))));
        verifyNoInteractions(tooClose); // provjera „preblizu“ samo za 1. varijaciju
    }

    @Test
    void muchShorterExplanationThanTemplateIsRejected() {
        firstVariation("{\"zadatak\":\"a) Bicikl\",\"objasnjenje\":\"" + "y".repeat(500) + "\",\"rezultati\":{\"a\":\"1\"}}");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> step.run(job));
        assertTrue(e.getMessage().contains("kraće od predloška"));
        verify(jdbc, never()).update(contains("INSERT INTO task"), any(SqlParameterSource.class));
    }
}
