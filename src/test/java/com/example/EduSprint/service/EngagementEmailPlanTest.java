package com.example.EduSprint.service;

import com.example.EduSprint.service.EngagementEmailPlan.NeglectedCourse;
import com.example.EduSprint.service.EngagementEmailPlan.Type;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngagementEmailPlanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate LONG_AGO = TODAY.minusYears(1);

    private static SolvedTaskService.Streak alive(int current) {
        return new SolvedTaskService.Streak(current, current, 0, 7, Set.of(), null, 0);
    }

    private static SolvedTaskService.Streak lostYesterday(int length) {
        return new SolvedTaskService.Streak(0, length, 0, 7, Set.of(), TODAY.minusDays(1), length);
    }

    private static SolvedTaskService.Streak lostEarlier(int length) {
        return new SolvedTaskService.Streak(0, length, 0, 7, Set.of(), TODAY.minusDays(5), length);
    }

    private static Optional<Type> inactiveFor(int days, SolvedTaskService.Streak streak) {
        return EngagementEmailPlan.forDay(TODAY, LONG_AGO, TODAY.minusDays(days), () -> streak);
    }

    private static Optional<Type> registeredDaysAgoWithoutTasks(int days) {
        return EngagementEmailPlan.forDay(TODAY, TODAY.minusDays(days), null, () -> alive(0));
    }

    @Test
    void activeTodayGetsNothing() {
        assertTrue(inactiveFor(0, alive(3)).isEmpty());
    }

    @Test
    void activeYesterdayGetsTheDailyReminder() {
        assertEquals(Optional.of(Type.DAILY_REMINDER), inactiveFor(1, alive(1)));
    }

    @Test
    void streakKeptByFreezesStillGetsTheDailyReminder() {
        assertEquals(Optional.of(Type.DAILY_REMINDER), inactiveFor(2, alive(9)));
        assertEquals(Optional.of(Type.DAILY_REMINDER), inactiveFor(3, alive(9)));
    }

    @Test
    void streakBrokenYesterdayGetsTheStreakLostEmail() {
        assertEquals(Optional.of(Type.STREAK_LOST), inactiveFor(2, lostYesterday(3)));
        // after two freezes ran out
        assertEquals(Optional.of(Type.STREAK_LOST), inactiveFor(4, lostYesterday(14)));
    }

    @Test
    void brokenShortStreakGetsNothing() {
        assertTrue(inactiveFor(2, lostYesterday(2)).isEmpty());
    }

    @Test
    void streakBrokenEarlierGetsNothing() {
        assertTrue(inactiveFor(3, lostEarlier(5)).isEmpty());
    }

    @Test
    void inactivitySequenceSendsFourEmailsInAMonthThenStops() {
        List<Integer> emailDays = new ArrayList<>();
        for (int day = 2; day <= 365; day++) {
            SolvedTaskService.Streak streak = day == 2 ? lostYesterday(5) : lostEarlier(5);
            if (inactiveFor(day, streak).isPresent()) emailDays.add(day);
        }
        assertEquals(List.of(2, 7, 14, 30), emailDays);
        assertEquals(Optional.of(Type.PAUSE), inactiveFor(7, lostEarlier(5)));
        assertEquals(Optional.of(Type.PAUSE), inactiveFor(14, lostEarlier(5)));
        assertEquals(Optional.of(Type.LAST_CALL), inactiveFor(30, lostEarlier(5)));
    }

    @Test
    void neverStartedGetsTwoEmailsThenStops() {
        List<Integer> emailDays = new ArrayList<>();
        for (int day = 0; day <= 365; day++) {
            if (registeredDaysAgoWithoutTasks(day).isPresent()) emailDays.add(day);
        }
        assertEquals(List.of(2, 7), emailDays);
        assertEquals(Optional.of(Type.NEVER_STARTED), registeredDaysAgoWithoutTasks(2));
        assertEquals(Optional.of(Type.NEVER_STARTED_LAST), registeredDaysAgoWithoutTasks(7));
    }

    @Test
    void streakIsOnlyComputedInTheFirstDaysOfInactivity() {
        EngagementEmailPlan.forDay(TODAY, LONG_AGO, TODAY.minusDays(7), () -> {
            throw new AssertionError("streak must not be computed");
        });
    }

    @Test
    void logTypeFitsTheEmailLogColumn() {
        for (Type type : Type.values()) {
            assertTrue(type.logType().length() <= 30, type.logType());
        }
    }

    @Test
    void neglectedCourseIsTheLongestSkippedOneWithinTheWindow() {
        Optional<NeglectedCourse> course = EngagementEmailPlan.neglectedCourse(Map.of(
                "Matematika", TODAY.minusDays(1),
                "Fizika", TODAY.minusDays(9),
                "Kemija", TODAY.minusDays(6),
                "Biologija", TODAY.minusDays(60)
        ), TODAY);

        assertEquals(Optional.of(new NeglectedCourse("Fizika", 9)), course);
    }

    @Test
    void noNeglectedCourseWithASingleCourse() {
        assertFalse(EngagementEmailPlan.neglectedCourse(Map.of("Matematika", TODAY.minusDays(9)), TODAY).isPresent());
    }

    @Test
    void noNeglectedCourseWhenNoCourseIsInUse() {
        assertFalse(EngagementEmailPlan.neglectedCourse(Map.of(
                "Matematika", TODAY.minusDays(8),
                "Fizika", TODAY.minusDays(9)
        ), TODAY).isPresent());
    }
}
