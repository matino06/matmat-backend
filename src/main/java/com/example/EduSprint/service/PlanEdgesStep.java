package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

// Izravni preduvjeti za jednu seriju stavki. Cijeli popis stavki je u system poruci (isti za sve serije,
// pa se kešira), a serija u user poruci. Preduvjet koji pokazuje na duplikat preusmjerava se na stavku koja ostaje.
@Component
public class PlanEdgesStep implements BuildStep {

    public static final String KIND = "plan_edges";

    static final String SYSTEM = """
            Za svaku stavku iz SERIJE navedi njezine IZRAVNE preduvjete: stavke s CIJELOG popisa bez kojih se ona
            ne može razumjeti. Samo izravne (ne tranzitivne), obično 0–3. Predznanje (%s) nije stavka.
            Vrati JSON {"preduvjeti":{"id":[id]}}.

            CIJELI POPIS (%s):
            %s""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanEdgesStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
        List<Long> batch = new ArrayList<>();
        mapper.readTree(job.input()).path("itemIds").forEach(n -> batch.add(n.asLong()));
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        List<Map<String, Object>> all = jdbc.queryForList(
                "SELECT item_id, text, duplicate_of FROM knowledge_item WHERE course_id = :c ORDER BY item_id",
                new MapSqlParameterSource("c", job.courseId()));
        Map<Long, Long> alias = new HashMap<>();
        StringBuilder list = new StringBuilder();
        Map<Long, String> text = new HashMap<>();
        for (Map<String, Object> i : all) {
            long id = ((Number) i.get("item_id")).longValue();
            Object dup = i.get("duplicate_of");
            if (dup != null) alias.put(id, ((Number) dup).longValue());
            else list.append(id).append(' ').append(i.get("text")).append('\n');
            text.put(id, (String) i.get("text"));
        }
        String system = SYSTEM.formatted(String.join("; ", plan.assumedKnowledge(course)), course.describe(), list);
        StringBuilder user = new StringBuilder("SERIJA:\n");
        for (Long id : batch) user.append(id).append(' ').append(text.get(id)).append('\n');

        JsonNode out = plan.json(llm.chat(job, plan.planModel, system, user.toString(), OwnCourseLlm.Options.of("low", 8000)).content())
                .path("preduvjeti");
        for (Long id : batch) {
            Set<Long> pre = new TreeSet<>();
            for (JsonNode p : out.path(String.valueOf(id))) {
                long target = resolve(p.asLong(), alias);
                if (target != id && text.containsKey(target)) pre.add(target);
            }
            jdbc.update("UPDATE knowledge_item SET prerequisites = :p WHERE item_id = :i",
                    new MapSqlParameterSource("i", id).addValue("p", plan.toJson(pre)));
        }
        return batch.size() + " stavki";
    }

    // Duplikat može pokazivati na drugi duplikat; prati lanac do stavke koja ostaje.
    static long resolve(long id, Map<Long, Long> alias) {
        long current = id;
        for (int i = 0; i < 20 && alias.containsKey(current); i++) current = alias.get(current);
        return current;
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterEdges(job);
    }
}
