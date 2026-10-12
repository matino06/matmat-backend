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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class SolvedTaskService {
    // Tasks (from any course) needed in a day to keep the streak going. Kept separate from the
    // daily goal so a high goal does not make the streak harder to keep.
    public static final int STREAK_MIN_TASKS = 1;

    // Streak freezes: one is earned every FREEZE_EVERY_STREAK_DAYS streak days, at most MAX_STREAK_FREEZES are held.
    public static final int FREEZE_EVERY_STREAK_DAYS = 7;
    public static final int MAX_STREAK_FREEZES = 2;

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

    // Day of the last solved task (Zagreb time) per account; accounts that never solved one are missing.
    public Map<Long, LocalDate> getLastActivityDatePerAccount() {
        return toDatePerKey(solvedTaskRepository.findLastEndTimePerAccount());
    }

    // Day of the last solved task (Zagreb time) per course name of one account.
    public Map<String, LocalDate> getLastActivityDatePerCourse(Long accountId) {
        return toDatePerKey(solvedTaskRepository.findLastEndTimePerCourse(accountId));
    }

    @SuppressWarnings("unchecked")
    private static <K> Map<K, LocalDate> toDatePerKey(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> (K) r[0],
                r -> AppTime.dateOf((Instant) r[1])
        ));
    }

    // State of the streak as of today, replayed day by day from the first streak day.
    //   current           - streak length (today counts once done; an unfinished today does not break it)
    //   longest           - longest streak ever
    //   freezes           - streak freezes available now
    //   daysToNextFreeze  - streak days still needed to earn the next freeze
    //   frozenDays        - missed days a freeze covered
    //   lostOn/lostLength - last day a streak broke, and how long it was
    public record Streak(int current, int longest, int freezes, int daysToNextFreeze,
                         Set<LocalDate> frozenDays, LocalDate lostOn, int lostLength) {}

    public Streak streak(Account account) {
        return streak(getDailyGoalDTOs(account.getAccountId(), account.getDailyGoal()));
    }

    // A freeze is earned every FREEZE_EVERY_STREAK_DAYS streak days (at most MAX_STREAK_FREEZES held).
    // A missed day uses one up: the streak survives but does not grow, as nothing was solved that day.
    public Streak streak(List<DailyGoalDTO> calendar) {
        Set<LocalDate> streakDays = calendar.stream()
                .filter(DailyGoalDTO::isStreakDay)
                .map(DailyGoalDTO::getDate)
                .collect(Collectors.toSet());
        if (streakDays.isEmpty()) {
            return new Streak(0, 0, 0, FREEZE_EVERY_STREAK_DAYS, Set.of(), null, 0);
        }

        LocalDate today = AppTime.today();
        int current = 0;
        int longest = 0;
        int freezes = 0;
        int daysSinceFreeze = 0;
        Set<LocalDate> frozenDays = new TreeSet<>();
        LocalDate lostOn = null;
        int lostLength = 0;

        for (LocalDate day = Collections.min(streakDays); !day.isAfter(today); day = day.plusDays(1)) {
            if (streakDays.contains(day)) {
                current++;
                longest = Math.max(longest, current);
                if (++daysSinceFreeze == FREEZE_EVERY_STREAK_DAYS) {
                    freezes = Math.min(MAX_STREAK_FREEZES, freezes + 1);
                    daysSinceFreeze = 0;
                }
            } else if (day.equals(today) || current == 0) {
                // today is not over yet; with no streak there is nothing to protect
            } else if (freezes > 0) {
                freezes--;
                frozenDays.add(day);
            } else {
                lostOn = day;
                lostLength = current;
                current = 0;
                daysSinceFreeze = 0;
            }
        }

        return new Streak(current, longest, freezes, FREEZE_EVERY_STREAK_DAYS - daysSinceFreeze,
                frozenDays, lostOn, lostLength);
    }
}
