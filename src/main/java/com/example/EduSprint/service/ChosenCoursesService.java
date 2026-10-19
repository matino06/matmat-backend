package com.example.EduSprint.service;

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
 * The official courses a student chose to study (sql/2026-10-19-odabrani-predmeti.sql); the web's course switcher
 * lists only these. Plain JDBC, like AiConsentService: the Account entity and {@code ddl-auto=validate} are left alone.
 */
@Service
public class ChosenCoursesService {

    static final int MAX_COURSES = 20;

    private final NamedParameterJdbcTemplate jdbc;

    public ChosenCoursesService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    /** Replaces the choice: at least one course, each an official course, no repeats. */
    public List<Long> set(long accountId, List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty() || courseIds.size() > MAX_COURSES
                || courseIds.stream().anyMatch(Objects::isNull) || courseIds.stream().distinct().count() != courseIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neispravan popis predmeta");
        }
        Integer official = jdbc.queryForObject(
                "SELECT count(*) FROM course WHERE course_id IN (:ids) AND owner_account_id IS NULL",
                new MapSqlParameterSource("ids", courseIds), Integer.class);
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
