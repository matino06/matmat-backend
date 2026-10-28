package com.example.EduSprint.service;

import com.example.EduSprint.dto.OfferedCourseDTO;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The official courses the apps offer (sql/2026-10-28-predmeti-iz-baze.sql) and those a student chose to study
 * (sql/2026-10-19-odabrani-predmeti.sql); the course switcher lists only the chosen ones. An unpublished course is
 * offered to admins only. Plain JDBC, like AiConsentService: the Account entity and {@code ddl-auto=validate} are left alone.
 */
@Service
public class ChosenCoursesService {

    static final int MAX_COURSES = 20;

    private final NamedParameterJdbcTemplate jdbc;

    public ChosenCoursesService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The official courses in menu order: the published ones, and for an admin the unpublished ones too. */
    public List<OfferedCourseDTO> offered(boolean admin) {
        return jdbc.query("""
                SELECT course_id, course_name, subject, short_name, matura_level, level_hint, badge, hue, tagline,
                       formula_pdf, published
                FROM course WHERE owner_account_id IS NULL AND (published OR :admin)
                ORDER BY position NULLS LAST, course_id""", new MapSqlParameterSource("admin", admin), (rs, i) ->
                new OfferedCourseDTO(rs.getLong("course_id"), rs.getString("course_name"), rs.getString("subject"),
                        rs.getString("short_name"), rs.getString("matura_level"), rs.getString("level_hint"),
                        rs.getString("badge"), rs.getObject("hue", Integer.class), rs.getString("tagline"),
                        rs.getString("formula_pdf"), rs.getBoolean("published")));
    }

    /** Whether the account may study an official course: it is published, or the account is an admin. */
    public boolean isOffered(long courseId, boolean admin) {
        return admin || Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM course WHERE course_id = :id AND owner_account_id IS NULL AND published)",
                new MapSqlParameterSource("id", courseId), Boolean.class));
    }

    /**
     * The chosen courses in the order they were saved. An account that never chose (every account before this
     * feature) gets its current course, if that is an official one, so its switcher isn't empty.
     */
    public List<Long> get(long accountId) {
        try {
            return jdbc.queryForObject("""
                    SELECT a.chosen_courses, c.course_id FROM account a
                    LEFT JOIN course c ON c.course_id = a.current_course_id AND c.owner_account_id IS NULL
                    WHERE a.account_id = :id""", new MapSqlParameterSource("id", accountId), (rs, i) -> {
                Array chosen = rs.getArray(1);
                if (chosen != null) return Arrays.asList((Long[]) chosen.getArray());
                long current = rs.getLong(2);
                return rs.wasNull() ? List.<Long>of() : List.of(current);
            });
        } catch (DataAccessException e) {
            return List.of();
        }
    }

    /** Replaces the choice: at least one course, each an official course offered to the account, no repeats. */
    public List<Long> set(long accountId, List<Long> courseIds, boolean admin) {
        if (courseIds == null || courseIds.isEmpty() || courseIds.size() > MAX_COURSES
                || courseIds.stream().anyMatch(Objects::isNull) || courseIds.stream().distinct().count() != courseIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neispravan popis predmeta");
        }
        Integer official = jdbc.queryForObject(
                "SELECT count(*) FROM course WHERE course_id IN (:ids) AND owner_account_id IS NULL AND (published OR :admin)",
                new MapSqlParameterSource("ids", courseIds).addValue("admin", admin), Integer.class);
        if (official == null || official != courseIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nepoznat predmet");
        }
        jdbc.update("UPDATE account SET chosen_courses = :ids WHERE account_id = :id",
                new MapSqlParameterSource()
                        .addValue("ids", courseIds.toArray(Long[]::new))
                        .addValue("id", accountId));
        return List.copyOf(courseIds);
    }
}
