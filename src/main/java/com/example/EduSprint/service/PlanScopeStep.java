package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Opseg gradiva: ishodi (teme) s razradom iz kataloga, kurikuluma ili skripte. Postaju cjeline plana.
// Za svaki materijal sa stranicama ispita dodaje se korak koji izvlači zadatke s ispita.
@Component
public class PlanScopeStep implements BuildStep {

    public static final String KIND = "plan_scope";

    static final String SYSTEM = """
            Iz materijala za %s izvuci sve ISHODE (teme koje učenik mora savladati) s razradom.
            - Ako materijal ima ispitni katalog ili kurikulum, ishodi su njegovi ishodi, a razrada doslovno ono što piše uz njih.
            - Ako ga nema, ishodi su glavne teme skripte (otprilike jedna po poglavlju), a razrada su njihove podteme.
            - Ne izmišljaj ništa čega u materijalu nema. Poredaj ishode redom kojim se uče.
            Vrati JSON {"ishodi":[{"oznaka":"oznaka iz kataloga ili prazno","naslov":"…","razrada":["…"]}]}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final BuildJobQueue queue;

    public PlanScopeStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow, BuildJobQueue queue) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.flow = flow;
        this.queue = queue;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) {
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        // Katalog, kurikulum i skripta određuju opseg; ako ih nema, opseg se izvodi iz ispita.
        List<String> pages = pages(job.courseId(), "('kurikulum', 'skripta')");
        if (pages.isEmpty()) pages = pages(job.courseId(), "('ispit', 'rjesenja')");
        if (pages.isEmpty()) throw new IllegalStateException("U materijalu nema stranica s gradivom.");

        JsonNode out = plan.json(llm.chat(job, plan.planModel, SYSTEM.formatted(course.describe()),
                String.join("\n\n", pages), OwnCourseLlm.Options.of("medium", 24000)).content());
        JsonNode outcomes = out.path("ishodi");
        if (!outcomes.isArray() || outcomes.isEmpty()) throw new IllegalStateException("Model nije našao nijedan ishod.");

        jdbc.update("DELETE FROM plan_outcome WHERE course_id = :c", c); // ponovljeni korak počinje ispočetka
        int position = 0;
        for (JsonNode o : outcomes) {
            String title = o.path("naslov").asText("").strip();
            if (title.isEmpty()) continue;
            jdbc.update("INSERT INTO plan_outcome (course_id, position, code, title, details) VALUES (:c, :p, :code, :t, :d)",
                    new MapSqlParameterSource(Map.of("c", job.courseId(), "p", ++position, "t", title))
                            .addValue("code", o.path("oznaka").asText("").strip())
                            .addValue("d", plan.toJson(o.path("razrada"))));
        }
        if (jdbc.queryForObject("SELECT count(*) FROM build_job WHERE course_id = :c AND kind = 'plan_exam_tasks'", c, Integer.class) == 0) {
            List<Long> exams = jdbc.queryForList("""
                    SELECT DISTINCT m.material_id FROM course_material m JOIN material_page p ON p.material_id = m.material_id
                    WHERE m.course_id = :c AND p.kind IN ('ispit', 'rjesenja') ORDER BY m.material_id""", c, Long.class);
            for (Long materialId : exams) {
                queue.enqueue(job.courseId(), job.accountId(), PlanExamTasksStep.KIND, "ispit " + materialId,
                        BuildJobQueue.PRIORITY_BACKGROUND, true, "{\"materialId\":" + materialId + "}");
            }
        }
        return position + " ishoda";
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterScope(job);
    }

    private List<String> pages(long courseId, String kinds) {
        return jdbc.queryForList("""
                SELECT '### ' || m.original_name || ', str. ' || p.page_no || E'\\n' || p.markdown
                FROM material_page p JOIN course_material m ON m.material_id = p.material_id
                WHERE m.course_id = :c AND p.kind IN """ + kinds + " AND p.markdown IS NOT NULL ORDER BY m.material_id, p.page_no",
                new MapSqlParameterSource("c", courseId), String.class);
    }
}
