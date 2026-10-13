package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Task;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Varijacije 2–4 vlastitog predmeta nastaju postupno: kad učenik riješi zadatak lekcije, izrađuje se sljedeća
// varijacija (posao s prioritetom, ne troši kvotu). Učenik nikad ne ostaje bez zadatka: dok nova nije gotova,
// rotacija (findTaskFromObjective) daje neku od postojećih.
@Service
public class OwnCourseVariationService {

    // Stanje sljedeće varijacije za zadatak koji učenik upravo dobiva.
    public static final String NONE = "none";       // ništa ne treba čekati
    public static final String PENDING = "pending"; // nova varijacija se izrađuje; kratko čekanje
    public static final String READY = "ready";     // postoji varijacija koju učenik još nije riješio
    public static final String FAILED = "failed";   // izrada je pala; ponovno je pokrenuta, učenik dobiva prethodnu

    private final NamedParameterJdbcTemplate jdbc;
    private final BuildJobQueue queue;
    private final TaskService tasks;

    public OwnCourseVariationService(NamedParameterJdbcTemplate jdbc, BuildJobQueue queue, TaskService tasks) {
        this.jdbc = jdbc;
        this.queue = queue;
        this.tasks = tasks;
    }

    record Lesson(long courseId, long ownerId, long objectiveId, int variation) {
    }

    // Zadatak vlastitog predmeta s varijacijom, ili null za službene zadatke.
    Lesson lesson(long taskId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT c.course_id, c.owner_account_id, t.objective_id, t.variation FROM task t
                JOIN course_objective co ON co.objective_id = t.objective_id JOIN course c ON c.course_id = co.course_id
                WHERE t.task_id = :t AND c.owner_account_id IS NOT NULL AND t.variation IS NOT NULL""",
                new MapSqlParameterSource("t", taskId));
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        return new Lesson(((Number) r.get("course_id")).longValue(), ((Number) r.get("owner_account_id")).longValue(),
                ((Number) r.get("objective_id")).longValue(), ((Number) r.get("variation")).intValue());
    }

    // Poziva se nakon spremanja riješenog zadatka.
    public void onSolved(long taskId) {
        Lesson l = lesson(taskId);
        if (l != null) nextJobStatus(l);
    }

    public String nextVariation(Account account, long taskId) {
        Lesson l = lesson(taskId);
        if (l == null) return NONE;
        MapSqlParameterSource p = new MapSqlParameterSource("o", l.objectiveId()).addValue("a", account.getAccountId()).addValue("t", taskId);
        Integer solvedThis = jdbc.queryForObject("SELECT count(*) FROM solved_task WHERE account_id = :a AND task_id = :t", p, Integer.class);
        if (solvedThis == null || solvedThis == 0) return NONE; // novi zadatak za učenika, nema što čekati
        Integer unsolved = jdbc.queryForObject("""
                SELECT count(*) FROM task t WHERE t.objective_id = :o AND t.variation IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM solved_task s WHERE s.task_id = t.task_id AND s.account_id = :a)""", p, Integer.class);
        if (unsolved != null && unsolved > 0) return READY;
        String status = nextJobStatus(l);
        if (status == null) return NONE;
        return BuildJobQueue.FAILED.equals(status) ? FAILED : PENDING;
    }

    // Učenik je čekao novu varijaciju: trenutni zadatak se ponovno bira (rotacija sada uključuje novu).
    public Task refreshCurrent(Account account) {
        Task current = account.getCurrentTask();
        if (current == null || lesson(current.getId()) == null) return current;
        Task next = tasks.getTaskFromObjective(account, current.getObjective().getObjectiveId());
        return next == null ? current : next;
    }

    // Osigurava posao za sljedeću nedostajuću varijaciju; vraća njegov status prije ponovnog pokretanja
    // (null ako su sve 4 već napravljene). Neuspjeli posao kreće ispočetka.
    private String nextJobStatus(Lesson l) {
        MapSqlParameterSource o = new MapSqlParameterSource("o", l.objectiveId());
        Integer max = jdbc.queryForObject("SELECT COALESCE(max(variation), 0) FROM task WHERE objective_id = :o", o, Integer.class);
        int next = (max == null ? 0 : max) + 1;
        if (next > TaskVariationStep.MAX_VARIATIONS || next < 2) return null;
        String ref = "v" + next + " " + l.objectiveId();
        queue.enqueue(l.courseId(), l.ownerId(), TaskVariationStep.KIND, ref, BuildJobQueue.PRIORITY_WAITED_ON, false,
                TaskVariationStep.input(l.objectiveId(), next));
        MapSqlParameterSource j = new MapSqlParameterSource("c", l.courseId()).addValue("k", TaskVariationStep.KIND).addValue("r", ref);
        String status = jdbc.queryForObject("SELECT status FROM build_job WHERE course_id = :c AND kind = :k AND ref = :r", j, String.class);
        if (BuildJobQueue.FAILED.equals(status)) {
            jdbc.update("""
                    UPDATE build_job SET status = 'waiting', attempts = 0, error = NULL, next_attempt_at = now(), finished_at = NULL
                    WHERE course_id = :c AND kind = :k AND ref = :r AND status = 'failed'""", j);
        }
        return status;
    }
}
