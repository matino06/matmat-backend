package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Sažeto ponavljanje: do datuma ispita sljedeće ponavljanje je najkasnije dan prije ispita.
class ExamDateCapTest {

    private static final Instant MONDAY_NOON = Instant.parse("2026-10-12T10:00:00Z"); // 12. 10. u Zagrebu
    private static final LocalDate EXAM = LocalDate.of(2026, 10, 20);

    @Test
    void intervalThatWouldPassTheExamEndsTheDayBefore() {
        assertEquals(7, AccountObjectiveService.capToExam((short) 15, MONDAY_NOON, EXAM)); // 19. 10.
    }

    @Test
    void shorterIntervalIsUnchanged() {
        assertEquals(3, AccountObjectiveService.capToExam((short) 3, MONDAY_NOON, EXAM));
    }

    @Test
    void withoutExamDateOrOnAndAfterItNothingChanges() {
        assertEquals(15, AccountObjectiveService.capToExam((short) 15, MONDAY_NOON, null));
        assertEquals(15, AccountObjectiveService.capToExam((short) 15, Instant.parse("2026-10-20T08:00:00Z"), EXAM));
        assertEquals(15, AccountObjectiveService.capToExam((short) 15, Instant.parse("2026-10-25T08:00:00Z"), EXAM));
    }

    @Test
    void dayBeforeTheExamStillGetsAtLeastOneDay() {
        assertEquals(1, AccountObjectiveService.capToExam((short) 6, Instant.parse("2026-10-19T08:00:00Z"), EXAM));
    }

    @Test
    void dayIsTakenInZagrebTime() {
        // 18. 10. u 23:30 UTC je već 19. 10. u Zagrebu: dan prije ispita
        assertEquals(1, AccountObjectiveService.capToExam((short) 6, Instant.parse("2026-10-18T23:30:00Z"), EXAM));
    }
}
