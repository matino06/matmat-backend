package com.example.EduSprint.repository;

import com.example.EduSprint.entity.SolvedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolvedTaskRepository extends JpaRepository<SolvedTask, Long> {

    @Query(value = "SELECT MAX(end_time) FROM solved_task WHERE account_id = :accountId", nativeQuery = true)
    java.sql.Timestamp findMaxEndTimeByAccountId(@Param("accountId") Long accountId);

    @Query(value = """
                SELECT st.end_time::date AS date,
                       c.course_id,
                       c.course_name,
                       COUNT(*) AS solvedTaskCount
                    FROM solved_task st
                        JOIN course c ON c.course_id = st.course_id
                    WHERE st.account_id = :accountId
                    GROUP BY st.end_time::date, c.course_id, c.course_name
                    ORDER BY date, c.course_id
            """, nativeQuery = true)
    List<Object[]> findSolvedTaskCountPerDayAndCourse(@Param("accountId") Long accountId);

    // Days on which every course of the account had fewer tasks due than usual (a user_daily_tasks
    // row exists for each of them), with the total due across courses. On other days the goal is not capped.
    @Query(value = """
                SELECT udt.date,
                       SUM(udt.task_count) AS available
                    FROM user_daily_tasks udt
                    WHERE udt.account_id = :accountId
                    GROUP BY udt.date
                    HAVING COUNT(DISTINCT udt.course_id) >= (SELECT COUNT(*)
                                                              FROM user_course_goal ucg
                                                              WHERE ucg.account_id = :accountId)
            """, nativeQuery = true)
    List<Object[]> findAvailableTaskCountPerDay(@Param("accountId") Long accountId);
}
