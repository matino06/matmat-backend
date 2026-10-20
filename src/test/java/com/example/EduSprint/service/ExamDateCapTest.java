package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Dan prije ispita ponavlja se samo ono što je slabije ocijenjeno ili bi palo na dan ispita.
class ExamDateCapTest {

    private static final Instant MONDAY_NOON = Instant.parse("2026-10-12T10:00:00Z"); // 12. 10. u Zagrebu
    private static final LocalDate EXAM = LocalDate.of(2026, 10, 20);

    private static short cap(int interval, int q, Instant solvedAt, LocalDate exam) {
        return AccountObjectiveService.capToExam((short) interval, (short) q, solvedAt, exam);
    }

    @Test
    void weakLessonThatWouldPassTheExamEndsTheDayBefore() {
        assertEquals(7, cap(15, 3, MONDAY_NOON, EXAM)); // 19. 10.
    }

    @Test
    void wellKnownLessonThatWouldPassTheExamKeepsItsInterval() {
        assertEquals(15, cap(15, 4, MONDAY_NOON, EXAM));
        assertEquals(15, cap(15, 5, MONDAY_NOON, EXAM));
    }

    @Test
    void reviewThatWouldFallOnTheExamDayMovesToTheDayBefore() {
        assertEquals(7, cap(8, 5, MONDAY_NOON, EXAM)); // 20. 10. -> 19. 10.
        assertEquals(7, cap(8, 3, MONDAY_NOON, EXAM));
    }

    @Test
    void reviewBeforeTheExamIsUnchanged() {
        assertEquals(3, cap(3, 3, MONDAY_NOON, EXAM));
        assertEquals(7, cap(7, 5, MONDAY_NOON, EXAM));
    }

    @Test
    void withoutExamDateOrOnAndAfterItNothingChanges() {
        assertEquals(15, cap(15, 3, MONDAY_NOON, null));
        assertEquals(15, cap(15, 3, Instant.parse("2026-10-20T08:00:00Z"), EXAM));
        assertEquals(15, cap(15, 3, Instant.parse("2026-10-25T08:00:00Z"), EXAM));
    }

    @Test
    void dayBeforeTheExamStillGetsAtLeastOneDay() {
        assertEquals(1, cap(6, 3, Instant.parse("2026-10-19T08:00:00Z"), EXAM));
        assertEquals(6, cap(6, 5, Instant.parse("2026-10-19T08:00:00Z"), EXAM));
    }

    @Test
    void dayIsTakenInZagrebTime() {
        // 18. 10. u 23:30 UTC je već 19. 10. u Zagrebu: dan prije ispita
        assertEquals(1, cap(6, 3, Instant.parse("2026-10-18T23:30:00Z"), EXAM));
    }
}
