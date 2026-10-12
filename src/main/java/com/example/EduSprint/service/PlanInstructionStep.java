package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

// AI uputa kreatora za plan („spoji akceleraciju i usporavanje“): model plana vraća PRIJEDLOG operacija,
// a plan se mijenja tek kad ga kreator prihvati. Plan je u system poruci (kešira se između uputa).
// Izlaz posla je JSON {"poruka":"…","operacije":[…]} s operacijama koje se odnose na postojeće lekcije.
@Component
public class PlanInstructionStep implements BuildStep {

    public static final String KIND = "plan_instruction";
    static final Set<String> OPS = Set.of("spoji", "preimenuj", "obrisi", "dodaj", "premjesti");

    static final String SYSTEM = """
            Pomažeš kreatoru urediti plan učenja za %s. Plan je popis lekcija redom učenja (id, redni broj, cjelina, naziv, stavke).
            Kreator napiše uputu; ti predložiš izmjene plana kao operacije. Ne mijenjaj ništa što uputa ne traži.
            Pravila plana: lekcija uči jednu ideju kroz 4–6 stavki znanja; lekcija koristi samo ono što su je naučile ranije lekcije.
            Operacije:
            - {"op":"spoji","a":id,"b":id}: lekcija b se spaja u lekciju a;
            - {"op":"preimenuj","id":id,"naziv":"…"};
            - {"op":"obrisi","id":id};
            - {"op":"dodaj","iza":id ili null za početak,"naziv":"…"}: nova lekcija (stavke joj izradi AI kasnije);
            - {"op":"premjesti","id":id,"iza":id ili null za početak}.
            Vrati JSON {"poruka":"jedna ili dvije rečenice kreatoru što predlažeš ili zašto ne možeš","operacije":[…]}.

            PLAN:
            %s""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanInstructionStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) throws Exception {
        String instruction = mapper.readTree(job.input()).path("text").asText("");
        PlanSupport.CourseInfo course = plan.course(job.courseId());
        Map<Long, StringBuilder> lessons = new LinkedHashMap<>();
        jdbc.query("""
                SELECT lo.objective_id, co.position, s.subfield_name, lo.objective_name, k.text
                FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                JOIN subfield_of_study s ON s.subfield_id = lo.subfield_id
                LEFT JOIN knowledge_item k ON k.objective_id = lo.objective_id AND k.duplicate_of IS NULL
                WHERE co.course_id = :c ORDER BY co.position, k.position""", new MapSqlParameterSource("c", job.courseId()), rs -> {
            long id = rs.getLong(1);
            String head = id + " | " + rs.getInt(2) + ". | " + rs.getString(3) + " | " + rs.getString(4);
            String item = rs.getString(5);
            StringBuilder sb = lessons.computeIfAbsent(id, k -> new StringBuilder(head));
            if (item != null) sb.append("\n    - ").append(item);
        });
        JsonNode out = plan.json(llm.chat(job, plan.planModel, SYSTEM.formatted(course.describe(), String.join("\n", lessons.values())),
                "UPUTA KREATORA: " + instruction, OwnCourseLlm.Options.of("medium", 4000)).content());
        return mapper.writeValueAsString(sanitize(out, lessons.keySet()));
    }

    // Zadržava samo operacije koje se odnose na postojeće lekcije i imaju ono što trebaju.
    ObjectNode sanitize(JsonNode out, Set<Long> lessons) {
        ObjectNode result = mapper.createObjectNode();
        result.put("poruka", out.path("poruka").asText(""));
        ArrayNode ops = result.putArray("operacije");
        for (JsonNode o : out.path("operacije")) {
            String op = o.path("op").asText("");
            if (!OPS.contains(op)) continue;
            boolean ok = switch (op) {
                case "spoji" -> lessons.contains(o.path("a").asLong()) && lessons.contains(o.path("b").asLong())
                        && o.path("a").asLong() != o.path("b").asLong();
                case "preimenuj" -> lessons.contains(o.path("id").asLong()) && !o.path("naziv").asText("").isBlank();
                case "obrisi" -> lessons.contains(o.path("id").asLong());
                case "dodaj" -> !o.path("naziv").asText("").isBlank() && (o.path("iza").isNull() || o.path("iza").isMissingNode()
                        || lessons.contains(o.path("iza").asLong()));
                case "premjesti" -> lessons.contains(o.path("id").asLong()) && (o.path("iza").isNull() || o.path("iza").isMissingNode()
                        || lessons.contains(o.path("iza").asLong()) && o.path("iza").asLong() != o.path("id").asLong());
                default -> false;
            };
            if (ok) ops.add(o);
        }
        return result;
    }
}
