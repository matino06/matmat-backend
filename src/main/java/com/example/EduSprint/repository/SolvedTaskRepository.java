package com.example.EduSprint.repository;

import com.example.EduSprint.entity.SolvedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolvedTaskRepository extends JpaRepository<SolvedTask, Long> {

    @Query(value = """
                SELECT st.end_time::date AS date,
                       COUNT(*) AS solvedTaskCount,
                       (COUNT(*) >= ucg.daily_goal OR (udt.task_count IS NOT NULL AND COUNT(*) >= udt.task_count)) AS goalMet,
                       (COUNT(*) > 0
                        AND COUNT(*) < ucg.daily_goal
                        AND (udt.task_count IS NULL OR COUNT(*) < udt.task_count)) AS partial,
                        LEAST(ucg.daily_goal, COALESCE(udt.task_count, ucg.daily_goal)) AS target_goal
                    FROM solved_task st
                        JOIN user_course_goal ucg
                            ON ucg.account_id = st.account_id
                                AND ucg.course_id = st.course_id
                        LEFT JOIN user_daily_tasks udt
                            ON udt.date = st.end_time::date
                                AND udt.course_id = st.course_id
                                AND udt.account_id = :accountId
                    WHERE st.account_id=:accountId
                        AND st.course_id=:courseId
                    GROUP BY st.end_time::date, udt.task_count, ucg.daily_goal
            """, nativeQuery = true)
    List<Object[]> findSolvedTaskCountPerDay(@Param("accountId") Long accountId, @Param("courseId") Long courseId);
}
