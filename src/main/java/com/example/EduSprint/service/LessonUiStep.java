package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Interaktivna verzija varijacije zadatka (predmet s oblikom lekcija „interaktivno“): iz već spremljenog i
// provjerenog teksta model za pisanje (kao za zadatke) napiše JSON lekciju od widgeta, a LessonUi je provjerava
// istim kodom kao frontend; ako ne prođe, jeftiniji model je jednom popravlja.
// Ne prođe li ni nakon jednog popravka, zadatak ostaje samo tekstualan: posao završava bez greške, jer tekst i
// dalje vrijedi, a ponavljanje posla samo bi trošilo. Ne troši kvotu kreatora, kao ni izrada zadataka.
@Component
public class LessonUiStep implements BuildStep {

    public static final String KIND = "lesson_ui";
    public static final String FORMAT_TEXT = "tekst";
    public static final String FORMAT_INTERACTIVE = "interaktivno";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final LessonUi lessonUi;
    private final BuildJobQueue queue;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    public LessonUiStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, LessonUi lessonUi, BuildJobQueue queue,
                        @Value("${own-courses.models.writer:openai/gpt-6.1-sol}") String model) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.lessonUi = lessonUi;
        this.queue = queue;
        this.model = model;
    }

    @Override
    public String kind() {
        return KIND;
    }

    public static String input(long objectiveId, int variation) {
        return "{\"objectiveId\":" + objectiveId + ",\"variation\":" + variation + "}";
    }

    // Poziva TaskVariationStep kad je varijacija spremljena: interaktivna verzija samo za predmete koji je imaju.
    // Red ne dodaje isti (predmet, vrsta, ref) dvaput, pa ponovno izrađen tekst (replaced) dobiva novu oznaku posla.
    public void enqueueIfInteractive(BuildJob job, long objectiveId, int variation, boolean replaced) {
        String format = jdbc.queryForObject("SELECT lesson_format FROM course WHERE course_id = :c",
                new MapSqlParameterSource("c", job.courseId()), String.class);
        if (!FORMAT_INTERACTIVE.equals(format)) return;
        String ref = "ui" + variation + " " + objectiveId + (replaced ? " #" + job.jobId() : "");
        queue.enqueue(job.courseId(), job.accountId(), KIND, ref, BuildJobQueue.PRIORITY_BACKGROUND, false, input(objectiveId, variation));
    }

    @Override
    public String run(BuildJob job) throws Exception {
        JsonNode in = mapper.readTree(job.input());
        MapSqlParameterSource p = new MapSqlParameterSource("o", in.path("objectiveId").asLong())
                .addValue("v", in.path("variation").asInt(1)).addValue("c", job.courseId());
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT t.task_id, t.task_text, t.explanation, t.results, t.sketch, t.lesson_ui, lo.objective_name, c.subject, c.level
                FROM task t JOIN learning_objective lo ON lo.objective_id = t.objective_id JOIN course c ON c.course_id = :c
                WHERE t.objective_id = :o AND t.variation = :v""", p);
        if (rows.isEmpty()) return "nema zadatka";
        Map<String, Object> task = rows.get(0);
        if (task.get("lesson_ui") != null) return "već postoji";

        Map<String, Object> source = new LinkedHashMap<>();
        for (String k : List.of("subject", "level", "objective_name", "task_text", "sketch", "results", "explanation")) source.put(k, task.get(k));
        String results = (String) task.get("results");
        String sketch = (String) task.get("sketch");
        String system = lessonUi.system();
        String user = lessonUi.user(source);

        String json = lesson(llm.chat(job, model, system, user, OwnCourseLlm.Options.of("medium", 32000)).content());
        LessonUi.Check check = json == null ? new LessonUi.Check(List.of("model nije vratio JSON"), List.of()) : lessonUi.check(json, results, sketch);
        boolean repaired = false;
        if (!check.ok()) {
            // popravak u jednom pozivu: izvor, modelova lekcija i nalaz provjere (bez ponovnog razmišljanja ispočetka)
            repaired = true;
            String fix = user + "\n\nTVOJA LEKCIJA (JSON):\n" + (json == null ? "(nije JSON)" : json) + "\n\n" + lessonUi.repair(check.errors());
            json = lesson(llm.chat(job, plan.cheapModel, system, fix, OwnCourseLlm.Options.of("medium", 32000)).content());
            check = json == null ? new LessonUi.Check(List.of("model nije vratio JSON"), List.of()) : lessonUi.check(json, results, sketch);
        }
        if (!check.ok()) return "bez interaktivne verzije: " + String.join(" | ", check.errors().subList(0, Math.min(3, check.errors().size())));

        jdbc.update("UPDATE task SET lesson_ui = :u WHERE task_id = :t AND lesson_ui IS NULL",
                new MapSqlParameterSource("u", json).addValue("t", task.get("task_id")));
        return "spremljeno" + (repaired ? " nakon popravka" : "") + (check.warnings().isEmpty() ? "" : ", " + check.warnings().size() + " upozorenja");
    }

    // JSON lekcije u jednom retku, ili null ako ga u odgovoru nema
    private String lesson(String content) {
        try {
            return mapper.writeValueAsString(plan.json(content));
        } catch (Exception e) {
            return null;
        }
    }
}
