package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DailyTaskCountService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Runs every day at 3:00 AM (Zagreb time).
     * Counts how many objectives are due today for each account and course, with the same rules
     * findNextLearningObjective uses to serve them (has a task, due, not blocked by a prerequisite
     * not yet learnt, see LearningObjectiveRepository), and stores the count in user_daily_tasks when it is below 10.
     * Courses with nothing due get a row with 0. Today's rows are replaced, so a rerun is safe.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Zagreb")
    public void updateDailyTaskCounts() {
        LocalDate today = AppTime.today();

        entityManager.createNativeQuery("DELETE FROM user_daily_tasks WHERE date = :today")
                .setParameter("today", today)
                .executeUpdate();

        // The due check uses CURRENT_DATE like findNextLearningObjective; at 3 AM Zagreb it is the same day as :today.
        String sql = """
            WITH RECURSIVE blocked AS (
                SELECT a.account_id, op.objective_id
                FROM account a
                CROSS JOIN objective_prerequisite op
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM account_objective ao_prereq
                    WHERE ao_prereq.account_id = a.account_id
                    AND ao_prereq.objective_id = op.prerequisite_id
                    AND (ao_prereq.last_grade >= 3 OR ao_prereq.n >= 2)
                )
                UNION
                SELECT b.account_id, op.objective_id
                FROM objective_prerequisite op
                JOIN blocked b ON op.prerequisite_id = b.objective_id
            ),
            counts AS (
                SELECT
                    ao.account_id,
                    co.course_id,
                    COUNT(*) FILTER (
                        WHERE EXISTS (SELECT 1 FROM task t WHERE t.objective_id = ao.objective_id)
                        AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i))
                            <= (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
                        AND NOT EXISTS (
                            SELECT 1 FROM blocked b
                            WHERE b.account_id = ao.account_id
                            AND b.objective_id = ao.objective_id
                        )
                    ) AS task_count
                FROM account_objective ao
                JOIN course_objective co ON co.objective_id = ao.objective_id
                GROUP BY ao.account_id, co.course_id
            )
            INSERT INTO user_daily_tasks (account_id, course_id, date, task_count)
            SELECT account_id, course_id, :today, task_count
            FROM counts
            WHERE task_count < 10;
            """;

        int updatedRows = entityManager.createNativeQuery(sql)
                .setParameter("today", today)
                .executeUpdate();
        System.out.println("Daily task counts updated for " + updatedRows + " account courses (with <10 tasks).");
    }
}
