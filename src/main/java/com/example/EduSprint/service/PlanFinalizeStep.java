package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

// Završetak izrade plana: popravak kodom (PlanRepair), grupiranje cjelina u područja (jedan poziv modela)
// i upis u postojeću hijerarhiju: područje = field_of_study, cjelina (ishod) = subfield_of_study,
// lekcija = learning_objective, redoslijed u course_objective.position, preduvjeti u objective_prerequisite.
// Zatim slijede provjere plana (PlanZeroStep, PlanCoverageStep) i popravak (PlanCheckRepairStep).
@Component
public class PlanFinalizeStep implements BuildStep {

    public static final String KIND = "plan_finalize";
    public static final String STATUS_DRAFT = "nacrt";
    public static final String STATUS_TOO_BIG = "prevelik";

    static final String AREAS = """
            Grupiraj cjeline plana za %s u 3–8 PODRUČJA: velike dijelove gradiva (npr. Mehanika, Toplina).
            Svaka cjelina je u točno jednom području; područja poredaj redom učenja. Ako cjelina ima malo, može i manje područja.
            Vrati JSON {"podrucja":[{"naziv":"…","cjeline":[broj cjeline]}]}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final TransactionTemplate transactions;
    private final PlanFlow flow;

    public PlanFinalizeStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan,
                            TransactionTemplate transactions, PlanFlow flow) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.transactions = transactions;
        this.flow = flow;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) {
        long courseId = job.courseId();
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        if (jdbc.queryForObject("SELECT count(*) FROM course_objective WHERE course_id = :c", c, Integer.class) > 0) {
            return "plan je već upisan"; // ponovljeni korak nakon uspješnog upisa
        }
        PlanSupport.CourseInfo course = plan.course(courseId);

        List<Map<String, Object>> outcomes = jdbc.queryForList(
                "SELECT outcome_id, position, title FROM plan_outcome WHERE course_id = :c ORDER BY position", c);
        Map<Long, Integer> outcomePos = new HashMap<>();
        for (Map<String, Object> o : outcomes) outcomePos.put(num(o.get("outcome_id")), ((Number) o.get("position")).intValue());

        List<PlanRepair.Lesson> drafts = new ArrayList<>();
        for (Map<String, Object> d : jdbc.queryForList(
                "SELECT outcome_id, position, name, concept, items FROM draft_lesson WHERE course_id = :c", c)) {
            List<Long> items = new ArrayList<>();
            plan.readJson((String) d.get("items")).forEach(n -> items.add(n.asLong()));
            long outcomeId = num(d.get("outcome_id"));
            drafts.add(new PlanRepair.Lesson(outcomeId, outcomePos.getOrDefault(outcomeId, 0),
                    ((Number) d.get("position")).intValue(), (String) d.get("name"), Objects.toString(d.get("concept"), ""), items));
        }
        Map<Long, Set<Long>> itemPre = new HashMap<>();
        for (Map<String, Object> i : jdbc.queryForList(
                "SELECT item_id, prerequisites FROM knowledge_item WHERE course_id = :c AND duplicate_of IS NULL", c)) {
            Set<Long> pre = new HashSet<>();
            plan.readJson((String) i.get("prerequisites")).forEach(n -> pre.add(n.asLong()));
            itemPre.put(num(i.get("item_id")), pre);
        }

        PlanRepair.Result result = PlanRepair.repair(drafts, itemPre);
        if (result.lessons().size() > PlanRepair.MAX_LESSONS) {
            jdbc.update("UPDATE course SET status = :s WHERE course_id = :c", c.addValue("s", STATUS_TOO_BIG));
            return result.lessons().size() + " lekcija: previše";
        }

        // cjelina bez ijedne lekcije (ishod sav dodatni sadržaj ili samo duplikati) ne ulazi u plan
        Set<Long> withLessons = new HashSet<>();
        result.lessons().forEach(l -> withLessons.add(l.outcomeId()));
        List<Map<String, Object>> used = outcomes.stream().filter(o -> withLessons.contains(num(o.get("outcome_id")))).toList();
        Map<Long, String> areaOf = areas(job, course, used);
        transactions.executeWithoutResult(tx -> write(course, used, areaOf, result));
        return result.lessons().size() + " lekcija";
    }

    // Cjelina → naziv područja. Ako model ne uspije ili izostavi cjelinu, ide u zajedničko područje.
    private Map<Long, String> areas(BuildJob job, PlanSupport.CourseInfo course, List<Map<String, Object>> outcomes) {
        Map<Integer, Long> byPos = new HashMap<>();
        StringBuilder list = new StringBuilder();
        for (Map<String, Object> o : outcomes) {
            int pos = ((Number) o.get("position")).intValue();
            byPos.put(pos, num(o.get("outcome_id")));
            list.append(pos).append(". ").append(o.get("title")).append('\n');
        }
        Map<Long, String> areaOf = new LinkedHashMap<>();
        try {
            JsonNode out = plan.json(llm.chat(job, plan.planModel, AREAS.formatted(course.describe()), list.toString(),
                    OwnCourseLlm.Options.of("low", 4000)).content());
            for (JsonNode a : out.path("podrucja")) {
                String name = a.path("naziv").asText("").strip();
                for (JsonNode p : a.path("cjeline")) {
                    Long id = byPos.get(p.asInt());
                    if (id != null && !name.isEmpty()) areaOf.putIfAbsent(id, name);
                }
            }
        } catch (RuntimeException e) {
            if (e instanceof OwnCourseLlm.GlobalLimitException) throw e;
        }
        for (Long id : byPos.values()) areaOf.putIfAbsent(id, "Gradivo");
        return areaOf;
    }

    private void write(PlanSupport.CourseInfo course, List<Map<String, Object>> outcomes, Map<Long, String> areaOf,
                       PlanRepair.Result result) {
        long courseId = course.courseId();
        Map<String, Long> fieldIds = new LinkedHashMap<>();
        Map<Long, Long> subfieldOf = new HashMap<>();
        for (Map<String, Object> o : outcomes) {
            long outcomeId = num(o.get("outcome_id"));
            String area = areaOf.get(outcomeId);
            Long fieldId = fieldIds.computeIfAbsent(area, a -> jdbc.queryForObject(
                    "INSERT INTO field_of_study (field_name) VALUES (:n) RETURNING field_id",
                    new MapSqlParameterSource("n", a), Long.class));
            subfieldOf.put(outcomeId, jdbc.queryForObject(
                    "INSERT INTO subfield_of_study (field_id, subfield_name) VALUES (:f, :n) RETURNING subfield_id",
                    new MapSqlParameterSource("f", fieldId).addValue("n", o.get("title")), Long.class));
        }

        List<Long> objectiveIds = new ArrayList<>();
        Map<Long, Integer> lessonsPerField = new HashMap<>();
        for (int k = 0; k < result.lessons().size(); k++) {
            PlanRepair.Lesson l = result.lessons().get(k);
            Long objectiveId = jdbc.queryForObject("""
                    INSERT INTO learning_objective (subfield_id, objective_name, concept, kind)
                    VALUES (:s, :n, :c, 'koncept') RETURNING objective_id""",
                    new MapSqlParameterSource("s", subfieldOf.get(l.outcomeId())).addValue("n", l.name())
                            .addValue("c", l.concept()), Long.class);
            objectiveIds.add(objectiveId);
            jdbc.update("INSERT INTO course_objective (course_id, objective_id, position) VALUES (:c, :o, :p)",
                    new MapSqlParameterSource("c", courseId).addValue("o", objectiveId).addValue("p", k + 1));
            jdbc.update("UPDATE knowledge_item SET objective_id = :o WHERE item_id IN (:i)",
                    new MapSqlParameterSource("o", objectiveId).addValue("i", l.items()));
            lessonsPerField.merge(fieldIds.get(areaOf.get(l.outcomeId())), 1, Integer::sum);
        }
        for (int k = 0; k < result.lessons().size(); k++) {
            for (int p : result.prerequisites().get(k)) {
                jdbc.update("INSERT INTO objective_prerequisite (objective_id, prerequisite_id) VALUES (:o, :p)",
                        new MapSqlParameterSource("o", objectiveIds.get(k)).addValue("p", objectiveIds.get(p)));
            }
        }
        // Težine spremnosti po području: udio lekcija (s ispitima ih kasnije preračunava provjera pokrivenosti).
        int total = Math.max(1, result.lessons().size());
        for (Map.Entry<Long, Integer> e : lessonsPerField.entrySet()) {
            jdbc.update("INSERT INTO course_field_weight (course_id, field_id, weight_percent) VALUES (:c, :f, :w)",
                    new MapSqlParameterSource("c", courseId).addValue("f", e.getKey())
                            .addValue("w", BigDecimal.valueOf(100.0 * e.getValue() / total).setScale(1, RoundingMode.HALF_UP)));
        }
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        if (jdbc.queryForObject("SELECT count(*) FROM assumed_knowledge WHERE course_id = :c", c, Integer.class) == 0) {
            jdbc.update("INSERT INTO assumed_knowledge (course_id, text) VALUES (:c, :t)",
                    c.addValue("t", PlanSupport.defaultAssumedKnowledge(course.level())));
        }
    }

    // Plan je upisan: slijede provjere (nulta pretpostavka, pokrivenost ispita) i popravak.
    @Override
    public void afterComplete(BuildJob job) {
        flow.afterFinalize(job);
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }
}
