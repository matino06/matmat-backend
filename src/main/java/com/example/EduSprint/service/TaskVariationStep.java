package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// 1. varijacija zadatka lekcije (Sol medium). Ne prođu li provjere kodom ili je zadatak preblizu zadatku
// s ispita, korak pada i red ga ponavlja (do 3 puta), pa nastaje nova varijacija.
// Nacrt iz batcha (input.draft) koristi se samo u prvom pokušaju; ponovni pokušaji pišu zadatak iznova.
// Varijacije 2–4 piše jeftiniji model po predlošku 1. varijacije, kad učenik riješi prethodnu.
// Skica: model uz zadatak može dati JSON opis skice; provjerava ga isti kod kojim ga frontend crta (SketchChecker).
// Ako ne prođe, model ga jednom pokuša ispraviti; ako ni tada ne prođe, zadatak se sprema bez skice.
@Component
public class TaskVariationStep implements BuildStep {

    public static final String KIND = "task_variation";
    public static final String STATUS_CONFIRMED = "potvrden";
    public static final String STATUS_READY = "spreman";
    public static final int MAX_VARIATIONS = 4;
    static final double MIN_TEMPLATE_LENGTH = 0.8;

    static final String SKETCH_FIX = """
            Popravljaš opis skice uz zadatak. Aplikacija iz opisa sama crta skicu i provjerava ga, a provjera je našla probleme.
            Ispravi opis tako da odgovara zadatku i rješenju: isti tip skice i ista JSON građa, brojevi kao u zadatku i rješenju.
            Ako skica zadatku ne treba ili se ne može ispraviti, vrati null. Vrati JSON {"skica":opis ili null}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final VariationPrompt prompt;
    private final VariationChecks checks;
    private final TooCloseCheck tooClose;
    private final SketchChecker sketches;
    private final ExplanationSketches explanationSketches;
    private final OwnCourseMailer mailer;
    private final LessonUiStep lessonUi;
    private final String writerModel;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaskVariationStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, VariationPrompt prompt,
                             VariationChecks checks, TooCloseCheck tooClose, SketchChecker sketches,
                             ExplanationSketches explanationSketches, OwnCourseMailer mailer, LessonUiStep lessonUi,
                             @Value("${own-courses.models.writer:openai/gpt-6.1-sol}") String writerModel) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.prompt = prompt;
        this.checks = checks;
        this.tooClose = tooClose;
        this.sketches = sketches;
        this.explanationSketches = explanationSketches;
        this.mailer = mailer;
        this.lessonUi = lessonUi;
        this.writerModel = writerModel;
    }

    @Override
    public String kind() {
        return KIND;
    }

    public static String input(long objectiveId, int variation) {
        return "{\"objectiveId\":" + objectiveId + ",\"variation\":" + variation + "}";
    }

    public static OwnCourseLlm.Options options() {
        return OwnCourseLlm.Options.of("medium", 100000);
    }

    String model() {
        return writerModel;
    }

    @Override
    public String run(BuildJob job) throws Exception {
        JsonNode in = mapper.readTree(job.input());
        long objectiveId = in.path("objectiveId").asLong();
        int variation = in.path("variation").asInt(1);
        MapSqlParameterSource o = new MapSqlParameterSource("o", objectiveId).addValue("v", variation);
        boolean replace = in.path("replace").asBoolean(false); // ponovna izrada postojeće varijacije
        Integer exists = jdbc.queryForObject("SELECT count(*) FROM task WHERE objective_id = :o AND variation = :v", o, Integer.class);
        if (!replace && exists != null && exists > 0) return "već postoji";

        JsonNode v;
        List<String> problems;
        if (variation == 1) {
            v = in.has("draft") && job.attempts() <= 1
                    ? in.get("draft")
                    : plan.json(llm.chat(job, writerModel, prompt.system(job.courseId()), prompt.user(job.courseId(), objectiveId), options()).content());
            problems = checks.problems(v);
        } else {
            List<java.util.Map<String, Object>> existing = jdbc.queryForList("""
                    SELECT variation, task_text, explanation, results, sketch FROM task WHERE objective_id = :o AND variation IS NOT NULL
                    ORDER BY variation""", o);
            if (existing.isEmpty() || ((Number) existing.get(0).get("variation")).intValue() != 1) {
                throw new IllegalStateException("Lekcija nema 1. varijaciju");
            }
            java.util.Map<String, Object> first = existing.get(0);
            java.util.Map<String, Object> tpl = new java.util.LinkedHashMap<>();
            tpl.put("zadatak", first.get("task_text"));
            tpl.put("objasnjenje", first.get("explanation"));
            tpl.put("rezultati", mapper.readTree(String.valueOf(first.get("results"))));
            tpl.put("skica", first.get("sketch") == null ? null : mapper.readTree(String.valueOf(first.get("sketch"))));
            String template = mapper.writeValueAsString(tpl);
            List<String> previous = existing.stream().skip(1).map(t -> (String) t.get("task_text")).toList();
            v = plan.json(llm.chat(job, plan.cheapModel, prompt.templateSystem(job.courseId()), prompt.templateUser(template, previous),
                    OwnCourseLlm.Options.of("xhigh", 32000)).content());
            problems = new java.util.ArrayList<>(checks.problems(v));
            double ratio = (double) v.path("objasnjenje").asText("").length() / Math.max(1, ((String) first.get("explanation")).length());
            if (ratio < MIN_TEMPLATE_LENGTH) problems.add("Objašnjenje je kraće od predloška (" + Math.round(ratio * 100) + " %).");
        }
        if (!problems.isEmpty()) throw new IllegalStateException("Provjere: " + String.join(" | ", problems));
        if (variation == 1) {
            String close = tooClose.tooCloseTo(job, v.path("zadatak").asText());
            if (close != null) throw new IllegalStateException("Preblizu zadatku s ispita: " + close);
        }

        String sketch = sketch(job, v);
        ExplanationSketches.Result explanation = explanationSketches.validate(v.path("objasnjenje").asText());
        o.addValue("t", v.path("zadatak").asText()).addValue("e", explanation.explanation())
                .addValue("r", mapper.writeValueAsString(v.path("rezultati"))).addValue("s", sketch);
        String withSketch = (sketch == null ? "" : " sa skicom") + explanation.summary();
        if (replace && exists != null && exists > 0) {
            // isti zadatak (task_id) ostaje, pa riješeni zadaci i ponavljanje ostaju vezani uz njega; stara interaktivna
            // verzija ne odgovara novom tekstu, pa se briše i (za interaktivne predmete) izrađuje iznova
            jdbc.update("UPDATE task SET task_text = :t, explanation = :e, results = :r, sketch = :s, lesson_ui = NULL WHERE objective_id = :o AND variation = :v", o);
            return "zamijenjeno" + withSketch;
        }
        jdbc.update("INSERT INTO task (objective_id, task_text, explanation, variation, results, sketch) VALUES (:o, :t, :e, :v, :r, :s)", o);
        return "spremljeno" + withSketch;
    }

    // Opis skice koji se može nacrtati (JSON), ili null: bez skice, ili ni nakon jednog ispravka ne prolazi provjeru.
    String sketch(BuildJob job, JsonNode v) throws Exception {
        JsonNode spec = v.path("skica");
        if (!spec.isObject()) return null;
        String text = v.path("zadatak").asText("") + "\n" + ExplanationSketches.strip(v.path("objasnjenje").asText("")) + "\n" + v.path("rezultati");
        String json = mapper.writeValueAsString(spec);
        List<String> problems = sketches.problems(json, text);
        if (problems.isEmpty()) return json;
        try {
            JsonNode fixed = plan.json(llm.chat(job, plan.cheapModel, SKETCH_FIX,
                    "ZADATAK I RJEŠENJE:\n" + text + "\n\nOPIS SKICE:\n" + json + "\n\nPROBLEMI:\n- " + String.join("\n- ", problems),
                    OwnCourseLlm.Options.of("low", 4000)).content()).path("skica");
            if (!fixed.isObject()) return null;
            String fixedJson = mapper.writeValueAsString(fixed);
            return sketches.problems(fixedJson, text).isEmpty() ? fixedJson : null;
        } catch (OwnCourseLlm.GlobalLimitException e) {
            throw e;
        } catch (RuntimeException e) {
            return null; // zadatak ide bez skice
        }
    }

    // Kad sve lekcije imaju 1. varijaciju, predmet je spreman; ako je izrada išla preko batcha, kreator dobije e-mail.
    // Interaktivni predmet uz 1. varijaciju dobiva i njezinu interaktivnu verziju (ne čeka je: tekst je dovoljan za učenje).
    @Override
    public void afterComplete(BuildJob job) {
        interactiveVersion(job);
        if (job.ref() != null && !job.ref().startsWith("v1 ")) return;
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        Integer missing = jdbc.queryForObject("""
                SELECT count(*) FROM course_objective co
                WHERE co.course_id = :c AND NOT EXISTS (SELECT 1 FROM task t WHERE t.objective_id = co.objective_id AND t.variation = 1)""",
                c, Integer.class);
        if (missing == null || missing > 0) return;
        int updated = jdbc.update("UPDATE course SET status = :r WHERE course_id = :c AND status = :p",
                c.addValue("r", STATUS_READY).addValue("p", STATUS_CONFIRMED));
        Integer batches = jdbc.queryForObject("SELECT count(*) FROM variation_batch WHERE course_id = :c", c, Integer.class);
        if (updated == 1 && batches != null && batches > 0) mailer.tasksReady(job.courseId());
    }

    private void interactiveVersion(BuildJob job) {
        try {
            JsonNode in = mapper.readTree(job.input());
            int variation = in.path("variation").asInt(1);
            if (variation == 1) lessonUi.enqueueIfInteractive(job, in.path("objectiveId").asLong(), variation, in.path("replace").asBoolean(false));
        } catch (Exception e) {
            // bez interaktivne verzije zadatak i dalje radi kao tekst
        }
    }
}
