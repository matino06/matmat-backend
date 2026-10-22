package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Dan prije ispita ponavlja se samo ono što učenik na dan ispita ne bi znao s 90 % ili bi palo na dan ispita.
class ExamDateCapTest {

    private static final Instant MONDAY_NOON = Instant.parse("2026-10-12T10:00:00Z"); // 12. 10. u Zagrebu
    private static final LocalDate EXAM = LocalDate.of(2026, 10, 20); // 8 dana kasnije

    private static short cap(int interval, double stability, Instant solvedAt, LocalDate exam) {
        return AccountObjectiveService.capToExam(interval, stability, solvedAt, exam);
    }

    @Test
    void lessonKnownOnTheExamDayKeepsItsInterval() {
        // S = 15: nakon 8 dana vjerojatnost je iznad 90 %
        assertEquals(15, cap(15, 15, MONDAY_NOON, EXAM));
    }

    @Test
    void lessonThatWouldBeForgottenByTheExamEndsTheDayBefore() {
        // pravilo gleda vjerojatnost na dan ispita: uz S = 6 nakon 8 dana pala bi ispod 90 %
        assertEquals(7, cap(15, 6, MONDAY_NOON, EXAM)); // 19. 10.
    }

    @Test
    void reviewThatWouldFallOnTheExamDayMovesToTheDayBefore() {
        assertEquals(7, cap(8, 8, MONDAY_NOON, EXAM)); // 20. 10. -> 19. 10.
    }

    @Test
    void reviewBeforeTheExamIsUnchanged() {
        assertEquals(3, cap(3, 3, MONDAY_NOON, EXAM));
        assertEquals(7, cap(7, 7, MONDAY_NOON, EXAM));
    }

    @Test
    void withoutExamDateOrOnAndAfterItNothingChanges() {
        assertEquals(15, cap(15, 1, MONDAY_NOON, null));
        assertEquals(15, cap(15, 1, Instant.parse("2026-10-20T08:00:00Z"), EXAM));
        assertEquals(15, cap(15, 1, Instant.parse("2026-10-25T08:00:00Z"), EXAM));
    }

    @Test
    void dayBeforeTheExamStillGetsAtLeastOneDay() {
        assertEquals(1, cap(1, 1, Instant.parse("2026-10-19T08:00:00Z"), EXAM));
        assertEquals(6, cap(6, 6, Instant.parse("2026-10-19T08:00:00Z"), EXAM));
    }

    @Test
    void dayIsTakenInZagrebTime() {
        // 18. 10. u 23:30 UTC je već 19. 10. u Zagrebu: dan prije ispita, pa interval 1 pada na ispit i ostaje 1
        assertEquals(1, cap(1, 1, Instant.parse("2026-10-18T23:30:00Z"), EXAM));
        // 18. 10. u 21:00 UTC je još 18. 10.: interval 2 pada na ispit, pa dolazi dan ranije
        assertEquals(1, cap(2, 2, Instant.parse("2026-10-18T21:00:00Z"), EXAM));
    }
}
