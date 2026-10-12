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

    private List<Long> outcomes(long courseId) {
        return jdbc.queryForList("SELECT outcome_id FROM plan_outcome WHERE course_id = :c ORDER BY position",
                new MapSqlParameterSource("c", courseId), Long.class);
    }

    private void add(BuildJob job, String kind, String ref, String input) {
        queue.enqueue(job.courseId(), job.accountId(), kind, ref, BuildJobQueue.PRIORITY_BACKGROUND, true, input);
    }
}
