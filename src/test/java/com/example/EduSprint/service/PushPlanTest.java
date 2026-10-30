package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PushPlanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    private static PushPlan.Day day(int goal, int completed, int streak, int freezes, int reviews, String next, Integer inactive) {
        return new PushPlan.Day(goal, completed, streak, freezes, reviews, next, inactive);
    }

    @Test
    void noReminderOnceTheGoalIsMet() {
        assertTrue(PushPlan.daily(day(5, 5, 3, 0, 0, null, 0), TODAY).isEmpty());
    }

    @Test
    void halfwayItCountsWhatIsLeft() {
        PushPlan.Message m = PushPlan.daily(day(5, 2, 3, 0, 0, "Logaritmi", 0), TODAY).orElseThrow();
        assertEquals("Još 3 zadatka do cilja", m.title());
        assertEquals("Riješio si 2 od 5. Sljedeće: Logaritmi.", m.body());
    }

    @Test
    void aStreakToKeepComesFirst() {
        PushPlan.Message m = PushPlan.daily(day(5, 0, 21, 0, 4, "Logaritmi", 1), TODAY).orElseThrow();
        assertEquals("Čuvaj niz od 21 dan", m.title());
        assertEquals("Prvo te čeka: Logaritmi. Već jedan zadatak čuva niz.", m.body());
    }

    @Test
    void withoutAStreakTheReviewsDueAreNamed() {
        PushPlan.Message m = PushPlan.daily(day(5, 0, 0, 0, 3, "Logaritmi", 2), TODAY).orElseThrow();
        assertEquals("Danas te čeka 3 ponavljanja", m.title());
        assertTrue(m.body().startsWith("Prvo te čeka: Logaritmi."));
    }

    @Test
    void afterTheStreakCouldBeSavedTheEmailsTakeOver() {
        assertTrue(PushPlan.daily(day(5, 0, 0, 0, 3, null, EngagementEmailPlan.LAST_STREAK_DAY + 1), TODAY).isEmpty());
        assertTrue(PushPlan.daily(day(5, 0, 0, 0, 0, null, null), TODAY).isEmpty());
    }

    @Test
    void theGeneralTextChangesFromDayToDay() {
        PushPlan.Day nothing = day(5, 0, 0, 0, 0, null, 1);
        assertNotEquals(PushPlan.daily(nothing, TODAY).orElseThrow(), PushPlan.daily(nothing, TODAY.plusDays(1)).orElseThrow());
    }

    @Test
    void theEveningWarningOnlyWhileAStreakIsOnAndNothingIsSolved() {
        assertEquals("Niz od 4 dana večeras pukne", PushPlan.streakWarning(day(5, 0, 4, 0, 0, null, 1), 18 * 60).orElseThrow().title());
        assertEquals("Niz od 4 dana čeka zadatak", PushPlan.streakWarning(day(5, 0, 4, 1, 0, null, 1), null).orElseThrow().title());
        assertTrue(PushPlan.streakWarning(day(5, 1, 5, 0, 0, null, 0), 18 * 60).isEmpty());
        assertTrue(PushPlan.streakWarning(day(5, 0, 0, 0, 0, null, 3), 18 * 60).isEmpty());
        // A reminder at 20:00 or later is itself the evening reminder.
        assertTrue(PushPlan.streakWarning(day(5, 0, 4, 0, 0, null, 1), 20 * 60).isEmpty());
    }

    @Test
    void croatianPlurals() {
        assertEquals("1 zadatak", PushPlan.tasks(1));
        assertEquals("3 zadatka", PushPlan.tasks(3));
        assertEquals("12 zadataka", PushPlan.tasks(12));
        assertEquals("22 zadatka", PushPlan.tasks(22));
        assertEquals("1 ponavljanje", PushPlan.reviews(1));
        assertEquals("5 ponavljanja", PushPlan.reviews(5));
        assertEquals("11 dana", PushPlan.days(11));
        assertEquals("21 dan", PushPlan.days(21));
    }

    @Test
    void theExamScoreStaysOffTheLockScreen() {
        PushPlan.Message m = PushPlan.examGraded("Matematika A 2025");
        assertFalse(m.body().matches(".*\\d+/\\d+.*"));
    }
}
