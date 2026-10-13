package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// Zajedničko koracima izrade plana: modeli (postavka po koraku, može se vratiti na Sonnet),
// opis predmeta za prompt, predznanje, materijal dostupan na ispitu i čitanje JSON odgovora.
@Component
public class PlanSupport {

    final String planModel;
    final String cheapModel;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlanSupport(NamedParameterJdbcTemplate jdbc,
                       @Value("${own-courses.models.plan:openai/gpt-6.1-sol}") String planModel,
                       @Value("${own-courses.models.cheap:anthropic/claude-haiku-5.5}") String cheapModel) {
        this.jdbc = jdbc;
        this.planModel = planModel;
        this.cheapModel = cheapModel;
    }

    record CourseInfo(long courseId, String name, String subject, String level, long ownerId) {
        // npr. „fizika (srednja škola)“
        String describe() {
            String s = subject == null || subject.equals("ostalo") ? "predmet „" + name + "“" : subject;
            return s + " (" + levelLabel(level) + ")";
        }
    }

    CourseInfo course(long courseId) {
        Map<String, Object> c = jdbc.queryForMap(
                "SELECT course_name, subject, level, owner_account_id FROM course WHERE course_id = :c",
                new MapSqlParameterSource("c", courseId));
        return new CourseInfo(courseId, (String) c.get("course_name"), (String) c.get("subject"),
                (String) c.get("level"), ((Number) c.get("owner_account_id")).longValue());
    }

    static String levelLabel(String level) {
        return "os78".equals(level) ? "7.–8. razred osnovne škole" : "srednja škola";
    }

    // Zadano pretpostavljeno predznanje: samo matematika iz ranijih razreda, nikad gradivo samog predmeta.
    static String defaultAssumedKnowledge(String level) {
        return "os78".equals(level)
                ? "matematika do 6. razreda osnovne škole: računske operacije, razlomci, decimalni brojevi, postotci, jednostavne jednadžbe"
                : "osnovnoškolska matematika: računske operacije, razlomci, postotci, omjeri i proporcionalnost, potencije, "
                + "linearne jednadžbe i preoblikovanje formula, Pitagorin poučak, opseg, površina i obujam, graf linearne funkcije";
    }

    List<String> assumedKnowledge(CourseInfo course) {
        List<String> rows = jdbc.queryForList("SELECT text FROM assumed_knowledge WHERE course_id = :c ORDER BY assumed_id",
                new MapSqlParameterSource("c", course.courseId()), String.class);
        return rows.isEmpty() ? List.of(defaultAssumedKnowledge(course.level())) : rows;
    }

    // Stranice materijala na kojima se obrađuju ishodi (plan_outcome.page_ids, JSON popisi page_id), redom materijala.
    List<String> outcomePages(long courseId, Collection<String> pageIdsJson) {
        List<Long> ids = new ArrayList<>();
        for (String json : pageIdsJson) readJson(json).forEach(n -> ids.add(n.asLong()));
        if (ids.isEmpty()) return List.of();
        return jdbc.queryForList("""
                SELECT '### ' || m.original_name || ', str. ' || p.page_no || E'\\n' || p.markdown
                FROM material_page p JOIN course_material m ON m.material_id = p.material_id
                WHERE p.page_id IN (:p) AND m.course_id = :c AND p.markdown IS NOT NULL ORDER BY m.material_id, p.page_no""",
                new MapSqlParameterSource("p", ids).addValue("c", courseId), String.class);
    }

    // Stranice materijala koje učenik smije koristiti na ispitu (npr. knjižica formula), po stranicama.
    String availableOnExam(long courseId) {
        List<String> pages = jdbc.queryForList("""
                SELECT '### ' || m.original_name || ', str. ' || p.page_no || E'\\n' || COALESCE(p.markdown, '')
                FROM material_page p JOIN course_material m ON m.material_id = p.material_id
                WHERE m.course_id = :c AND m.available_on_exam
                ORDER BY m.material_id, p.page_no""", new MapSqlParameterSource("c", courseId), String.class);
        return pages.isEmpty() ? "" : "MATERIJAL DOSTUPAN NA ISPITU (učenik ga smije gledati):\n" + String.join("\n\n", pages);
    }

    JsonNode json(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) throw new IllegalStateException("Model nije vratio JSON");
        try {
            return mapper.readTree(content.substring(start, end + 1));
        } catch (Exception e) {
            throw new IllegalStateException("Model je vratio neispravan JSON: " + e.getMessage());
        }
    }

    String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    JsonNode readJson(String value) {
        try {
            return value == null ? mapper.createArrayNode() : mapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
