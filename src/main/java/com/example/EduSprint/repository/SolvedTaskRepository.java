package com.example.EduSprint.repository;

import com.example.EduSprint.entity.SolvedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolvedTaskRepository extends JpaRepository<SolvedTask, Long> {

    // Last solved task per account: [accountId, endTime]
    @Query("SELECT s.account.accountId, MAX(s.endTime) FROM SolvedTask s GROUP BY s.account.accountId")
    List<Object[]> findLastEndTimePerAccount();

    // Last solved task per course of one account: [courseName, endTime]
    @Query("SELECT c.courseName, MAX(s.endTime) FROM SolvedTask s JOIN s.course c WHERE s.account.accountId = :accountId GROUP BY c.courseId, c.courseName")
    List<Object[]> findLastEndTimePerCourse(@Param("accountId") Long accountId);

    // End time and course of every solved task. Grouped into days in Java (AppTime.ZONE) so the day
    // boundary does not depend on the database time zone.
    @Query("SELECT s.endTime, c.courseId, c.courseName FROM SolvedTask s JOIN s.course c WHERE s.account.accountId = :accountId")
    List<Object[]> findEndTimesWithCourse(@Param("accountId") Long accountId);

    // Days on which every course the account learns had fewer tasks due than usual (a user_daily_tasks
    // row exists for each of them), with the total due across courses. On other days the goal is not capped.
    @Query(value = """
                SELECT udt.date,
                       SUM(udt.task_count) AS available
                    FROM user_daily_tasks udt
                    WHERE udt.account_id = :accountId
                    GROUP BY udt.date
                    HAVING COUNT(DISTINCT udt.course_id) >= (SELECT COUNT(DISTINCT co.course_id)
                                                              FROM account_objective ao
                                                                  JOIN course_objective co ON co.objective_id = ao.objective_id
                                                              WHERE ao.account_id = :accountId)
            """, nativeQuery = true)
    List<Object[]> findAvailableTaskCountPerDay(@Param("accountId") Long accountId);
}
