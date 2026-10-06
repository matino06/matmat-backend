package com.example.EduSprint.repository;

import com.example.EduSprint.entity.ExplanationStep;
import com.example.EduSprint.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExplanationStepRepository extends JpaRepository<ExplanationStep, Long> {

    List<ExplanationStep> findByTaskOrderByStepNumberAsc(Task task);
}
