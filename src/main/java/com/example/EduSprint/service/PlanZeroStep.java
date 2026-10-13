package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Provjera nulte pretpostavke za jednu lekciju (ticket „Kako pouzdano provjeriti nultu pretpostavku“, v3).
// Model ne presuđuje: za svaki pojam iz predmeta koji stavke lekcije koriste, a lekcija ga ne uči, navodi
// SVE stavke plana koje ga uče (ili „predznanje“ / „nigdje“). Presudu donosi PlanCheckRepairStep.
// Cijeli plan je u system poruci, isti za sve lekcije, pa se kešira (cache_control za Haiku).
@Component
public class PlanZeroStep implements BuildStep {

    public static final String KIND = "plan_zero";

    static final String SYSTEM = """
            Pomažeš provjeriti plan učenja za %s. Dobit ćeš CIJELI PLAN kao popis stavki znanja (id stavke, [lekcija], tekst) i jednu LEKCIJU.
            Zadatak: nađi pojmove IZ PREDMETA koje stavke te lekcije KORISTE kao već poznate, a lekcija ih sama ne uči.
            Za svaki takav pojam navedi id-ove SVIH stavki iz plana koje ga uče (ako ga uči više stavki, navedi sve).
            Pravila:
            - Ne navodi ono što stavke same lekcije uvode i objašnjavaju.
            - Matematika iz PREDZNANJA nije pojam iz predmeta: %s. Za nju piši "predznanje".
            - Svakodnevno znanje (vrijeme, udaljenost, brzina kao riječ iz svakodnevice) nije pojam iz predmeta.
            - Ako pojam iz predmeta ne uči nijedna stavka plana, piši "nigdje".
            - Navedi samo pojmove bez kojih se stavka stvarno ne može razumjeti, ne usputne spomene.
            - Opće znanje koje učenik ima iz svakodnevice i osnovne škole NIJE pojam koji treba prijaviti (npr. atom, molekula,
              elektron, zvijezda, planet, masa i težina kao riječi, tekućina, plin, struja i baterija kao riječi, svjetlost,
              toplo i hladno). Prijavi ga samo ako stavka traži njegovu stručnu definiciju, formulu ili model.
            - Ako stavka sama objasni pojam, nije prekršaj.
            - Konstanta ili oznaka koja je dana u materijalu dostupnom na ispitu nije prekršaj ako je stavka samo koristi u formuli.
            - Pojam koji je samo jedan od primjera u nabrajanju nije prekršaj; prijavi ga samo ako stavka bez njega gubi smisao.
            Vrati JSON {"koristi":[{"pojam":"kratko","stavka_lekcije":id stavke ove lekcije koja ga koristi,
            "uci_ga":[id stavke iz plana, …] ili ["predznanje"] ili ["nigdje"]}]} (prazan popis ako nema ničega).

            CIJELI PLAN:
            %s""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanZeroStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
        JsonNode in = mapper.readTree(job.input());
        long objectiveId = in.path("objectiveId").asLong();
        int run = in.path("run").asInt(1);
        PlanSupport.CourseInfo course = plan.course(job.courseId());

        List<String> all = jdbc.queryForList("""
                SELECT k.item_id || ' [' || co.position || '] ' || k.text
                FROM knowledge_item k JOIN course_objective co ON co.objective_id = k.objective_id AND co.course_id = :c
                WHERE k.duplicate_of IS NULL ORDER BY co.position, k.position""",
                new MapSqlParameterSource("c", job.courseId()), String.class);
        String system = SYSTEM.formatted(course.describe(), String.join("; ", plan.assumedKnowledge(course)), String.join("\n", all));

        String name = jdbc.queryForObject("SELECT objective_name FROM learning_objective WHERE objective_id = :o",
                new MapSqlParameterSource("o", objectiveId), String.class);
        List<String> own = jdbc.queryForList("""
                SELECT item_id || ' ' || text FROM knowledge_item WHERE objective_id = :o AND duplicate_of IS NULL ORDER BY position""",
                new MapSqlParameterSource("o", objectiveId), String.class);
        if (own.isEmpty()) return "lekcija bez stavki"; // lekcija primjene ili stavke još nisu izrađene
        String user = "LEKCIJA: " + name + "\nSTAVKE LEKCIJE:\n" + String.join("\n", own);

        JsonNode out = plan.json(llm.chat(job, plan.cheapModel, system, user, OwnCourseLlm.Options.of("low", 4000)).content());
        MapSqlParameterSource key = new MapSqlParameterSource("o", objectiveId).addValue("r", run);
        if (in.path("round").asLong(0) > 0 || in.path("repair").asInt(0) > 0) {
            jdbc.update("DELETE FROM zero_finding WHERE objective_id = :o", key); // lekcija se promijenila: stari nalazi ne vrijede
        } else {
            jdbc.update("DELETE FROM zero_finding WHERE objective_id = :o AND run = :r", key);
        }
        int n = 0;
        for (JsonNode f : out.path("koristi")) {
            String concept = f.path("pojam").asText("").strip();
            if (concept.isEmpty()) continue;
            JsonNode taught = f.path("uci_ga");
            jdbc.update("""
                    INSERT INTO zero_finding (course_id, objective_id, run, concept, item_id, taught_by)
                    VALUES (:c, :o, :r, :k, :i, :t)""",
                    new MapSqlParameterSource(Map.of("c", job.courseId(), "o", objectiveId, "r", run, "k", concept,
                            "t", plan.toJson(taught.isArray() ? taught : mapper.createArrayNode().add(taught.asText()))))
                            .addValue("i", f.path("stavka_lekcije").canConvertToLong() ? f.path("stavka_lekcije").asLong() : null));
            n++;
        }
        return n + " pojmova";
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterChecks(job);
    }
}
