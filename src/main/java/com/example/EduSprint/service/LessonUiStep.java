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
// Varijacije 2–4 nastaju po predlošku 1. varijacije, pa i njihova lekcija: jeftiniji model prenosi lekciju 1. varijacije
// na novi zadatak (oko 0,01 $ umjesto 0,04 $), a tek ako ni nakon popravka ne prođe, lekciju piše model za pisanje.
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
    // Varijacije 2–4 nastaju dok predmet netko već uči (nakon riješenog zadatka), pa idu ispred izrade predmeta.
    public void enqueueIfInteractive(BuildJob job, long objectiveId, int variation, boolean replaced) {
        String format = jdbc.queryForObject("SELECT lesson_format FROM course WHERE course_id = :c",
                new MapSqlParameterSource("c", job.courseId()), String.class);
        if (!FORMAT_INTERACTIVE.equals(format)) return;
        String ref = "ui" + variation + " " + objectiveId + (replaced ? " #" + job.jobId() : "");
        int priority = variation > 1 ? BuildJobQueue.PRIORITY_WAITED_ON : BuildJobQueue.PRIORITY_BACKGROUND;
        queue.enqueue(job.courseId(), job.accountId(), KIND, ref, priority, false, input(objectiveId, variation));
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

        String template = in.path("variation").asInt(1) > 1 ? firstVariationLesson(p) : null;
        Attempt a = null;
        if (template != null) a = attempt(job, plan.cheapModel, system, user + "\n\n" + lessonUi.template(template), user, results, sketch);
        String via = a != null && a.check.ok() ? " po predlošku" : "";
        if (a == null || !a.check.ok()) a = attempt(job, model, system, user, user, results, sketch);
        if (!a.check.ok()) return "bez interaktivne verzije: " + String.join(" | ", a.check.errors().subList(0, Math.min(3, a.check.errors().size())));

        jdbc.update("UPDATE task SET lesson_ui = :u WHERE task_id = :t AND lesson_ui IS NULL",
                new MapSqlParameterSource("u", a.json).addValue("t", task.get("task_id")));
        return "spremljeno" + via + (a.repaired ? " nakon popravka" : "") + (a.check.warnings().isEmpty() ? "" : ", " + a.check.warnings().size() + " upozorenja");
    }

    private record Attempt(String json, LessonUi.Check check, boolean repaired) {
    }

    // Lekcija od modela pa, ako ne prođe provjeru, jedan popravak jeftinijim modelom u jednom pozivu: izvor, modelova
    // lekcija i nalaz provjere (bez ponovnog razmišljanja ispočetka).
    private Attempt attempt(BuildJob job, String writer, String system, String prompt, String user, String results, String sketch) throws Exception {
        String json = lesson(llm.chat(job, writer, system, prompt, OwnCourseLlm.Options.of("medium", 32000)).content());
        LessonUi.Check check = check(json, results, sketch);
        if (check.ok()) return new Attempt(json, check, false);
        String fix = user + "\n\nTVOJA LEKCIJA (JSON):\n" + (json == null ? "(nije JSON)" : json) + "\n\n" + lessonUi.repair(check.errors());
        json = lesson(llm.chat(job, plan.cheapModel, system, fix, OwnCourseLlm.Options.of("medium", 32000)).content());
        return new Attempt(json, check(json, results, sketch), true);
    }

    private LessonUi.Check check(String json, String results, String sketch) {
        return json == null ? new LessonUi.Check(List.of("model nije vratio JSON"), List.of()) : lessonUi.check(json, results, sketch);
    }

    // lekcija 1. varijacije iste lekcije, ili null ako je nema (izrada nije uspjela ili još traje)
    private String firstVariationLesson(MapSqlParameterSource p) {
        List<String> rows = jdbc.queryForList("SELECT lesson_ui FROM task WHERE objective_id = :o AND variation = 1 AND lesson_ui IS NOT NULL", p, String.class);
        return rows.isEmpty() ? null : rows.get(0);
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
