package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LessonUiStepTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final OwnCourseLlm llm = mock(OwnCourseLlm.class);
    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final LessonUiStep step = new LessonUiStep(jdbc, llm, new PlanSupport(jdbc, "sol", "haiku"), new LessonUi(), queue, "sol");
    private final BuildJob job = new BuildJob(1, 5, 7, LessonUiStep.KIND, "ui1 11", 1, false, 1, LessonUiStep.input(11, 1));
    private final BuildJob job2 = new BuildJob(2, 5, 7, LessonUiStep.KIND, "ui2 11", 0, false, 1, LessonUiStep.input(11, 2));

    private void task(String lessonUi) {
        Map<String, Object> row = new HashMap<>(Map.of("task_id", 30L, "task_text", "a) Kamen …", "explanation", "…", "results", "{\"a\":\"80 cm³\"}",
                "objective_name", "Mjerenje volumena", "subject", "fizika", "level", "os78"));
        row.put("sketch", null);
        row.put("lesson_ui", lessonUi);
        when(jdbc.queryForList(contains("FROM task t JOIN learning_objective"), any(SqlParameterSource.class))).thenReturn(List.of(row));
    }

    // lekcija 1. varijacije (predložak za varijacije 2–4), ili null ako je nema
    private void firstLesson(String lesson) {
        when(jdbc.queryForList(contains("variation = 1 AND lesson_ui"), any(SqlParameterSource.class), eq(String.class)))
                .thenReturn(lesson == null ? List.of() : List.of(lesson));
    }

    private void answers(String... contents) {
        var stub = when(llm.chat(any(), anyString(), any(), any(), any()));
        for (String c : contents) stub = stub.thenReturn(new OwnCourseLlm.Result(c, BigDecimal.ZERO, 0));
    }

    @Test
    void onlyInteractiveCoursesQueueTheInteractiveVersion() {
        when(jdbc.queryForObject(contains("lesson_format"), any(SqlParameterSource.class), eq(String.class))).thenReturn("tekst");
        step.enqueueIfInteractive(job, 11, 1, false);
        verifyNoInteractions(queue);

        when(jdbc.queryForObject(contains("lesson_format"), any(SqlParameterSource.class), eq(String.class))).thenReturn("interaktivno");
        step.enqueueIfInteractive(job, 11, 1, false);
        step.enqueueIfInteractive(job, 11, 1, true);
        verify(queue).enqueue(eq(5L), eq(7L), eq(LessonUiStep.KIND), eq("ui1 11"), eq(BuildJobQueue.PRIORITY_BACKGROUND), eq(false), contains("\"objectiveId\":11"));
        verify(queue).enqueue(eq(5L), eq(7L), eq(LessonUiStep.KIND), eq("ui1 11 #1"), anyInt(), eq(false), anyString());
        // varijacije 2–4 nastaju dok netko predmet već uči, pa idu ispred izrade predmeta
        step.enqueueIfInteractive(job, 11, 2, false);
        verify(queue).enqueue(eq(5L), eq(7L), eq(LessonUiStep.KIND), eq("ui2 11"), eq(BuildJobQueue.PRIORITY_WAITED_ON), eq(false), contains("\"variation\":2"));
    }

    @Test
    void laterVariationFollowsTheFirstVariationsLessonWithTheCheapModel() throws Exception {
        task(null);
        firstLesson(LessonUiTest.VALID);
        answers(LessonUiTest.VALID);
        assertEquals("spremljeno po predlošku", step.run(job2));
        verify(llm).chat(eq(job2), eq("haiku"), contains("KATALOG WIDGETA"), contains("PREDLOŽAK"), any());
        verify(llm, never()).chat(any(), eq("sol"), any(), any(), any());
    }

    @Test
    void templateThatStaysInvalidFallsBackToTheWriter() throws Exception {
        task(null);
        firstLesson(LessonUiTest.VALID);
        answers("{\"verzija\":1}", "{\"verzija\":1}", LessonUiTest.VALID);
        assertEquals("spremljeno", step.run(job2));
        verify(llm).chat(eq(job2), eq("sol"), any(), argThat((String u) -> !u.contains("PREDLOŽAK")), any());
    }

    @Test
    void laterVariationWithoutAFirstLessonIsWrittenFromScratch() throws Exception {
        task(null);
        firstLesson(null);
        answers(LessonUiTest.VALID);
        assertEquals("spremljeno", step.run(job2));
        verify(llm).chat(eq(job2), eq("sol"), any(), any(), any());
    }

    @Test
    void validLessonIsSaved() throws Exception {
        task(null);
        answers("Evo lekcije:\n" + LessonUiTest.VALID);
        assertEquals("spremljeno", step.run(job));
        verify(llm).chat(eq(job), eq("sol"), contains("KATALOG WIDGETA"), contains("Mjerenje volumena"), argThat(o -> "medium".equals(o.reasoning())));
        verify(jdbc).update(contains("SET lesson_ui"), argThat((SqlParameterSource p) -> String.valueOf(p.getValue("u")).contains("\"verzija\":1")));
    }

    @Test
    void invalidLessonIsRepairedOnceByTheCheapModel() throws Exception {
        task(null);
        answers("{\"verzija\":1}", LessonUiTest.VALID);
        assertEquals("spremljeno nakon popravka", step.run(job));
        verify(llm).chat(eq(job), eq("haiku"), any(), contains("TVOJA LEKCIJA"), any());
    }

    @Test
    void lessonThatStaysInvalidLeavesTheTaskAsText() throws Exception {
        task(null);
        answers("nije json", "{\"verzija\":1}");
        assertTrue(step.run(job).startsWith("bez interaktivne verzije"));
        verify(jdbc, never()).update(contains("SET lesson_ui"), any(SqlParameterSource.class));
    }

    @Test
    void existingVersionIsKept() throws Exception {
        task("{\"verzija\":1}");
        assertEquals("već postoji", step.run(job));
        verifyNoInteractions(llm);
    }
}
