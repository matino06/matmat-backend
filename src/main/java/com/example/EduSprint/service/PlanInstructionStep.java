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

// AI uputa kreatora za plan („spoji akceleraciju i usporavanje“, „popravi ovo upozorenje“): model plana vraća
// PRIJEDLOG operacija, a plan se mijenja tek kad ga kreator prihvati. Model vidi lekcije s preduvjetima, stavke
// s id-ovima i upozorenja koja kreator vidi, pa grešku na razini stavke može popraviti na razini stavke.
// Plan je u system poruci (kešira se između uputa). Izlaz posla je JSON {"poruka":"…","operacije":[…]}.
@Component
public class PlanInstructionStep implements BuildStep {

    public static final String KIND = "plan_instruction";
    static final Set<String> OPS = Set.of("spoji", "preimenuj", "obrisi", "dodaj", "premjesti",
            "prepravi_stavku", "premjesti_stavku", "dodaj_stavku", "dodaj_preduvjet", "ukloni_preduvjet");

    static final String SYSTEM = """
            Pomažeš kreatoru urediti plan učenja za %s. Plan je popis lekcija redom učenja:
            id | redni broj | cjelina | naziv | preduvjeti (id-ovi lekcija), a ispod lekcije njezine stavke znanja: [id stavke] tekst.
            Kreator napiše uputu; ti predložiš izmjene plana kao operacije. Ne mijenjaj ništa što uputa ne traži.
            Pravila plana: lekcija uči jednu ideju kroz 4–6 stavki znanja; lekcija smije koristiti samo ono što uče njezine
            stavke i stavke lekcija prije nje; preduvjet lekcije mora biti ispred nje.

            Operacije:
            - {"op":"spoji","a":id,"b":id}: lekcija b se spaja u lekciju a;
            - {"op":"preimenuj","id":id,"naziv":"…"};
            - {"op":"obrisi","id":id};
            - {"op":"dodaj","iza":id ili null za početak,"naziv":"…"}: nova lekcija (stavke i preduvjete joj kasnije izradi AI);
            - {"op":"premjesti","id":id,"iza":id ili null za početak};
            - {"op":"prepravi_stavku","stavka":id stavke,"tekst":"…"};
            - {"op":"premjesti_stavku","stavka":id stavke,"u":id lekcije}: stavka prelazi u drugu lekciju;
            - {"op":"dodaj_stavku","id":id lekcije,"tekst":"…"};
            - {"op":"dodaj_preduvjet","id":id lekcije,"preduvjet":id lekcije};
            - {"op":"ukloni_preduvjet","id":id lekcije,"preduvjet":id lekcije}.

            Kako se popravljaju upozorenja (najmanja izmjena koja ih stvarno uklanja):
            - „X koristi pojam P, koji se uči tek u lekciji Y“: P mora biti naučen prije X. Premjesti stavku koja uči P iz Y u X
              (ili u lekciju ispred X), ili prepravi stavku iz X tako da ne koristi P. Ako Y ne ovisi o X, može i premjesti X iza Y.
              Nikad ne dodaj novu lekciju za pojam koji već uči neka stavka: nova lekcija ne mijenja to što X koristi stavku iz Y.
            - „X koristi pojam P iz lekcije Y, koja mu nije preduvjet“: dodaj_preduvjet (X treba Y).
            - „X dolazi prije svog preduvjeta Y“: premjesti X iza Y, ili ukloni_preduvjet ako X stvarno ne treba Y.
            - „X koristi pojam P, a nijedna lekcija ga ne uči“: dodaj_stavku u X ili u lekciju ispred X.
            - „Plan ne pokriva zadatak s ispita“: dodaj stavku ili lekciju koja uči ono što nedostaje.
            Vrati JSON {"poruka":"jedna ili dvije rečenice kreatoru što predlažeš ili zašto ne možeš","operacije":[…]}.

            PLAN:
            %s

            UPOZORENJA koja kreator vidi (zanemarena nisu navedena):
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
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        PlanSupport.CourseInfo course = plan.course(job.courseId());

        Map<Long, List<Long>> prerequisites = new HashMap<>();
        jdbc.query("""
                SELECT op.objective_id, op.prerequisite_id FROM objective_prerequisite op
                JOIN course_objective co ON co.objective_id = op.objective_id AND co.course_id = :c ORDER BY op.prerequisite_id""", c,
                rs -> { prerequisites.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getLong(2)); });
        Map<Long, StringBuilder> lessons = new LinkedHashMap<>();
        Map<Long, Long> itemLesson = new HashMap<>();
        jdbc.query("""
                SELECT lo.objective_id, co.position, s.subfield_name, lo.objective_name, k.item_id, k.text
                FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                JOIN subfield_of_study s ON s.subfield_id = lo.subfield_id
                LEFT JOIN knowledge_item k ON k.objective_id = lo.objective_id AND k.duplicate_of IS NULL
                WHERE co.course_id = :c ORDER BY co.position, k.position""", c, rs -> {
            long id = rs.getLong(1);
            String head = id + " | " + rs.getInt(2) + ". | " + rs.getString(3) + " | " + rs.getString(4)
                    + " | preduvjeti: " + prerequisites.getOrDefault(id, List.of());
            StringBuilder sb = lessons.computeIfAbsent(id, k -> new StringBuilder(head));
            long item = rs.getLong(5);
            if (!rs.wasNull()) {
                itemLesson.put(item, id);
                sb.append("\n    [").append(item).append("] ").append(rs.getString(6));
            }
        });
        List<String> warnings = jdbc.query("""
                SELECT kind, serious, text, objective_id, prerequisite_id, detail FROM plan_warning
                WHERE course_id = :c AND NOT ignored ORDER BY serious DESC, warning_id""", c, (rs, i) -> {
            String where = rs.getObject(4) == null ? "" : " (lekcija " + rs.getLong(4)
                    + (rs.getObject(5) == null ? "" : ", druga lekcija " + rs.getLong(5)) + ")";
            return "- [" + rs.getString(1) + (rs.getBoolean(2) ? ", ozbiljno" : "") + "] " + rs.getString(3) + where
                    + (rs.getString(6) == null ? "" : " Nedostaje: " + rs.getString(6));
        });

        JsonNode out = plan.json(llm.chat(job, plan.planModel,
                SYSTEM.formatted(course.describe(), String.join("\n", lessons.values()), warnings.isEmpty() ? "nema" : String.join("\n", warnings)),
                "UPUTA KREATORA: " + instruction, OwnCourseLlm.Options.of("medium", 6000)).content());
        return mapper.writeValueAsString(sanitize(out, lessons.keySet(), itemLesson));
    }

    // Zadržava samo operacije koje se odnose na postojeće lekcije i stavke i imaju ono što trebaju.
    ObjectNode sanitize(JsonNode out, Set<Long> lessons, Map<Long, Long> items) {
        ObjectNode result = mapper.createObjectNode();
        result.put("poruka", out.path("poruka").asText(""));
        ArrayNode ops = result.putArray("operacije");
        for (JsonNode o : out.path("operacije")) {
            String op = o.path("op").asText("");
            if (!OPS.contains(op)) continue;
            long id = o.path("id").asLong(), item = o.path("stavka").asLong(), pre = o.path("preduvjet").asLong();
            boolean text = !o.path("tekst").asText("").isBlank();
            boolean ok = switch (op) {
                case "spoji" -> lessons.contains(o.path("a").asLong()) && lessons.contains(o.path("b").asLong())
                        && o.path("a").asLong() != o.path("b").asLong();
                case "preimenuj" -> lessons.contains(id) && !o.path("naziv").asText("").isBlank();
                case "obrisi" -> lessons.contains(id);
                case "dodaj" -> !o.path("naziv").asText("").isBlank() && (o.path("iza").isNull() || o.path("iza").isMissingNode()
                        || lessons.contains(o.path("iza").asLong()));
                case "premjesti" -> lessons.contains(id) && (o.path("iza").isNull() || o.path("iza").isMissingNode()
                        || lessons.contains(o.path("iza").asLong()) && o.path("iza").asLong() != id);
                case "prepravi_stavku" -> items.containsKey(item) && text;
                case "premjesti_stavku" -> items.containsKey(item) && lessons.contains(o.path("u").asLong())
                        && !items.get(item).equals(o.path("u").asLong());
                case "dodaj_stavku" -> lessons.contains(id) && text;
                case "dodaj_preduvjet", "ukloni_preduvjet" -> lessons.contains(id) && lessons.contains(pre) && id != pre;
                default -> false;
            };
            if (ok) ops.add(o);
        }
        return result;
    }
}
