package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Stavke se izvlače po ishodu, pa se iste stvari ponavljaju. Duplikat se ne briše nego označava,
// da se preduvjeti koji na njega pokazuju mogu preusmjeriti na stavku koja ostaje.
@Component
public class PlanDedupeStep implements BuildStep {

    public static final String KIND = "plan_dedupe";

    static final String SYSTEM = """
            Ovo je popis stavki znanja iz %s izvučen po ishodima, pa se neke ponavljaju.
            Nađi grupe stavki koje su ISTA stvar (ista formula ili definicija). Vrati JSON
            {"duplikati":[{"zadrzi":id,"ukloni":[id]}]} (prazan popis ako ih nema).""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;

    public PlanDedupeStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
    public String run(BuildJob job) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        jdbc.update("UPDATE knowledge_item SET duplicate_of = NULL WHERE course_id = :c", c);
        List<Map<String, Object>> items = jdbc.queryForList("SELECT item_id, text FROM knowledge_item WHERE course_id = :c ORDER BY item_id", c);
        // svi ishodi prazni (sav materijal dodatni ili model nije odgovorio): bez ovoga izrada stoji bez greške
        if (items.isEmpty()) throw new IllegalStateException("Nijedan ishod nema stavki znanja: u materijalu nema gradiva za učenje.");
        Set<Long> ids = new HashSet<>();
        StringBuilder list = new StringBuilder();
        for (Map<String, Object> i : items) {
            long id = ((Number) i.get("item_id")).longValue();
            ids.add(id);
            list.append(id).append(' ').append(i.get("text")).append('\n');
        }
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        JsonNode out = plan.json(llm.chat(job, plan.planModel, SYSTEM.formatted(course.describe()), list.toString(),
                OwnCourseLlm.Options.of("low", 12000)).content());
        int removed = 0;
        for (JsonNode group : out.path("duplikati")) {
            long keep = group.path("zadrzi").asLong();
            if (!ids.contains(keep)) continue;
            for (JsonNode r : group.path("ukloni")) {
                long id = r.asLong();
                if (id == keep || !ids.contains(id)) continue;
                removed += jdbc.update("UPDATE knowledge_item SET duplicate_of = :k WHERE item_id = :i AND duplicate_of IS NULL",
                        new MapSqlParameterSource("k", keep).addValue("i", id));
            }
        }
        return removed + " duplikata";
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterDedupe(job);
    }
}
