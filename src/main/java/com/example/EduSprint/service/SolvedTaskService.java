package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.dto.CourseDayCountDTO;
import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class SolvedTaskService {
    // Tasks (from any course) needed in a day to keep the streak going. Kept separate from the
    // daily goal so a high goal does not make the streak harder to keep.
    public static final int STREAK_MIN_TASKS = 1;

    public SolvedTaskRepository solvedTaskRepository;

    public SolvedTaskService(SolvedTaskRepository solvedTaskRepository) {
        this.solvedTaskRepository = solvedTaskRepository;
    }

    // One entry per day with at least one solved task, counted across all courses.
    public List<DailyGoalDTO> getDailyGoalDTOs(Long accountId, int dailyGoal) {
        Map<LocalDate, Integer> availablePerDay = getAvailableTasksPerDay(accountId);

        // day -> courseId -> count, both sorted
        Map<LocalDate, Map<Long, CourseDayCountDTO>> coursesPerDay = new TreeMap<>();
        for (Object[] r : solvedTaskRepository.findEndTimesWithCourse(accountId)) {
            Long courseId = ((Number) r[1]).longValue();
            CourseDayCountDTO course = coursesPerDay
                    .computeIfAbsent(AppTime.dateOf((Instant) r[0]), d -> new TreeMap<>())
                    .computeIfAbsent(courseId, id -> new CourseDayCountDTO(id, (String) r[2], 0));
            course.setCompleted(course.getCompleted() + 1);
        }

        return coursesPerDay.entrySet().stream()
                .map(e -> {
                    List<CourseDayCountDTO> courses = new ArrayList<>(e.getValue().values());
                    int completed = courses.stream().mapToInt(CourseDayCountDTO::getCompleted).sum();
                    int goal = Math.min(dailyGoal, availablePerDay.getOrDefault(e.getKey(), dailyGoal));
                    boolean goalMet = completed >= goal;
                    return new DailyGoalDTO(
                            e.getKey(),
                            goalMet,
                            completed > 0 && !goalMet,
                            completed,
                            goal,
                            completed >= STREAK_MIN_TASKS,
                            courses
                    );
                })
                .collect(Collectors.toList());
    }

    // Today's target: the daily goal, lowered when fewer tasks than that are due across all courses.
    public int getTodayGoal(Long accountId, int dailyGoal) {
        return Math.min(dailyGoal, getAvailableTasksPerDay(accountId).getOrDefault(AppTime.today(), dailyGoal));
    }

    private Map<LocalDate, Integer> getAvailableTasksPerDay(Long accountId) {
        return solvedTaskRepository.findAvailableTaskCountPerDay(accountId).stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).intValue()
                ));
    }

    public java.sql.Timestamp getMaxEndTime(Long accountId) {
        return solvedTaskRepository.findMaxEndTimeByAccountId(accountId);
    }

    public int[] calculateStreaks(Account account) {
        return calculateStreaks(getDailyGoalDTOs(account.getAccountId(), account.getDailyGoal()));
    }

    public int[] calculateStreaks(List<DailyGoalDTO> calendar) {
        if (calendar == null || calendar.isEmpty()) {
            return new int[]{0, 0}; // [current, longest]
        }

        calendar = new ArrayList<>(calendar);
        calendar.sort(Comparator.comparing(DailyGoalDTO::getDate));

        Map<LocalDate, DailyGoalDTO> dayMap = calendar.stream()
                .collect(Collectors.toMap(DailyGoalDTO::getDate, d -> d));

        LocalDate startDate = calendar.get(0).getDate();
        LocalDate today = AppTime.today();

        int longest = 0;
        int temp = 0;

        LocalDate cursor = startDate;

        while (!cursor.isAfter(today)) {

            DailyGoalDTO day = dayMap.get(cursor);
            boolean streakDay = day != null && day.isStreakDay();

            if (streakDay) {
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
        if (todayDTO == null || !todayDTO.isStreakDay()) {
            cursor = cursor.minusDays(1);
        }

        while (!cursor.isBefore(startDate)) {

            DailyGoalDTO day = dayMap.get(cursor);

            if (day != null && day.isStreakDay()) {
                current++;
            } else {
                break;
            }

            cursor = cursor.minusDays(1);
        }

        return new int[]{current, longest};
    }
}
