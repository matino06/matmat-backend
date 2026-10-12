package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

// Brza priprema: nad lekcijama koncepata model plana dodaje lekcije primjene, svaka je jedan tip zadatka
// kakav dolazi na testu (ticket „Kako nastaje plan brze pripreme“, prompt brza_in/PROMPT.md iz prototipa).
// Mjesto i preduvjete određuje kod: lekcija primjene ide odmah iza zadnje lekcije koju koristi.
@Component
public class PlanApplicationStep implements BuildStep {

    public static final String KIND = "plan_application";
    public static final String LESSON_KIND = "primjena";
    static final Set<String> TYPES = Set.of("kombinacija", "nova situacija", "obrnuto", "graf", "zamka", "omjer", "miješana");

    static final String SYSTEM = """
            Izrađuješ LEKCIJE PRIMJENE za plan BRZE PRIPREME za %s, u aplikaciji za učenje kroz zadatke s laičkim objašnjenjima.
            Učenik za nekoliko dana ima test i uči samo ovdje. Već ima lekcije koncepata (ispod): svaka uči jednu novu ideju kroz
            jedan zadatak. Tvoj posao je dodati lekcije koje ga uvježbaju za TIPOVE ZADATAKA kakvi dolaze na testu, tako da nakon
            plana zna riješiti većinu zadataka iz tog gradiva.

            Lekcija primjene nema novi koncept: uči primijeniti već naučene koncepte na jednom tipu zadatka. Tipovi:
            - kombinacija: zadatak traži dva ili više koncepata zajedno (npr. kosina s trenjem: rastav sile teže + trenje + II. Newtonov zakon);
            - nova situacija: poznat koncept u drugačijem kontekstu koji učenika zbuni (npr. nakon dilatacije vremena: frekvencija
              otkucaja srca astronauta gledana sa Zemlje);
            - obrnuto: zadan je rezultat, a traži se ulazni podatak;
            - graf: čitanje ili crtanje grafa i zaključivanje iz njega;
            - zamka: tip zadatka u kojem učenici tipično griješe;
            - omjer: koliko se puta nešto promijeni kad se promijeni drugo (npr. dvostruka brzina → četiri puta veća kinetička energija);
            - miješana: na kraju područja, spaja više cjelina kao zadatak na testu.

            Pravila:
            1. Ne radi lekciju primjene za ono što zadatak lekcije koncepta već pokriva (izravna primjena jedne formule iz te lekcije).
            2. Po cjelini 1–3 lekcije primjene, za tipove koji se na testovima stvarno pojavljuju. Na kraju područja jedna miješana.
               Ukupno otprilike 40–60 %% broja lekcija koncepata.
            3. Pokrij većinu tipova zadataka iz gradiva, ne nužno sve; važnije je da su pokriveni najčešći.
            4. Tipove biraj iz GRADIVA (stavke lekcija). Zadaci s ispita služe samo da vidiš koje vrste zadataka postoje.
               Nikad ne kopiraj zadatak s ispita: primjer situacije mora imati drugi kontekst i druge brojeve.
            5. Nulta pretpostavka: lekcija primjene koristi samo koncepte iz lekcija koje navedeš u „koristi“ (i njihovih preduvjeta).
            6. Zadatak lekcije primjene ima najviše tri podzadatka a), b), c).

            Vrati isključivo JSON:
            {"lekcije_primjene":[{"naziv":"kratak naziv tipa zadatka","tip":"kombinacija|nova situacija|obrnuto|graf|zamka|omjer|miješana",
            "koristi":[id lekcija koncepata],"opis":"jedna rečenica: koji tip zadatka uči",
            "primjer_situacije":"jedna rečenica: situacija za zadatak (NE zadatak s ispita)"}]}

            LEKCIJE KONCEPATA (id | područje | cjelina | naziv, pa stavke):
            %s""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final TransactionTemplate transactions;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanApplicationStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow,
                               TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.flow = flow;
        this.transactions = transactions;
    }

    @Override
    public String kind() {
        return KIND;
    }

    record Application(String name, String type, String description, String example, List<Long> uses, long after) {
    }

    @Override
    public String run(BuildJob job) throws Exception {
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        removeApplications(job.courseId()); // ponovljeni pokušaj kreće ispočetka
        Map<Long, StringBuilder> lessons = new LinkedHashMap<>();
        Map<Long, Integer> positions = new HashMap<>();
        jdbc.query("""
                SELECT lo.objective_id, co.position, f.field_name, s.subfield_name, lo.objective_name, k.text
                FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                JOIN subfield_of_study s ON s.subfield_id = lo.subfield_id JOIN field_of_study f ON f.field_id = s.field_id
                LEFT JOIN knowledge_item k ON k.objective_id = lo.objective_id AND k.duplicate_of IS NULL
                WHERE co.course_id = :c ORDER BY co.position, k.position""", c, rs -> {
            long id = rs.getLong(1);
            positions.put(id, rs.getInt(2));
            String head = id + " | " + rs.getString(3) + " | " + rs.getString(4) + " | " + rs.getString(5);
            String item = rs.getString(6);
            StringBuilder sb = lessons.computeIfAbsent(id, k -> new StringBuilder(head));
            if (item != null) sb.append("\n    - ").append(item);
        });
        List<String> tasks = jdbc.queryForList(
                "SELECT COALESCE(number, '') || ' ' || summary FROM exam_task WHERE course_id = :c ORDER BY exam_task_id", c, String.class);
        String user = tasks.isEmpty()
                ? "U materijalu nema zadataka s ispita: tipove izvedi samo iz gradiva."
                : "ZADACI S ISPITA (samo da vidiš koje vrste postoje, ne kopiraj ih):\n" + String.join("\n", tasks);

        PlanSupport.CourseInfo course = plan.course(job.courseId());
        JsonNode out = plan.json(llm.chat(job, plan.planModel, SYSTEM.formatted(course.describe(), String.join("\n", lessons.values())),
                user, OwnCourseLlm.Options.of("medium", 16000)).content());
        List<Application> applications = parse(out, positions);
        transactions.executeWithoutResult(tx -> write(job.courseId(), applications));
        return applications.size() + " lekcija primjene";
    }

    // Zadržava lekcije poznatog tipa koje koriste postojeće lekcije koncepata; mjesto je iza zadnje od njih.
    static List<Application> parse(JsonNode out, Map<Long, Integer> positions) {
        List<Application> result = new ArrayList<>();
        for (JsonNode a : out.path("lekcije_primjene")) {
            String type = a.path("tip").asText("").strip().toLowerCase();
            String name = a.path("naziv").asText("").strip().replaceAll("\\s+", " ");
            if (!TYPES.contains(type) || name.isEmpty()) continue;
            List<Long> uses = new ArrayList<>();
            for (JsonNode u : a.path("koristi")) {
                long id = u.asLong(-1);
                if (positions.containsKey(id) && !uses.contains(id)) uses.add(id);
            }
            if (uses.isEmpty()) continue;
            long after = uses.stream().max(Comparator.comparingInt(positions::get)).orElseThrow();
            result.add(new Application(name.length() > 200 ? name.substring(0, 200) : name, type,
                    a.path("opis").asText("").strip(), a.path("primjer_situacije").asText("").strip(), uses, after));
            if (result.size() >= positions.size()) break; // više primjene nego koncepata nije smisleno
        }
        return result;
    }

    private void write(long courseId, List<Application> applications) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        List<Long> order = jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c ORDER BY position", c, Long.class);
        Map<Long, List<Application>> byAnchor = new HashMap<>();
        for (Application a : applications) byAnchor.computeIfAbsent(a.after(), k -> new ArrayList<>()).add(a);
        int position = 0;
        for (Long id : order) {
            jdbc.update("UPDATE course_objective SET position = :p WHERE course_id = :c AND objective_id = :o",
                    new MapSqlParameterSource("c", courseId).addValue("o", id).addValue("p", ++position));
            for (Application a : byAnchor.getOrDefault(id, List.of())) {
                Long created = jdbc.queryForObject("""
                        INSERT INTO learning_objective (subfield_id, objective_name, concept, kind, application_type, example_situation)
                        SELECT subfield_id, :n, :d, :k, :t, :e FROM learning_objective WHERE objective_id = :o
                        RETURNING objective_id""", new MapSqlParameterSource("o", id).addValue("n", a.name()).addValue("d", a.description())
                        .addValue("k", LESSON_KIND).addValue("t", a.type()).addValue("e", a.example()), Long.class);
                jdbc.update("INSERT INTO course_objective (course_id, objective_id, position) VALUES (:c, :o, :p)",
                        new MapSqlParameterSource("c", courseId).addValue("o", created).addValue("p", ++position));
                for (Long u : a.uses()) {
                    jdbc.update("INSERT INTO objective_prerequisite (objective_id, prerequisite_id) VALUES (:o, :p)",
                            new MapSqlParameterSource("o", created).addValue("p", u));
                }
            }
        }
    }

    private void removeApplications(long courseId) {
        transactions.executeWithoutResult(tx -> {
            MapSqlParameterSource c = new MapSqlParameterSource("c", courseId).addValue("k", LESSON_KIND);
            String ids = """
                    SELECT lo.objective_id FROM learning_objective lo JOIN course_objective co ON co.objective_id = lo.objective_id
                    WHERE co.course_id = :c AND lo.kind = :k""";
            List<Long> existing = jdbc.queryForList(ids, c, Long.class);
            if (existing.isEmpty()) return;
            MapSqlParameterSource o = new MapSqlParameterSource("o", existing).addValue("c", courseId);
            jdbc.update("DELETE FROM objective_prerequisite WHERE objective_id IN (:o) OR prerequisite_id IN (:o)", o);
            jdbc.update("DELETE FROM plan_warning WHERE objective_id IN (:o) OR prerequisite_id IN (:o)", o);
            jdbc.update("DELETE FROM course_objective WHERE course_id = :c AND objective_id IN (:o)", o);
            jdbc.update("DELETE FROM learning_objective WHERE objective_id IN (:o)", o);
        });
    }

    // Nove lekcije ulaze u ponovnu provjeru (redoslijed i pokrivenost ispita).
    @Override
    public void afterComplete(BuildJob job) {
        List<Long> created = jdbc.queryForList("""
                SELECT lo.objective_id FROM learning_objective lo JOIN course_objective co ON co.objective_id = lo.objective_id
                WHERE co.course_id = :c AND lo.kind = :k""",
                new MapSqlParameterSource("c", job.courseId()).addValue("k", LESSON_KIND), Long.class);
        flow.recheck(job.courseId(), job.accountId(), System.currentTimeMillis(), created);
    }
}
