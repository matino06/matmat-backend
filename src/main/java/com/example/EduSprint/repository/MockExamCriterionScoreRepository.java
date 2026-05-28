package com.example.EduSprint.repository;

import com.example.EduSprint.entity.MockExamCriterionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockExamCriterionScoreRepository extends JpaRepository<MockExamCriterionScore, Long> {

    List<MockExamCriterionScore> findByAnswer_AnswerId(Long answerId);
}
