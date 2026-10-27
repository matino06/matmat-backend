package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.example.EduSprint.storage.MaterialStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

// Čitanje jedne stranice materijala: slika stranice (uz tekstni sloj PDF-a kao pomoć) ide vision modelu,
// koji vraća Markdown s LaTeX formulama i vrstu stranice. Vrsta se određuje u istom pozivu, umjesto
// zasebnog poziva modela odluke, jer je to jeftinije.
@Component
public class ReadPageStep implements BuildStep {

    public static final String KIND = "read_page";
    static final String MODEL = "google/gemini-2.5-flash-lite";
    static final Set<String> KINDS = Set.of("kurikulum", "ispit", "rjesenja", "skripta", "ostalo");

    static final String SYSTEM = """
            Prepisuješ jednu stranicu nastavnog materijala (skripta, kurikulum ili katalog, ispit, rješenja) točno kako piše, na hrvatskom.
            - Formule piši u LaTeX-u: \\( … \\) u retku, $$ … $$ za izdvojene. Tablice kao Markdown.
            - Za svaku sliku, graf ili shemu napiši [SLIKA: kratak opis što prikazuje, s brojevima i oznakama].
            - NIKAD ne rješavaj zadatke, ne dodaji objašnjenja, rješenja ni poveznice kojih na stranici nema. Samo prepiši.
            - Ako je dan tekstni sloj PDF-a, koristi ga za točne hrvatske znakove i riječi, a sliku za formule, tablice i raspored.
            Odredi i vrstu stranice:
            kurikulum (katalog, kurikulum, popis ishoda ili gradiva), ispit (zadaci ispita, testa ili provjere znanja),
            rjesenja (ključ ili rješenja), skripta (objašnjenje gradiva, primjeri i zadatci za vježbu iz udžbenika ili skripte),
            ostalo (naslovnica, upute, prazna stranica).
            Vrati JSON {"markdown":"…","vrsta":"kurikulum|ispit|rjesenja|skripta|ostalo"}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final MaterialStorage storage;
    private final OwnCourseLlm llm;
    private final BuildJobQueue queue;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReadPageStep(NamedParameterJdbcTemplate jdbc, MaterialStorage storage, OwnCourseLlm llm, BuildJobQueue queue) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.llm = llm;
        this.queue = queue;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(BuildJob job) throws Exception {
        long pageId = mapper.readTree(job.input()).path("pageId").asLong();
        Map<String, Object> page = jdbc.queryForMap("SELECT image_key, text_layer FROM material_page WHERE page_id = :p",
                new MapSqlParameterSource("p", pageId));
        String imageKey = (String) page.get("image_key");
        String textLayer = (String) page.get("text_layer");
        byte[] image = storage.bytes(imageKey);
        String user = textLayer == null ? "Prepiši ovu stranicu." : "Prepiši ovu stranicu.\n\nTEKSTNI SLOJ PDF-a:\n" + textLayer;
        OwnCourseLlm.Result result = llm.chatWithImage(job, MODEL, SYSTEM, user, image, mimeType(imageKey),
                OwnCourseLlm.Options.of(null, 8000));
        JsonNode out = parse(result.content());
        String markdown = out.path("markdown").asText("").strip();
        String kind = normaliseKind(out.path("vrsta").asText(""));
        jdbc.update("UPDATE material_page SET markdown = :md, kind = :k WHERE page_id = :p",
                new MapSqlParameterSource("p", pageId).addValue("md", markdown).addValue("k", kind));
        return kind;
    }

    // Kad je pročitana zadnja stranica predmeta, kreće izrada plana.
    @Override
    public void afterComplete(BuildJob job) {
        if (queue.notDone(job.courseId(), SplitMaterialStep.KIND, KIND) == 0) {
            queue.enqueue(job.courseId(), job.accountId(), PlanScopeStep.KIND, "opseg",
                    BuildJobQueue.PRIORITY_BACKGROUND, true, null);
        }
    }

    JsonNode parse(String content) throws Exception {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) throw new IllegalStateException("Model nije vratio JSON");
        // model prepisuje formule u LaTeX-u, a backslash u JSON-u ne udvostruči uvijek
        return mapper.readTree(JsonLatex.fix(content.substring(start, end + 1)));
    }

    static String normaliseKind(String value) {
        String v = value == null ? "" : value.strip().toLowerCase().replace("š", "s").replace("ješ", "jes");
        return KINDS.contains(v) ? v : "ostalo";
    }

    static String mimeType(String key) {
        String k = key.toLowerCase();
        return k.endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
