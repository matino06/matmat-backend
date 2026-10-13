package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

// Admin pregled vlastitih predmeta: predmeti s kreatorom, stanjem i troškom, zapisnik izrade po predmetu,
// te kreatori s mjesečnom kvotom i potrošnjom. Trošak dolazi iz build_cost (stvarni usage.cost poziva).
@Service
public class AdminOwnCourseService {

    static final int EVENT_LIMIT = 1000;

    private final NamedParameterJdbcTemplate jdbc;

    public AdminOwnCourseService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> courses() {
        return jdbc.queryForList("""
                SELECT c.course_id AS "courseId", c.course_name AS "courseName", c.subject, c.status,
                       c.learning_mode AS "learningMode", a.account_id AS "accountId", a.email, a.name,
                       COALESCE((SELECT sum(b.cost_usd) FROM build_cost b WHERE b.course_id = c.course_id), 0) AS "costUsd",
                       (SELECT count(*) FROM course_objective co WHERE co.course_id = c.course_id) AS "lessonCount",
                       (SELECT count(*) FROM task t JOIN course_objective co ON co.objective_id = t.objective_id
                        WHERE co.course_id = c.course_id) AS "taskCount",
                       (SELECT count(*) FROM build_job j WHERE j.course_id = c.course_id AND j.status = 'failed') AS "failedJobs"
                FROM course c JOIN account a ON a.account_id = c.owner_account_id
                ORDER BY c.course_id DESC""", new MapSqlParameterSource());
    }

    // Zapisnik izrade: svaki korak s vremenima, pokušajima, greškom i troškom, najnoviji prvi.
    public List<Map<String, Object>> events(long courseId) {
        return jdbc.queryForList("""
                SELECT j.job_id AS "jobId", j.kind, j.ref, j.status, j.attempts, j.error, j.priority,
                       j.counts_quota AS "countsQuota", j.created_at AS "createdAt", j.started_at AS "startedAt",
                       j.finished_at AS "finishedAt",
                       COALESCE((SELECT sum(b.cost_usd) FROM build_cost b WHERE b.job_id = j.job_id), 0) AS "costUsd"
                FROM build_job j WHERE j.course_id = :c ORDER BY j.job_id DESC LIMIT :n""",
                new MapSqlParameterSource("c", courseId).addValue("n", EVENT_LIMIT));
    }

    // Kreatori (i računi koji imaju vlastite predmete): kvota, potrošnja ovog mjeseca (ona koja se broji
    // u kvotu i sva) i ukupno.
    public List<Map<String, Object>> creators() {
        return jdbc.queryForList("""
                SELECT a.account_id AS "accountId", a.email, a.name, a.is_creator AS "isCreator",
                       a.monthly_quota_usd AS "monthlyQuotaUsd",
                       COALESCE(sum(b.cost_usd) FILTER (WHERE b.created_at >= :from AND b.counts_quota), 0) AS "quotaCostMonth",
                       COALESCE(sum(b.cost_usd) FILTER (WHERE b.created_at >= :from), 0) AS "costMonth",
                       COALESCE(sum(b.cost_usd), 0) AS "costTotal",
                       (SELECT count(*) FROM course c WHERE c.owner_account_id = a.account_id) AS "courseCount"
                FROM account a LEFT JOIN build_cost b ON b.account_id = a.account_id
                WHERE a.is_creator OR EXISTS (SELECT 1 FROM course c WHERE c.owner_account_id = a.account_id)
                GROUP BY a.account_id ORDER BY "costTotal" DESC, a.email""",
                new MapSqlParameterSource("from", Timestamp.from(BuildQuotaService.monthStart(AppTime.today()))));
    }

    // Uključi ili isključi kreatora i postavi mu mjesečnu kvotu (po e-mailu računa).
    public void updateCreator(String email, Boolean isCreator, BigDecimal monthlyQuotaUsd) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upiši e-mail računa.");
        if (monthlyQuotaUsd != null && (monthlyQuotaUsd.signum() < 0 || monthlyQuotaUsd.compareTo(new BigDecimal("1000")) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kvota mora biti između 0 i 1000 $.");
        }
        MapSqlParameterSource p = new MapSqlParameterSource("e", email.strip()).addValue("c", isCreator).addValue("q", monthlyQuotaUsd);
        int n = jdbc.update("""
                UPDATE account SET is_creator = COALESCE(:c, is_creator), monthly_quota_usd = COALESCE(:q, monthly_quota_usd)
                WHERE lower(email) = lower(:e)""", p);
        if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Račun s tim e-mailom ne postoji.");
    }
}
