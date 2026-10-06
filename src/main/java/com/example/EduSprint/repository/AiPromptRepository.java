package com.example.EduSprint.repository;

import com.example.EduSprint.entity.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {

    Optional<AiPrompt> findFirstBySubjectIsNullAndIsActiveTrue();

    Optional<AiPrompt> findFirstBySubjectAndIsActiveTrue(String subject);
}
