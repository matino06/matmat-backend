package com.example.EduSprint.service;

import com.example.EduSprint.dto.ObjectiveDTO;
import com.example.EduSprint.dto.ProgressSummaryDTO;
import com.example.EduSprint.entity.Account;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProgressService {

    private final LearningObjectiveService learningObjectiveService;

    public ProgressService(LearningObjectiveService learningObjectiveService) {
        this.learningObjectiveService = learningObjectiveService;
    }

    public ProgressSummaryDTO getProgressSummary(Account account) {
        LocalDate today = LocalDate.now();

        List<ObjectiveDTO> todayObjectives = learningObjectiveService.findObjectivesForToday(account);
        List<ObjectiveDTO> scheduledObjectives = learningObjectiveService.findScheduledObjectives(account);

        ProgressSummaryDTO dto = new ProgressSummaryDTO();
        dto.setTodayObjectives(todayObjectives);
        dto.setFutureObjectives(scheduledObjectives);
        return dto;
    }
}
