package com.example.EduSprint.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DailyTaskCountService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Runs every day at 3:00 AM (server time).
     * Counts how many tasks are due for each user (considering prerequisites and current course)
     * and stores the count in user_daily_tasks if it is less than 10.
     * Uses PostgreSQL's ON CONFLICT to upsert.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Zagreb")
    public void updateDailyTaskCounts() {
        String sql = """
            INSERT INTO user_daily_tasks (account_id, course_id, date, task_count)
            SELECT 
                ao.account_id,
                co.course_id,
                CURRENT_DATE,
                COUNT(*) as task_count
            FROM account_objective ao
            JOIN learning_objective lo ON ao.objective_id = lo.objective_id
            JOIN course_objective co ON lo.objective_id = co.objective_id
            WHERE EXISTS (SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id)
            AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i)) 
                <= (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
            AND NOT EXISTS (
                SELECT 1 
                FROM objective_prerequisite op
                WHERE op.objective_id = lo.objective_id
                AND NOT EXISTS (
                    SELECT 1 
                    FROM account_objective ao_prereq
                    WHERE ao_prereq.account_id = ao.account_id
                    AND ao_prereq.objective_id = op.prerequisite_id
                    AND ao_prereq.n >= 2
                )
            )
            GROUP BY ao.account_id, co.course_id
            HAVING COUNT(*) < 10;
            """;

        int updatedRows = entityManager.createNativeQuery(sql).executeUpdate();
        System.out.println("Daily task counts updated for " + updatedRows + " users (with <10 tasks).");
    }
}