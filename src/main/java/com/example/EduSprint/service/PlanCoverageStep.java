package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

// Pokrivenost jedne serije zadataka s ispita: koje lekcije učenik mora proći da ga riješi i nedostaje li
// nešto što nije ni u jednoj lekciji ni u predznanju. Popis lekcija sa stavkama i predznanje su u system poruci (keširaju se).
@Component
public class PlanCoverageStep implements BuildStep {

    public static final String KIND = "plan_coverage";

    static final String SYSTEM = """
            Za svaki zadatak s ispita iz %s navedi koje lekcije plana učenik mora proći da ga riješi, i je li za njega
            potrebno nešto što NIJE ni u jednoj lekciji ni u predznanju. Konstante i formule iz materijala dostupnog na ispitu
            su dostupne i ne računaju se kao nedostatak.
            Vrati JSON {"zadaci":[{"i":0,"lekcije":[id lekcije],"nedostaje":"" }]} (nedostaje prazno ako ništa).

            PREDZNANJE: %s

            %s

            LEKCIJE (id, naziv i što uči, ispod stavke znanja; lekcija pokriva sve što piše u njezinim stavkama):
            %s""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanCoverageStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.flow = flow;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) throws Exception {
        List<Long> taskIds = new ArrayList<>();
        mapper.readTree(job.input()).path("taskIds").forEach(n -> taskIds.add(n.asLong()));
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        // naziv i koncept ne kažu sve što lekcija uči (npr. vrijeme poluraspada u lekciji o građi atoma), pa idu i stavke
        List<String> lessons = jdbc.queryForList("""
                SELECT lo.objective_id || ' ' || lo.objective_name || COALESCE(': ' || lo.concept, '')
                    || COALESCE((SELECT string_agg(E'\\n  - ' || k.text, '' ORDER BY k.position) FROM knowledge_item k
                                 WHERE k.objective_id = lo.objective_id AND k.duplicate_of IS NULL), '')
                FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                WHERE co.course_id = :c ORDER BY co.position""", c, String.class);
        Set<Long> valid = new HashSet<>(jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c", c, Long.class));
        String system = SYSTEM.formatted(course.describe(), String.join("; ", plan.assumedKnowledge(course)),
                plan.availableOnExam(job.courseId()), String.join("\n", lessons));

        List<Map<String, Object>> tasks = jdbc.queryForList(
                "SELECT exam_task_id, number, summary, answer FROM exam_task WHERE exam_task_id IN (:t) ORDER BY exam_task_id",
                new MapSqlParameterSource("t", taskIds));
        StringBuilder user = new StringBuilder("ZADACI:\n");
        for (int i = 0; i < tasks.size(); i++) {
            Map<String, Object> t = tasks.get(i);
            user.append(i).append(". [").append(Objects.toString(t.get("number"), "")).append("] ").append(t.get("summary"))
                    .append(" (odgovor: ").append(Objects.toString(t.get("answer"), "")).append(")\n");
        }
        JsonNode out = plan.json(llm.chat(job, plan.cheapModel, system, user.toString(), OwnCourseLlm.Options.of(null, 8000)).content());
        Map<Integer, JsonNode> byIndex = new HashMap<>();
        for (JsonNode z : out.path("zadaci")) byIndex.put(z.path("i").asInt(-1), z);
        for (int i = 0; i < tasks.size(); i++) {
            JsonNode z = byIndex.get(i);
            List<Long> lessonIds = new ArrayList<>();
            String missing = z == null ? "model nije odgovorio" : z.path("nedostaje").asText("").strip();
            if (z != null) z.path("lekcije").forEach(n -> { if (valid.contains(n.asLong())) lessonIds.add(n.asLong()); });
            jdbc.update("UPDATE exam_task SET lessons = :l, missing = :m WHERE exam_task_id = :t",
                    new MapSqlParameterSource("t", tasks.get(i).get("exam_task_id"))
                            .addValue("l", plan.toJson(lessonIds)).addValue("m", missing.isEmpty() ? null : missing));
        }
        return tasks.size() + " zadataka";
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterChecks(job);
    }
}
