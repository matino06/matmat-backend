package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

// Je li zadatak preblizu zadatku s ispita iz materijala (ticket „Modeli odluke“): predfilter embeddingom
// izabere najsličnije zadatke s ispita, a model odluke (Jev, noul) presudi je li to isti zadatak s drugim brojevima.
// Embeddingi zadataka s ispita računaju se jednom i spremaju uz zadatak.
@Component
public class TooCloseCheck {

    private static final Logger log = LoggerFactory.getLogger(TooCloseCheck.class);

    static final int CANDIDATES = 3;
    static final double MIN_SIMILARITY = 0.5;
    static final double TOO_CLOSE = 0.5;
    static final String QUESTION = "preblizu";
    static final String INSTRUCTIONS = "Je li NOVI zadatak u biti ISPITNI zadatak: ista situacija i isto pitanje, samo s drugim "
            + "brojevima, imenima ili sitnicama? Odgovori „ne“ ako samo uči isto gradivo ili je isti tip zadatka u drugoj situaciji.";

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final String embeddingModel;
    private final String decideModel;
    private final ObjectMapper mapper = new ObjectMapper();

    public TooCloseCheck(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm,
                         @Value("${own-courses.models.embedding:openai/text-embedding-3-small}") String embeddingModel,
                         @Value("${own-courses.models.decide:typesafe/jev-1.13}") String decideModel) {
        this.jdbc = jdbc;
        this.llm = llm;
        this.embeddingModel = embeddingModel;
        this.decideModel = decideModel;
    }

    // Zadatak s ispita kojem je novi zadatak preblizu, ili null.
    public String tooCloseTo(BuildJob job, String task) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        List<Map<String, Object>> exam = jdbc.queryForList(
                "SELECT exam_task_id, COALESCE(number, '') AS number, summary, embedding FROM exam_task WHERE course_id = :c ORDER BY exam_task_id", c);
        if (exam.isEmpty()) return null;
        try {
            Map<Long, double[]> vectors = examVectors(job, exam);
            double[] mine = llm.embed(job, embeddingModel, List.of(task)).get(0);
            List<Map<String, Object>> nearest = nearest(exam, vectors, mine);
            for (Map<String, Object> e : nearest) {
                ObjectNode state = mapper.createObjectNode().put("ispitni", (String) e.get("summary")).put("novi", task);
                if (llm.decide(job, decideModel, state, QUESTION, INSTRUCTIONS) >= TOO_CLOSE) {
                    return (e.get("number") + " " + e.get("summary")).strip();
                }
            }
            return null;
        } catch (OwnCourseLlm.GlobalLimitException e) {
            throw e;
        } catch (RuntimeException e) {
            // provjera je zaštita, ne uvjet: ako embeddingi ili odluka ne rade, zadatak se ne odbacuje
            log.warn("Provjera „preblizu“ za predmet {} nije uspjela: {}", job.courseId(), e.getMessage());
            return null;
        }
    }

    private Map<Long, double[]> examVectors(BuildJob job, List<Map<String, Object>> exam) {
        Map<Long, double[]> out = new HashMap<>();
        List<Map<String, Object>> missing = new ArrayList<>();
        for (Map<String, Object> e : exam) {
            String json = (String) e.get("embedding");
            if (json == null) missing.add(e);
            else out.put(((Number) e.get("exam_task_id")).longValue(), parse(json));
        }
        if (!missing.isEmpty()) {
            List<double[]> vs = llm.embed(job, embeddingModel, missing.stream().map(e -> (String) e.get("summary")).toList());
            for (int i = 0; i < missing.size(); i++) {
                long id = ((Number) missing.get(i).get("exam_task_id")).longValue();
                out.put(id, vs.get(i));
                jdbc.update("UPDATE exam_task SET embedding = :e WHERE exam_task_id = :id",
                        new MapSqlParameterSource("id", id).addValue("e", Arrays.toString(vs.get(i))));
            }
        }
        return out;
    }

    static List<Map<String, Object>> nearest(List<Map<String, Object>> exam, Map<Long, double[]> vectors, double[] mine) {
        List<Map.Entry<Double, Map<String, Object>>> scored = new ArrayList<>();
        for (Map<String, Object> e : exam) {
            double s = cosine(mine, vectors.get(((Number) e.get("exam_task_id")).longValue()));
            if (s >= MIN_SIMILARITY) scored.add(Map.entry(s, e));
        }
        scored.sort(Map.Entry.<Double, Map<String, Object>>comparingByKey().reversed());
        return scored.stream().limit(CANDIDATES).map(Map.Entry::getValue).toList();
    }

    static double cosine(double[] a, double[] b) {
        if (b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / Math.sqrt(na * nb);
    }

    private double[] parse(String json) {
        try {
            return mapper.readValue(json, double[].class);
        } catch (Exception e) {
            return new double[0];
        }
    }
}
