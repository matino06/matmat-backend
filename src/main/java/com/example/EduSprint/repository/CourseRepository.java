package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findCourseByCourseId(Long id);

    // Admin overview: every course with how many objectives (and tasks within those objectives) it holds.
    @Query(value = """
                SELECT c.course_id,
                       c.course_name,
                       c.course_description,
                       COUNT(DISTINCT co.objective_id) AS objective_count,
                       COUNT(DISTINCT t.task_id)        AS task_count
                FROM course c
                LEFT JOIN course_objective co ON co.course_id = c.course_id
                LEFT JOIN task t ON t.objective_id = co.objective_id
                GROUP BY c.course_id, c.course_name, c.course_description
                ORDER BY c.course_id
            """, nativeQuery = true)
    List<Object[]> findAllCoursesWithCounts();
}
