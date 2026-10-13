package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

// Nova lekcija koju je kreator dodao samo nazivom (ili „ipak ne znam“ iz predznanja): model joj izradi
// koncept, 4–6 stavki znanja i preduvjete iz postojećeg plana. Preduvjeti smiju biti samo lekcije ispred nove
// (inače nastaje krug ili lekcija ispred svog preduvjeta). Nakon toga se ponovno provjeravaju nova lekcija i lekcije
// s ozbiljnim upozorenjem, jer nova lekcija može ukloniti (ili promijeniti) njihov razlog.
@Component
public class PlanNewLessonStep implements BuildStep {

    public static final String KIND = "plan_new_lesson";

    static final String SYSTEM = """
            Kreator je u plan učenja za %s dodao novu lekciju samo nazivom. Napiši joj:
            - koncept: jedna rečenica što lekcija uči;
            - 4–6 stavki znanja (formula, definicija, pojava, postupak ili tipična zamka), sve što treba za naziv lekcije;
            - preduvjete: id-ove lekcija IZNAD oznake „NOVA LEKCIJA“ čije gradivo nova lekcija koristi (samo izravne, obično 0–3);
              lekcije ispod oznake dolaze poslije nje i ne smiju joj biti preduvjet.
            Predznanje (ne treba lekciju): %s.
            Vrati JSON {"koncept":"…","stavke":[{"tekst":"…","vrsta":"formula|definicija|pojava|postupak|zamka"}],"preduvjeti":[id lekcije]}.

            PLAN (id lekcije, naziv i stavke):
            %s""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanNewLessonStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
        long objectiveId = mapper.readTree(job.input()).path("objectiveId").asLong();
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        List<Map<String, Object>> name = jdbc.queryForList("SELECT objective_name FROM learning_objective WHERE objective_id = :o",
                new MapSqlParameterSource("o", objectiveId));
        if (name.isEmpty()) return "lekcija je u međuvremenu obrisana";
        PlanSupport.CourseInfo course = plan.course(job.courseId());

        Integer own = jdbc.queryForObject("SELECT position FROM course_objective WHERE course_id = :c AND objective_id = :o",
                new MapSqlParameterSource("c", job.courseId()).addValue("o", objectiveId), Integer.class);
        Map<Long, StringBuilder> lessons = new LinkedHashMap<>();
        Set<Long> before = new HashSet<>();
        jdbc.query("""
                SELECT lo.objective_id, lo.objective_name, k.text, co.position FROM course_objective co
                JOIN learning_objective lo ON lo.objective_id = co.objective_id
                LEFT JOIN knowledge_item k ON k.objective_id = lo.objective_id AND k.duplicate_of IS NULL
                WHERE co.course_id = :c AND lo.objective_id <> :o ORDER BY co.position, k.position""",
                c.addValue("o", objectiveId), rs -> {
                    long id = rs.getLong(1);
                    String lessonName = rs.getString(2);
                    String item = rs.getString(3);
                    boolean earlier = own == null || rs.getInt(4) < own;
                    if (earlier) before.add(id);
                    StringBuilder sb = lessons.computeIfAbsent(id, k -> new StringBuilder(
                            (!earlier && lessons.keySet().stream().allMatch(before::contains)
                                    ? "--- NOVA LEKCIJA DOLAZI OVDJE ---\n" : "") + k + " " + lessonName));
                    if (item != null) sb.append("\n  - ").append(item);
                });
        if (lessons.keySet().stream().allMatch(before::contains)) {
            lessons.put(-1L, new StringBuilder("--- NOVA LEKCIJA DOLAZI OVDJE (na kraju plana) ---"));
        }
        String system = SYSTEM.formatted(course.describe(), String.join("; ", plan.assumedKnowledge(course)),
                String.join("\n", lessons.values()));

        JsonNode out = plan.json(llm.chat(job, plan.planModel, system, "NOVA LEKCIJA: " + name.get(0).get("objective_name"),
                OwnCourseLlm.Options.of("low", 4000)).content());
        MapSqlParameterSource o = new MapSqlParameterSource("o", objectiveId).addValue("c", job.courseId());
        jdbc.update("UPDATE learning_objective SET concept = :k WHERE objective_id = :o", o.addValue("k", out.path("koncept").asText("")));
        jdbc.update("DELETE FROM knowledge_item WHERE objective_id = :o", o);
        int position = 0;
        for (JsonNode s : out.path("stavke")) {
            String text = s.path("tekst").asText("").strip();
            if (text.isEmpty()) continue;
            String kind = s.path("vrsta").asText("").strip().toLowerCase();
            jdbc.update("""
                    INSERT INTO knowledge_item (course_id, objective_id, position, text, kind) VALUES (:c, :o, :p, :t, :k)""",
                    new MapSqlParameterSource(Map.of("c", job.courseId(), "o", objectiveId, "p", ++position, "t", text))
                            .addValue("k", PlanPointsStep.KINDS.contains(kind) ? kind : null));
        }
        for (JsonNode p : out.path("preduvjeti")) {
            long pre = p.asLong();
            if (pre == objectiveId || !before.contains(pre)) continue; // samo lekcije ispred nove
            jdbc.update("""
                    INSERT INTO objective_prerequisite (objective_id, prerequisite_id)
                    SELECT :o, :p WHERE NOT EXISTS (SELECT 1 FROM objective_prerequisite WHERE objective_id = :o AND prerequisite_id = :p)""",
                    new MapSqlParameterSource("o", objectiveId).addValue("p", pre));
        }
        return position + " stavki";
    }

    @Override
    public void afterComplete(BuildJob job) {
        try {
            long objectiveId = mapper.readTree(job.input()).path("objectiveId").asLong();
            Set<Long> again = new TreeSet<>(jdbc.queryForList("""
                    SELECT DISTINCT objective_id FROM plan_warning
                    WHERE course_id = :c AND serious AND NOT ignored AND objective_id IS NOT NULL""",
                    new MapSqlParameterSource("c", job.courseId()), Long.class));
            again.add(objectiveId);
            flow.recheck(job.courseId(), job.accountId(), System.currentTimeMillis(), again);
        } catch (Exception ignored) {
            // provjera će se ionako pokrenuti s idućom izmjenom kreatora
        }
    }
}
