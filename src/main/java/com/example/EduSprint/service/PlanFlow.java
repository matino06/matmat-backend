package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// Redoslijed faza izrade plana. Svaka faza ima više koraka (po ishodu, po seriji stavki); iduća faza
// se dodaje tek kad je cijela prethodna gotova. Ponovljeno dodavanje je bezopasno (jedinstveni ref).
//
// opseg (+ zadaci s ispita) → stavke po ishodu → duplikati → preduvjeti po seriji → lekcije po ishodu → završetak
@Component
public class PlanFlow {

    static final int EDGE_BATCH = 40;

    private final BuildJobQueue queue;
    private final NamedParameterJdbcTemplate jdbc;

    public PlanFlow(BuildJobQueue queue, NamedParameterJdbcTemplate jdbc) {
        this.queue = queue;
        this.jdbc = jdbc;
    }

    void afterScope(BuildJob job) {
        if (queue.notDone(job.courseId(), PlanScopeStep.KIND, PlanExamTasksStep.KIND) > 0) return;
        for (Long outcomeId : outcomes(job.courseId())) {
            add(job, PlanPointsStep.KIND, "ishod " + outcomeId, "{\"outcomeId\":" + outcomeId + "}");
        }
    }

    void afterPoints(BuildJob job) {
        if (queue.notDone(job.courseId(), PlanPointsStep.KIND) > 0) return;
        add(job, PlanDedupeStep.KIND, "duplikati", null);
    }

    void afterDedupe(BuildJob job) {
        List<Long> items = jdbc.queryForList("""
                SELECT item_id FROM knowledge_item WHERE course_id = :c AND duplicate_of IS NULL ORDER BY item_id""",
                new MapSqlParameterSource("c", job.courseId()), Long.class);
        for (int i = 0; i < items.size(); i += EDGE_BATCH) {
            List<Long> batch = items.subList(i, Math.min(items.size(), i + EDGE_BATCH));
            add(job, PlanEdgesStep.KIND, "serija " + (i / EDGE_BATCH + 1), "{\"itemIds\":" + batch + "}");
        }
    }

    void afterEdges(BuildJob job) {
        if (queue.notDone(job.courseId(), PlanEdgesStep.KIND) > 0) return;
        for (Long outcomeId : outcomes(job.courseId())) {
            add(job, PlanGroupStep.KIND, "lekcije " + outcomeId, "{\"outcomeId\":" + outcomeId + "}");
        }
    }

    void afterGroup(BuildJob job) {
        if (queue.notDone(job.courseId(), PlanGroupStep.KIND) > 0) return;
        add(job, PlanFinalizeStep.KIND, "plan", null);
    }

    static final int ZERO_RUNS = 2;
    static final int COVERAGE_BATCH = 25;

    // Nulta pretpostavka po lekciji, dvaput (jedno pokretanje nađe samo dio prekršaja), i pokrivenost ispita.
    void afterFinalize(BuildJob job) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", job.courseId());
        List<Long> objectives = jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c ORDER BY position", c, Long.class);
        if (objectives.isEmpty()) return; // plan nije upisan (npr. preopsežan)
        for (int run = 1; run <= ZERO_RUNS; run++) {
            for (Long o : objectives) {
                add(job, PlanZeroStep.KIND, "nulta " + o + " " + run, "{\"objectiveId\":" + o + ",\"run\":" + run + "}");
            }
        }
        List<Long> tasks = jdbc.queryForList("SELECT exam_task_id FROM exam_task WHERE course_id = :c ORDER BY exam_task_id", c, Long.class);
        for (int i = 0; i < tasks.size(); i += COVERAGE_BATCH) {
            add(job, PlanCoverageStep.KIND, "pokrivenost " + (i / COVERAGE_BATCH + 1),
                    "{\"taskIds\":" + tasks.subList(i, Math.min(tasks.size(), i + COVERAGE_BATCH)) + "}");
        }
        afterChecks(job); // ako nema lekcija za provjeru, odmah na popravak
    }

    void afterChecks(BuildJob job) {
        if (queue.notDone(job.courseId(), PlanZeroStep.KIND, PlanCoverageStep.KIND) > 0) return;
        int repair = repairRound(job);
        if (repair > 0) {
            add(job, PlanCheckRepairStep.KIND, "provjera popravak " + repair, "{\"repair\":" + repair + "}");
            return;
        }
        long round = round(job);
        if (round == 0) add(job, PlanCheckRepairStep.KIND, "provjera", null);
        else add(job, PlanCheckRepairStep.KIND, "provjera " + round, "{\"recheck\":true}");
    }

    // Ponovna provjera nakon izmjena kreatora (runda > 0): nulta pretpostavka jednom, samo za zadane lekcije,
    // i pokrivenost svih zadataka s ispita. Popravak tada samo javlja upozorenja, ništa ne mijenja sam.
    void recheck(long courseId, long accountId, long round, java.util.Collection<Long> objectives) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        for (Long o : objectives) {
            queue.enqueue(courseId, accountId, PlanZeroStep.KIND, "nulta " + o + " r" + round, BuildJobQueue.PRIORITY_WAITED_ON, true,
                    "{\"objectiveId\":" + o + ",\"run\":1,\"round\":" + round + "}");
        }
        List<Long> tasks = jdbc.queryForList("SELECT exam_task_id FROM exam_task WHERE course_id = :c ORDER BY exam_task_id", c, Long.class);
        for (int i = 0; i < tasks.size(); i += COVERAGE_BATCH) {
            queue.enqueue(courseId, accountId, PlanCoverageStep.KIND, "pokrivenost " + (i / COVERAGE_BATCH + 1) + " r" + round,
                    BuildJobQueue.PRIORITY_WAITED_ON, true,
                    "{\"taskIds\":" + tasks.subList(i, Math.min(tasks.size(), i + COVERAGE_BATCH)) + ",\"round\":" + round + "}");
        }
        if (objectives.isEmpty() && tasks.isEmpty()) {
            queue.enqueue(courseId, accountId, PlanCheckRepairStep.KIND, "provjera " + round, BuildJobQueue.PRIORITY_WAITED_ON, true,
                    "{\"recheck\":true}");
        }
    }

    // Krug popravka pri izradi plana: lekcije s neriješenim crvenim nalazom ponovno prolaze nultu pretpostavku
    // (jednom, stari nalazi se brišu), pa popravak ide ponovno, i dalje sa samostalnim popravljanjem.
    void repairRound(long courseId, long accountId, int repair, java.util.Collection<Long> objectives) {
        for (Long o : objectives) {
            queue.enqueue(courseId, accountId, PlanZeroStep.KIND, "nulta " + o + " popravak " + repair, BuildJobQueue.PRIORITY_WAITED_ON, true,
                    "{\"objectiveId\":" + o + ",\"run\":1,\"repair\":" + repair + "}");
        }
    }

    static int repairRound(BuildJob job) {
        String in = job.input();
        if (in == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"repair\":(\\d+)").matcher(in);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    static long round(BuildJob job) {
        String in = job.input();
        if (in == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"round\":(\\d+)").matcher(in);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private List<Long> outcomes(long courseId) {
        return jdbc.queryForList("SELECT outcome_id FROM plan_outcome WHERE course_id = :c ORDER BY position",
                new MapSqlParameterSource("c", courseId), Long.class);
    }

    private void add(BuildJob job, String kind, String ref, String input) {
        queue.enqueue(job.courseId(), job.accountId(), kind, ref, job.priority(), true, input);
    }
}
