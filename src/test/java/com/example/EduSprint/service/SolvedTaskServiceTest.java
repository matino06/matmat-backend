package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolvedTaskServiceTest {

    private static final long ACCOUNT_ID = 1L;
    private static final LocalDate TODAY = AppTime.today();

    private final SolvedTaskRepository repository = mock(SolvedTaskRepository.class);
    private final SolvedTaskService service = new SolvedTaskService(repository);

    private final List<Object[]> solvedRows = new ArrayList<>();
    private final List<Object[]> availableRows = new ArrayList<>();

    private void solved(LocalDate date, long courseId, String courseName, int count) {
        for (int i = 0; i < count; i++) {
            solvedAt(date.atTime(12, 0).atZone(AppTime.ZONE).toInstant(), courseId, courseName);
        }
    }

    private void solvedAt(Instant endTime, long courseId, String courseName) {
        solvedRows.add(new Object[]{endTime, courseId, courseName});
    }

    private void available(LocalDate date, int count) {
        availableRows.add(new Object[]{Date.valueOf(date), (long) count});
    }

    private List<DailyGoalDTO> calendar(int dailyGoal) {
        when(repository.findEndTimesWithCourse(ACCOUNT_ID)).thenReturn(solvedRows);
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
    void taskSolvedJustAfterMidnightCountsForTheZagrebDay() {
        // 00:30 in Zagreb is still the previous day in UTC
        solvedAt(TODAY.atTime(LocalTime.of(0, 30)).atZone(AppTime.ZONE).toInstant(), 1L, "Matematika");

        List<DailyGoalDTO> days = calendar(3);

        assertEquals(1, days.size());
        assertEquals(TODAY, days.get(0).getDate());
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

        assertStreak(3, 3, service.streak(calendar(5)));
    }

    @Test
    void unsolvedTodayDoesNotBreakTheCurrentStreak() {
        solved(TODAY.minusDays(5), 1L, "Matematika", 1);
        solved(TODAY.minusDays(2), 1L, "Matematika", 1);
        solved(TODAY.minusDays(1), 2L, "Fizika", 1);

        assertStreak(2, 2, service.streak(calendar(3)));
    }

    @Test
    void missedDayResetsTheCurrentStreak() {
        solved(TODAY.minusDays(4), 1L, "Matematika", 1);
        solved(TODAY.minusDays(3), 1L, "Matematika", 1);
        solved(TODAY.minusDays(2), 1L, "Matematika", 1);

        SolvedTaskService.Streak streak = service.streak(calendar(3));
        assertStreak(0, 3, streak);
        assertEquals(TODAY.minusDays(1), streak.lostOn());
        assertEquals(3, streak.lostLength());
    }

    // Solves one task on each of `days` consecutive days, ending `endDaysAgo` days before today.
    private void solvedDays(int days, int endDaysAgo) {
        for (int i = 0; i < days; i++) {
            solved(TODAY.minusDays(endDaysAgo + i), 1L, "Matematika", 1);
        }
    }

    private static void assertStreak(int current, int longest, SolvedTaskService.Streak streak) {
        assertEquals(current, streak.current(), "current");
        assertEquals(longest, streak.longest(), "longest");
    }

    @Test
    void aFreezeIsEarnedEverySevenStreakDays() {
        solvedDays(6, 0);
        SolvedTaskService.Streak streak = service.streak(calendar(3));
        assertEquals(0, streak.freezes());
        assertEquals(1, streak.daysToNextFreeze());

        solvedRows.clear();
        solvedDays(7, 0);
        streak = service.streak(calendar(3));
        assertEquals(1, streak.freezes());
        assertEquals(7, streak.daysToNextFreeze());
    }

    @Test
    void atMostTwoFreezesAreHeld() {
        solvedDays(30, 0);
        assertEquals(SolvedTaskService.MAX_STREAK_FREEZES, service.streak(calendar(3)).freezes());
    }

    @Test
    void aMissedDayUsesAFreezeAndKeepsTheStreakWithoutGrowingIt() {
        solvedDays(7, 3);  // days -9..-3: earns a freeze
        // day -2 missed
        solvedDays(2, 0);  // days -1, 0

        SolvedTaskService.Streak streak = service.streak(calendar(3));

        assertStreak(9, 9, streak);
        assertEquals(0, streak.freezes());
        assertEquals(Set.of(TODAY.minusDays(2)), streak.frozenDays());
        assertNull(streak.lostOn());
    }

    @Test
    void streakBreaksWhenFreezesRunOut() {
        solvedDays(14, 4);  // two freezes
        // days -3, -2, -1 missed: two frozen, the third breaks the streak

        SolvedTaskService.Streak streak = service.streak(calendar(3));

        assertStreak(0, 14, streak);
        assertEquals(Set.of(TODAY.minusDays(3), TODAY.minusDays(2)), streak.frozenDays());
        assertEquals(TODAY.minusDays(1), streak.lostOn());
        assertEquals(14, streak.lostLength());
    }

    @Test
    void streakSavedByAFreezeIsStillAliveToday() {
        solvedDays(7, 2);  // days -8..-2
        // day -1 missed, today not solved yet

        SolvedTaskService.Streak streak = service.streak(calendar(3));

        assertStreak(7, 7, streak);
        assertEquals(Set.of(TODAY.minusDays(1)), streak.frozenDays());
    }

    @Test
    void noStreakNoFreezes() {
        SolvedTaskService.Streak streak = service.streak(calendar(3));
        assertStreak(0, 0, streak);
        assertEquals(0, streak.freezes());
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
