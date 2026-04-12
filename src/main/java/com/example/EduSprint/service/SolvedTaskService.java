package com.example.EduSprint.service;

import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public java.sql.Timestamp getMaxEndTime(Long accountId) {
        return solvedTaskRepository.findMaxEndTimeByAccountId(accountId);
    }

    public int[] calculateStreaks(Long accountId, Long courseId) {
        List<DailyGoalDTO> calendar = this.getDailyGoalDTOs(accountId, courseId);

        if (calendar == null || calendar.isEmpty()) {
            return new int[]{0, 0}; // [current, longest]
        }

        calendar.sort(Comparator.comparing(DailyGoalDTO::getDate));

        Map<LocalDate, DailyGoalDTO> dayMap = calendar.stream()
                .collect(Collectors.toMap(DailyGoalDTO::getDate, d -> d));

        LocalDate startDate = calendar.get(0).getDate();
        LocalDate today = LocalDate.now();

        int longest = 0;
        int temp = 0;

        LocalDate cursor = startDate;

        while (!cursor.isAfter(today)) {

            DailyGoalDTO day = dayMap.get(cursor);
            boolean goalMet = day != null && day.isGoalMet();

            if (goalMet) {
                temp++;
                longest = Math.max(longest, temp);
            } else {
                temp = 0;
            }

            cursor = cursor.plusDays(1);
        }

        // ===== CURRENT STREAK =====
        int current = 0;
        cursor = today;

        DailyGoalDTO todayDTO = dayMap.get(today);
        if (todayDTO == null || !todayDTO.isGoalMet()) {
            cursor = cursor.minusDays(1);
        }

        while (!cursor.isBefore(startDate)) {

            DailyGoalDTO day = dayMap.get(cursor);

            if (day != null && day.isGoalMet()) {
                current++;
            } else {
                break;
            }

            cursor = cursor.minusDays(1);
        }

        return new int[]{current, longest};
    }
}
