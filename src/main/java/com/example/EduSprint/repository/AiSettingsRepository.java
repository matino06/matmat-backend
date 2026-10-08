package com.example.EduSprint.repository;

import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSettingsRepository extends JpaRepository<AiSettings, Short> {

    @Query("SELECT new com.example.EduSprint.dto.AiSettingsDTO(s.chatModel, s.chatDailyLimit, s.chatMaxTokens, " +
            "s.chatReasoningEffort, s.gradingModelText, s.gradingModelVision, s.gradingMaxTokens, " +
            "s.gradingReasoningEffort, s.updatedAt, a.email) " +
            "FROM AiSettings s LEFT JOIN s.updatedBy a WHERE s.id = " + AiSettings.ID)
    Optional<AiSettingsDTO> findCurrent();
}
