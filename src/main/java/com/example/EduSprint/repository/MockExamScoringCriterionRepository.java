package com.example.EduSprint.repository;

import com.example.EduSprint.entity.MockExamScoringCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockExamScoringCriterionRepository extends JpaRepository<MockExamScoringCriterion, Long> {

    List<MockExamScoringCriterion> findByQuestion_QuestionIdOrderByCriterionOrderAsc(Long questionId);
}
