package com.example.EduSprint.repository;

import com.example.EduSprint.entity.MockExamQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockExamQuestionRepository extends JpaRepository<MockExamQuestion, Long> {

    @EntityGraph(attributePaths = {"images"})
    List<MockExamQuestion> findByExam_ExamIdOrderBySortOrderAsc(Long examId);
}
