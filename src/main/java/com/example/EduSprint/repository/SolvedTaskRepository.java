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
                SELECT st.end_time::date AS date, COUNT(*) AS solvedTaskCount,
                        (
                            COUNT(*) >= ucg.daily_goal
                        ) AS goalMet,
                        (
                            COUNT(*) < ucg.daily_goal AND COUNT(*) > 0
                        ) AS partial
                    FROM solved_task st
                        JOIN user_course_goal ucg
                            ON ucg.account_id = st.account_id
                                AND ucg.course_id = st.course_id
                    WHERE st.account_id=:accountId
                    GROUP BY st.end_time::date, ucg.daily_goal
            """, nativeQuery = true)
    List<Object[]> findSolvedTaskCountPerDay(@Param("accountId") Long accountId, @Param("courseId") Long courseId);
}
