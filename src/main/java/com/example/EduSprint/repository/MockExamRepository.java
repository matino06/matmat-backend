package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Course;
import com.example.EduSprint.entity.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockExamRepository extends JpaRepository<MockExam, Long> {

    List<MockExam> findAllByCourseAndIsPublishedTrueOrderByYearDescExamIdDesc(Course course);

    List<MockExam> findAllByCourseOrderByYearDescExamIdDesc(Course course);
}
