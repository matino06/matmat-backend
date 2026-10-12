package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

// Stavke jednog ishoda grupiraju se u lekcije od 4–6 stavki; svaku lekciju uči jedan zadatak s najviše
// tri podzadatka. Veličinu i redoslijed zatim popravlja kod (PlanRepair) u završnom koraku.
@Component
public class PlanGroupStep implements BuildStep {

    public static final String KIND = "plan_group";

    static final String SYSTEM = """
            Grupiraj stavke znanja jednog ishoda iz %s u LEKCIJE. Svaka lekcija ima %d–%d stavki (manje samo ako ishod
            nema dovoljno stavki), stavke u lekciji čine povezanu cjelinu, i lekcija se uči JEDNIM zadatkom s najviše tri
            podzadatka a)–c) (svaki podzadatak vodi kroz jednu ili dvije stavke). Svaka stavka je u točno jednoj lekciji.
            Preduvjet stavke mora biti u ranijoj lekciji ili u istoj lekciji. Poredaj lekcije redom učenja.
            Vrati JSON {"lekcije":[{"naziv":"…","koncept":"jedna rečenica: što lekcija uči","stavke":[id]}]}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanGroupStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
        long outcomeId = mapper.readTree(job.input()).path("outcomeId").asLong();
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        String title = jdbc.queryForObject("SELECT title FROM plan_outcome WHERE outcome_id = :o",
                new MapSqlParameterSource("o", outcomeId), String.class);
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT item_id, kind, text, prerequisites FROM knowledge_item
                WHERE outcome_id = :o AND duplicate_of IS NULL ORDER BY position""", new MapSqlParameterSource("o", outcomeId));
        jdbc.update("DELETE FROM draft_lesson WHERE outcome_id = :o", new MapSqlParameterSource("o", outcomeId));
        if (items.isEmpty()) return "0 lekcija"; // sve stavke ishoda su duplikati drugih ishoda

        Set<Long> own = new HashSet<>();
        StringBuilder list = new StringBuilder("ISHOD: " + title + "\n\nSTAVKE:\n");
        for (Map<String, Object> i : items) {
            long id = ((Number) i.get("item_id")).longValue();
            own.add(id);
            list.append(id).append(" [").append(i.get("kind") == null ? "" : i.get("kind")).append("] ")
                    .append(i.get("text")).append(" | preduvjeti: ").append(i.get("prerequisites") == null ? "—" : i.get("prerequisites")).append('\n');
        }
        JsonNode out = plan.json(llm.chat(job, plan.planModel,
                SYSTEM.formatted(course.describe(), PlanRepair.MIN_ITEMS, PlanRepair.MAX_ITEMS), list.toString(),
                OwnCourseLlm.Options.of("medium", 8000)).content());

        Set<Long> placed = new HashSet<>();
        int position = 0;
        for (JsonNode l : out.path("lekcije")) {
            List<Long> lessonItems = new ArrayList<>();
            for (JsonNode s : l.path("stavke")) {
                long id = s.asLong();
                if (own.contains(id) && placed.add(id)) lessonItems.add(id);
            }
            if (lessonItems.isEmpty()) continue;
            insert(job.courseId(), outcomeId, ++position, l.path("naziv").asText("Lekcija " + position),
                    l.path("koncept").asText(""), lessonItems);
        }
        // Stavka koju model nije stavio ni u jednu lekciju ne smije nestati: ide u zadnju lekciju ishoda,
        // a premalu ili preveliku lekciju kasnije popravlja kod.
        List<Long> missing = own.stream().filter(id -> !placed.contains(id)).sorted().toList();
        if (!missing.isEmpty()) insert(job.courseId(), outcomeId, ++position, title, "", missing);
        return position + " lekcija";
    }

    private void insert(long courseId, long outcomeId, int position, String name, String concept, List<Long> items) {
        jdbc.update("""
                INSERT INTO draft_lesson (course_id, outcome_id, position, name, concept, items)
                VALUES (:c, :o, :p, :n, :k, :i)""",
                new MapSqlParameterSource(Map.of("c", courseId, "o", outcomeId, "p", position, "n", name.strip()))
                        .addValue("k", concept.strip()).addValue("i", plan.toJson(items)));
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterGroup(job);
    }
}
