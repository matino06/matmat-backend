package com.example.EduSprint.repository;

import com.example.EduSprint.entity.MockExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockExamAnswerRepository extends JpaRepository<MockExamAnswer, Long> {

    List<MockExamAnswer> findByAttempt_AttemptId(Long attemptId);

    long countByAttempt_AttemptIdAndAiGradingStatus(Long attemptId, String aiGradingStatus);
}
