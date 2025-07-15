package com.example.EduSprint.repository;

import com.example.EduSprint.entity.ExplanationStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplanationStepRepository extends JpaRepository<ExplanationStep, Long> {
}
