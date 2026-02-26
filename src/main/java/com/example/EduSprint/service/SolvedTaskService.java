package com.example.EduSprint.service;

import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolvedTaskService {
    public SolvedTaskRepository solvedTaskRepository;

    public SolvedTaskService(SolvedTaskRepository solvedTaskRepository) {
        this.solvedTaskRepository = solvedTaskRepository;
    }

    public List<DailyGoalDTO> getDailyGoalDTOs(Long accountId, Long courseId) {
        List<Object[]> dailyGoalObjects = solvedTaskRepository.findSolvedTaskCountPerDay(accountId, courseId);
        List<DailyGoalDTO> dailyGoalDTOS = dailyGoalObjects.stream()
                .map(r -> new DailyGoalDTO(
                        ((java.sql.Date) r[0]).toLocalDate(),
                        (boolean) r[2],
                        (boolean) r[3],
                        ((Number) r[1]).intValue(),
                        ((Number) r[4]).intValue()
                ))
                .collect(Collectors.toList());

        return dailyGoalDTOS;
    }
}
