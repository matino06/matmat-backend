package com.example.EduSprint.repository;

import com.example.EduSprint.entity.MockExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockExamAttemptRepository extends JpaRepository<MockExamAttempt, Long> {

    List<MockExamAttempt> findByAccount_AccountIdOrderBySubmittedAtDesc(Long accountId);

    Optional<MockExamAttempt> findByAttemptIdAndAccount_AccountId(Long attemptId, Long accountId);
}
