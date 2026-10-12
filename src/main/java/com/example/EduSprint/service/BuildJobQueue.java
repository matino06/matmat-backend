package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Red poslova izrade vlastitih predmeta u Postgresu (build_job). Posao uzima jedna instanca
// (FOR UPDATE SKIP LOCKED) i drži ga „najmom“; ako instanca padne ili se redeploya, najam istekne
// i posao se vraća u red, pa izrada nastavlja od nedovršenog koraka.
@Service
public class BuildJobQueue {

    public static final String WAITING = "waiting";
    public static final String RUNNING = "running";
    public static final String DONE = "done";
    public static final String FAILED = "failed";
    public static final String WAITING_QUOTA = "waiting_quota";
    public static final String PAUSED = "paused";

    public static final int PRIORITY_WAITED_ON = 0;
    public static final int PRIORITY_BACKGROUND = 1;

    static final int MAX_ATTEMPTS = 3;
    static final Duration LEASE = Duration.ofMinutes(10);

    private final NamedParameterJdbcTemplate jdbc;

    public BuildJobQueue(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long enqueue(long courseId, long accountId, String kind, String ref, int priority,
                        boolean countsQuota, String input) {
        return jdbc.queryForObject("""
                INSERT INTO build_job (course_id, account_id, kind, ref, priority, counts_quota, input)
                VALUES (:c, :a, :k, :r, :p, :q, :i) RETURNING job_id""",
                new MapSqlParameterSource(Map.of("c", courseId, "a", accountId, "k", kind, "p", priority, "q", countsQuota))
                        .addValue("r", ref).addValue("i", input), Long.class);
    }

    // Uzima sljedeći posao: prvo one koje netko čeka (prioritet 0), pa redom kako su nastali.
    @Transactional
    public BuildJob claimNext() {
        List<BuildJob> jobs = jdbc.query("""
                UPDATE build_job SET status = 'running', attempts = attempts + 1, started_at = now(),
                       lease_until = now() + (:lease * interval '1 second')
                WHERE job_id = (SELECT job_id FROM build_job
                                WHERE status = 'waiting' AND next_attempt_at <= now()
                                ORDER BY priority, job_id
                                FOR UPDATE SKIP LOCKED LIMIT 1)
                RETURNING job_id, course_id, account_id, kind, ref, priority, counts_quota, attempts, input""",
                new MapSqlParameterSource("lease", LEASE.toSeconds()),
                (rs, i) -> new BuildJob(rs.getLong("job_id"), rs.getLong("course_id"), rs.getLong("account_id"),
                        rs.getString("kind"), rs.getString("ref"), rs.getInt("priority"),
                        rs.getBoolean("counts_quota"), rs.getInt("attempts"), rs.getString("input")));
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    public void complete(BuildJob job, String output) {
        jdbc.update("""
                UPDATE build_job SET status = 'done', output = :o, error = NULL, finished_at = now(), lease_until = NULL
                WHERE job_id = :id""", new MapSqlParameterSource("id", job.jobId()).addValue("o", output));
    }

    // Neuspjeh: do MAX_ATTEMPTS pokušaja posao se vraća u red s razmakom, zatim ostaje „neuspjelo“.
    public void fail(BuildJob job, String error) {
        String status = statusAfterFailure(job.attempts());
        jdbc.update("""
                UPDATE build_job SET status = :s, error = :e, lease_until = NULL,
                       next_attempt_at = now() + (:delay * interval '1 second'),
                       finished_at = CASE WHEN :s = 'failed' THEN now() END
                WHERE job_id = :id""",
                new MapSqlParameterSource("id", job.jobId()).addValue("s", status)
                        .addValue("e", truncate(error)).addValue("delay", backoff(job.attempts()).toSeconds()));
    }

    // Kvota je potrošena: korak se ne izvodi i ne broji kao pokušaj; nastavlja se kad se kvota obnovi.
    public void markWaitingQuota(BuildJob job) {
        jdbc.update("""
                UPDATE build_job SET status = 'waiting_quota', attempts = attempts - 1, lease_until = NULL
                WHERE job_id = :id""", new MapSqlParameterSource("id", job.jobId()));
    }

    // Globalni limit ključa: isto kao kvota, ali za sve kreatore; vraća se u red periodično.
    public void markPaused(BuildJob job) {
        jdbc.update("""
                UPDATE build_job SET status = 'paused', attempts = attempts - 1, lease_until = NULL
                WHERE job_id = :id""", new MapSqlParameterSource("id", job.jobId()));
    }

    // Posao u radu čiji je najam istekao (instanca je pala ili redeployana) vraća se u red.
    public int recoverExpiredLeases() {
        return jdbc.update("""
                UPDATE build_job SET status = 'waiting', lease_until = NULL, attempts = GREATEST(attempts - 1, 0)
                WHERE status = 'running' AND lease_until < now()""", new MapSqlParameterSource());
    }

    public List<Long> accountsWaitingForQuota() {
        return jdbc.queryForList("SELECT DISTINCT account_id FROM build_job WHERE status = 'waiting_quota'",
                new MapSqlParameterSource(), Long.class);
    }

    public int resumeWaitingQuota(long accountId) {
        return jdbc.update("UPDATE build_job SET status = 'waiting' WHERE status = 'waiting_quota' AND account_id = :a",
                new MapSqlParameterSource("a", accountId));
    }

    public int resumePaused() {
        return jdbc.update("UPDATE build_job SET status = 'waiting' WHERE status = 'paused'", new MapSqlParameterSource());
    }

    // Ručni ponovni pokušaj kreatora: svi neuspjeli koraci predmeta kreću ispočetka.
    public int retryFailed(long courseId) {
        return jdbc.update("""
                UPDATE build_job SET status = 'waiting', attempts = 0, error = NULL, next_attempt_at = now(), finished_at = NULL
                WHERE status = 'failed' AND course_id = :c""", new MapSqlParameterSource("c", courseId));
    }

    public void recordCost(BuildJob job, String model, BigDecimal costUsd) {
        jdbc.update("""
                INSERT INTO build_cost (account_id, course_id, job_id, model, cost_usd, counts_quota)
                VALUES (:a, :c, :j, :m, :cost, :q)""",
                new MapSqlParameterSource(Map.of("a", job.accountId(), "c", job.courseId(), "j", job.jobId(),
                        "cost", costUsd, "q", job.countsQuota())).addValue("m", model));
    }

    static String statusAfterFailure(int attempts) {
        return attempts >= MAX_ATTEMPTS ? FAILED : WAITING;
    }

    // 30 s nakon prvog neuspjeha, 60 s nakon drugog.
    static Duration backoff(int attempts) {
        return Duration.ofSeconds(30L << Math.max(0, Math.min(attempts - 1, 4)));
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
