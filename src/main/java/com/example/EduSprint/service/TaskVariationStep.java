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
@Component
public class TaskVariationStep implements BuildStep {

    public static final String KIND = "task_variation";
    public static final String STATUS_CONFIRMED = "potvrden";
    public static final String STATUS_READY = "spreman";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final VariationPrompt prompt;
    private final VariationChecks checks;
    private final TooCloseCheck tooClose;
    private final OwnCourseMailer mailer;
    private final String writerModel;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaskVariationStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, VariationPrompt prompt,
                             VariationChecks checks, TooCloseCheck tooClose, OwnCourseMailer mailer,
                             @Value("${own-courses.models.writer:openai/gpt-6.1-sol}") String writerModel) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.prompt = prompt;
        this.checks = checks;
        this.tooClose = tooClose;
        this.mailer = mailer;
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
        Integer exists = jdbc.queryForObject("SELECT count(*) FROM task WHERE objective_id = :o AND variation = :v", o, Integer.class);
        if (exists != null && exists > 0) return "već postoji";

        JsonNode v = in.has("draft") && job.attempts() <= 1
                ? in.get("draft")
                : plan.json(llm.chat(job, writerModel, prompt.system(job.courseId()), prompt.user(objectiveId), options()).content());
        List<String> problems = checks.problems(v);
        if (!problems.isEmpty()) throw new IllegalStateException("Provjere: " + String.join(" | ", problems));
        String close = tooClose.tooCloseTo(job, v.path("zadatak").asText());
        if (close != null) throw new IllegalStateException("Preblizu zadatku s ispita: " + close);

        jdbc.update("""
                INSERT INTO task (objective_id, task_text, explanation, variation, results) VALUES (:o, :t, :e, :v, :r)""",
                o.addValue("t", v.path("zadatak").asText()).addValue("e", v.path("objasnjenje").asText())
                        .addValue("r", mapper.writeValueAsString(v.path("rezultati"))));
        return "spremljeno";
    }

    // Kad sve lekcije imaju 1. varijaciju, predmet je spreman; ako je izrada išla preko batcha, kreator dobije e-mail.
    @Override
    public void afterComplete(BuildJob job) {
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
}
