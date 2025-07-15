package com.example.EduSprint.repository;

import com.example.EduSprint.entity.SolvedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolvedTaskRepository extends JpaRepository<SolvedTask, Long> {
}
