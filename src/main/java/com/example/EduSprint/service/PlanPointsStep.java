package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Stavke znanja jednog ishoda. Zajednički dio (pravila, svi ishodi, zadaci s ispita, materijal dostupan
// na ispitu) je u system poruci, isti za sve ishode, pa ga model kešira; ishod koji se mijenja je u user poruci.
@Component
public class PlanPointsStep implements BuildStep {

    public static final String KIND = "plan_points";
    static final Set<String> KINDS = Set.of("formula", "definicija", "pojava", "postupak", "zamka");

    static final String RULES = """
            Izvlačiš STAVKE ZNANJA za jedan ishod iz %s.
            Stavka znanja je najsitnija stvar koju učenik mora znati ili umjeti: formula, definicija veličine, pojava,
            postupak ili tipična zamka. Stavka obuhvaća jednu veličinu ili zakon ZAJEDNO s njegovom definicijom,
            formulom, jedinicom i tipičnom zamkom kad idu zajedno, npr. „put i pomak: put je ukupna duljina putanje (skalar),
            pomak vektor od početnog do konačnog položaja; put je jednak iznosu pomaka samo ako tijelo ne mijenja smjer“.
            Ne rastavljaj definiciju, jedinicu, formulu i zamku iste veličine na zasebne stavke. Jedan ishod obično ima 10–25 stavki.
            Stavka NIJE: isti zakon u drugom kontekstu, obrat iste formule, isti zadatak s drugim brojevima.
            Riješeni primjer ili zadatak iz materijala nije stavka: iz njega uzmi pravilo ili postupak koji pokazuje
            (npr. „prije računanja sve veličine svedi na istu jedinicu“), a sama situacija (svjetlosna godina, zrnca pijeska,
            slavina koja kaplje) ostaje primjer i ne ulazi u stavku kao zaseban sadržaj.
            Sadržaj koji materijal sam označi kao dodatni (proširenje, zanimljivost, povijesna crtica, „za one koji žele znati
            više“) nije stavka, osim ako ga traže pitanja ili zadatci u materijalu. Popis ključnih pojmova ili sažetak nije
            zasebna stavka: njegovi pojmovi već su u stavkama. Kod pokusa ili mjerenja stavka je ono što pokus pokazuje
            (pojava, reakcija, zaključak), a ne opis kako se izvodi.
            Predznanje (nije stavka): %s. Sve iz samog predmeta je stavka.
            Konstante i tablične vrijednosti iz materijala dostupnog na ispitu NISU stavke (učenik ih tamo pronađe),
            ali značenje veličine i kada se formula koristi jesu.
            Uzmi samo stavke koje pripadaju OVOM ishodu (ostali imaju svoje pozive). Uključi sve što traži razrada
            i sve što zadaci s ispita iz ovog područja zahtijevaju. Poredaj ih redom učenja.
            Kad je uz ishod priloženo GRADIVO IZ MATERIJALA (skripta, udžbenik, bilješke), stavke izvlači samo iz njega:
            ništa ne dodaji iz svojeg znanja (pojam, formulu, primjer, popis, iznimku), čak ni kad bi to bilo korisno. Što
            gradivo samo spominje, stavka samo spominje: ako piše „SI ima sedam osnovnih jedinica“, ne nabrajaj ih. Cilj je
            da učenik nauči točno ono što je u materijalu, jer to dolazi na test, i ništa više.
            Ispitni katalog ili kurikulum nije takvo gradivo: navodi samo što učenik mora umjeti („izračunati pH otopine“),
            a ne i formule i definicije s kojima to radi. Kad je to jedino gradivo ishoda, svaku vještinu iz razrade pretvori
            u stavke sa svime što učenik za nju treba (formula s jedinicama, definicija, postupak, tipična zamka), na razini
            na kojoj se uči u školi i kakvu traže zadaci s ispita; teme izvan razrade ne dodaji.
            Vrati JSON {"stavke":[{"tekst":"…","vrsta":"formula|definicija|pojava|postupak|zamka"}]}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final PlanFlow flow;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanPointsStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan, PlanFlow flow) {
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
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());

        List<String> outcomes = jdbc.queryForList(
                "SELECT '- ' || COALESCE(NULLIF(code, '') || ': ', '') || title FROM plan_outcome WHERE course_id = :c ORDER BY position",
                c, String.class);
        List<String> tasks = jdbc.queryForList(
                "SELECT '- [' || COALESCE(number, '') || '] ' || summary || ' | treba: ' || COALESCE(needed, '') FROM exam_task WHERE course_id = :c ORDER BY exam_task_id",
                c, String.class);
        String system = RULES.formatted(course.describe(), String.join("; ", plan.assumedKnowledge(course)))
                + "\n\nSVI ISHODI (da znaš granice):\n" + String.join("\n", outcomes)
                + (tasks.isEmpty() ? "" : "\n\nZADACI S ISPITA IZ MATERIJALA:\n" + String.join("\n", tasks))
                + "\n\n" + plan.availableOnExam(job.courseId());

        Map<String, Object> outcome = jdbc.queryForMap("SELECT code, title, details, page_ids FROM plan_outcome WHERE outcome_id = :o",
                new MapSqlParameterSource("o", outcomeId));
        String pagesJson = (String) outcome.remove("page_ids");
        String user = "OVAJ ISHOD:\n" + plan.toJson(outcome);
        List<String> pages = plan.outcomePages(job.courseId(), Collections.singletonList(pagesJson));
        if (!pages.isEmpty()) {
            boolean curriculum = plan.onlyCurriculum(job.courseId(), Collections.singletonList(pagesJson));
            user += "\n\nGRADIVO OVOG ISHODA IZ MATERIJALA" + (curriculum ? " (samo ispitni katalog ili kurikulum)" : "") + ":\n"
                    + String.join("\n\n", pages);
        }

        JsonNode out = plan.json(llm.chat(job, plan.planModel, system, user, OwnCourseLlm.Options.of("medium", 12000)).content());
        jdbc.update("DELETE FROM knowledge_item WHERE outcome_id = :o", new MapSqlParameterSource("o", outcomeId));
        int position = 0;
        for (JsonNode s : out.path("stavke")) {
            String text = s.path("tekst").asText("").strip();
            if (text.isEmpty()) continue;
            String kind = s.path("vrsta").asText("").strip().toLowerCase();
            jdbc.update("""
                    INSERT INTO knowledge_item (course_id, outcome_id, position, text, kind) VALUES (:c, :o, :p, :t, :k)""",
                    new MapSqlParameterSource(Map.of("c", job.courseId(), "o", outcomeId, "p", ++position, "t", text))
                            .addValue("k", KINDS.contains(kind) ? kind : null));
        }
        if (position == 0) throw new IllegalStateException("Model nije vratio nijednu stavku.");
        return position + " stavki";
    }

    @Override
    public void afterComplete(BuildJob job) {
        flow.afterPoints(job);
    }
}
