package com.example.EduSprint.service;

import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolvedTaskServiceTest {

    private static final long ACCOUNT_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now();

    private final SolvedTaskRepository repository = mock(SolvedTaskRepository.class);
    private final SolvedTaskService service = new SolvedTaskService(repository);

    private final List<Object[]> solvedRows = new ArrayList<>();
    private final List<Object[]> availableRows = new ArrayList<>();

    private void solved(LocalDate date, long courseId, String courseName, int count) {
        solvedRows.add(new Object[]{Date.valueOf(date), courseId, courseName, (long) count});
    }

    private void available(LocalDate date, int count) {
        availableRows.add(new Object[]{Date.valueOf(date), (long) count});
    }

    private List<DailyGoalDTO> calendar(int dailyGoal) {
        when(repository.findSolvedTaskCountPerDayAndCourse(ACCOUNT_ID)).thenReturn(solvedRows);
        when(repository.findAvailableTaskCountPerDay(ACCOUNT_ID)).thenReturn(availableRows);
        return service.getDailyGoalDTOs(ACCOUNT_ID, dailyGoal);
    }

    @Test
    void tasksFromAllCoursesCountTowardsOneGoal() {
        solved(TODAY, 1L, "Matematika", 2);
        solved(TODAY, 2L, "Fizika", 2);

        List<DailyGoalDTO> days = calendar(4);

        assertEquals(1, days.size());
        DailyGoalDTO today = days.get(0);
        assertEquals(4, today.getCompleted());
        assertTrue(today.isGoalMet());
        assertFalse(today.isPartial());
        assertEquals(2, today.getCourses().size());
    }

    @Test
    void dayBelowGoalStillKeepsTheStreak() {
        solved(TODAY, 1L, "Matematika", 1);

        DailyGoalDTO today = calendar(5).get(0);

        assertFalse(today.isGoalMet());
        assertTrue(today.isPartial());
        assertTrue(today.isStreakDay());
    }

    @Test
    void streakContinuesWhenSwitchingCourses() {
        solved(TODAY.minusDays(2), 1L, "Matematika", 1);
        solved(TODAY.minusDays(1), 2L, "Fizika", 1);
        solved(TODAY, 1L, "Matematika", 1);

        assertArrayEquals(new int[]{3, 3}, service.calculateStreaks(calendar(5)));
    }

    @Test
    void unsolvedTodayDoesNotBreakTheCurrentStreak() {
        solved(TODAY.minusDays(5), 1L, "Matematika", 1);
        solved(TODAY.minusDays(2), 1L, "Matematika", 1);
        solved(TODAY.minusDays(1), 2L, "Fizika", 1);

        assertArrayEquals(new int[]{2, 2}, service.calculateStreaks(calendar(3)));
    }

    @Test
    void missedDayResetsTheCurrentStreak() {
        solved(TODAY.minusDays(4), 1L, "Matematika", 1);
        solved(TODAY.minusDays(3), 1L, "Matematika", 1);
        solved(TODAY.minusDays(2), 1L, "Matematika", 1);

        assertArrayEquals(new int[]{0, 3}, service.calculateStreaks(calendar(3)));
    }

    @Test
    void goalIsLoweredWhenFewerTasksAreDue() {
        solved(TODAY, 1L, "Matematika", 2);
        available(TODAY, 2);

        DailyGoalDTO today = calendar(5).get(0);

        assertEquals(2, today.getGoal());
        assertTrue(today.isGoalMet());
        assertEquals(2, service.getTodayGoal(ACCOUNT_ID, 5));
    }

    @Test
    void goalIsNotRaisedWhenMoreTasksAreDue() {
        available(TODAY, 8);
        when(repository.findAvailableTaskCountPerDay(ACCOUNT_ID)).thenReturn(availableRows);

        assertEquals(5, service.getTodayGoal(ACCOUNT_ID, 5));
    }
}
