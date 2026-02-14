package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findCourseByCourseId(Long id);
}
