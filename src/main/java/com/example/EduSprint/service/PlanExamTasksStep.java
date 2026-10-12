package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Zadaci s jednog starog ispita iz materijala (zadaci + ključ): sažetak i potrebno znanje.
// Služe za stavke, pokrivenost i provjeru „preblizu“; nikad se ne kopiraju u zadatke.
@Component
public class PlanExamTasksStep implements BuildStep {

    public static final String KIND = "plan_exam_tasks";

    static final String SYSTEM = """
            Iz ispita za %s (zadaci i ključ ako postoji) izvuci SVAKI zadatak.
            Vrati JSON {"zadaci":[{"broj":"…","sazetak":"jedna rečenica što se traži","odgovor":"iz ključa ako postoji",
            "potrebno_znanje":["pojmovi i zakoni koje učenik mora razumjeti, kratko"]}]}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanExamTasksStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
        long materialId = mapper.readTree(job.input()).path("materialId").asLong();
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        List<String> pages = jdbc.queryForList("""
                SELECT '### str. ' || page_no || ' (' || kind || ')' || E'\\n' || markdown FROM material_page
                WHERE material_id = :m AND kind IN ('ispit', 'rjesenja') AND markdown IS NOT NULL ORDER BY page_no""",
                new MapSqlParameterSource("m", materialId), String.class);
        JsonNode out = plan.json(llm.chat(job, plan.cheapModel, SYSTEM.formatted(course.describe()),
                String.join("\n\n", pages), OwnCourseLlm.Options.of(null, 24000)).content());
        jdbc.update("DELETE FROM exam_task WHERE material_id = :m", new MapSqlParameterSource("m", materialId));
        int n = 0;
        for (JsonNode t : out.path("zadaci")) {
            String summary = t.path("sazetak").asText("").strip();
            if (summary.isEmpty()) continue;
            jdbc.update("""
                    INSERT INTO exam_task (course_id, material_id, number, summary, answer, needed)
                    VALUES (:c, :m, :n, :s, :a, :k)""",
                    new MapSqlParameterSource(Map.of("c", job.courseId(), "m", materialId, "s", summary))
                            .addValue("n", t.path("broj").asText("")).addValue("a", t.path("odgovor").asText(""))
                            .addValue("k", plan.toJson(t.path("potrebno_znanje"))));
            n++;
        }
        return n + " zadataka";
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterScope(job);
    }
}
