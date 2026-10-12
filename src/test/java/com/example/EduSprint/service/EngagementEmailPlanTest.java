package com.example.EduSprint.service;

import com.example.EduSprint.service.EngagementEmailPlan.NeglectedCourse;
import com.example.EduSprint.service.EngagementEmailPlan.Type;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngagementEmailPlanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate LONG_AGO = TODAY.minusYears(1);

    private static Optional<Type> inactiveFor(int days, int lostStreak) {
        return EngagementEmailPlan.forDay(TODAY, LONG_AGO, TODAY.minusDays(days), () -> lostStreak);
    }

    private static Optional<Type> registeredDaysAgoWithoutTasks(int days) {
        return EngagementEmailPlan.forDay(TODAY, TODAY.minusDays(days), null, () -> 0);
    }

    @Test
    void activeTodayGetsNothing() {
        assertTrue(inactiveFor(0, 0).isEmpty());
    }

    @Test
    void activeYesterdayGetsTheDailyReminder() {
        assertEquals(Optional.of(Type.DAILY_REMINDER), inactiveFor(1, 0));
    }

    @Test
    void brokenLongStreakGetsTheStreakLostEmail() {
        assertEquals(Optional.of(Type.STREAK_LOST), inactiveFor(2, 3));
    }

    @Test
    void brokenShortStreakGetsNothing() {
        assertTrue(inactiveFor(2, 2).isEmpty());
    }

    @Test
    void inactivitySequenceSendsFourEmailsInAMonthThenStops() {
        List<Integer> emailDays = new ArrayList<>();
        for (int day = 2; day <= 365; day++) {
            if (inactiveFor(day, 5).isPresent()) emailDays.add(day);
        }
        assertEquals(List.of(2, 7, 14, 30), emailDays);
        assertEquals(Optional.of(Type.PAUSE), inactiveFor(7, 5));
        assertEquals(Optional.of(Type.PAUSE), inactiveFor(14, 5));
        assertEquals(Optional.of(Type.LAST_CALL), inactiveFor(30, 5));
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
    void lostStreakIsOnlyComputedWhenNeeded() {
        EngagementEmailPlan.forDay(TODAY, LONG_AGO, TODAY.minusDays(1), () -> {
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
