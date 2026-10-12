package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlanCheckRepairStepTest {

    // 0 ← 1 ← 2 (lekcija 2 treba 1, 1 treba 0); lekcija 3 nema preduvjeta
    private static List<Set<Integer>> chain() {
        return new ArrayList<>(List.of(new HashSet<>(), new HashSet<>(Set.of(0)), new HashSet<>(Set.of(1)), new HashSet<>()));
    }

    private static PlanCheckRepairStep.Verdict verdict(int lesson, List<String> taughtBy, List<Integer> teachers) {
        return PlanCheckRepairStep.judge(lesson, taughtBy, teachers, chain()).verdict();
    }

    @Test
    void conceptFromAPrerequisiteOrTheLessonItselfIsFine() {
        assertEquals(PlanCheckRepairStep.Verdict.OK, verdict(2, List.of("11"), List.of(0))); // tranzitivni preduvjet
        assertEquals(PlanCheckRepairStep.Verdict.OK, verdict(2, List.of("21"), List.of(2)));
        assertEquals(PlanCheckRepairStep.Verdict.OK, verdict(2, List.of("predznanje"), List.of()));
        assertEquals(PlanCheckRepairStep.Verdict.OK, verdict(2, List.of("99", "11"), List.of(3, 0))); // jedna od stavki je poznata
    }

    @Test
    void earlierLessonThatIsNotAPrerequisiteBecomesOne() {
        PlanCheckRepairStep.Judgement j = PlanCheckRepairStep.judge(3, List.of("11"), List.of(1, 0), chain());
        assertEquals(PlanCheckRepairStep.Verdict.ADD_PREREQUISITE, j.verdict());
        assertEquals(0, j.teacher()); // najranija lekcija koja uči pojam
    }

    @Test
    void laterLessonIsAViolation() {
        PlanCheckRepairStep.Judgement j = PlanCheckRepairStep.judge(0, List.of("31"), List.of(3), chain());
        assertEquals(PlanCheckRepairStep.Verdict.LATER, j.verdict());
        assertEquals(3, j.teacher());
    }

    @Test
    void conceptTaughtNowhereIsAGap() {
        assertEquals(PlanCheckRepairStep.Verdict.NOWHERE, verdict(1, List.of("nigdje"), List.of()));
    }

    @Test
    void movingALessonAfterItsTeacherMustNotCloseACycle() {
        assertTrue(PlanRepair.createsCycle(chain(), 0, 2));  // 2 već (tranzitivno) treba 0
        assertFalse(PlanRepair.createsCycle(chain(), 0, 3)); // 3 ne ovisi o 0: 0 može ići iza 3
    }

    @Test
    void addedPrerequisiteMovesTheLessonBehindItsTeacher() {
        List<Set<Integer>> pre = chain();
        pre.get(0).add(3); // lekcija 0 sada treba 3
        PlanRepair.Order order = PlanRepair.topoOrder(pre);
        assertEquals(List.of(3, 0, 1, 2), order.order());
        assertTrue(order.backwards().isEmpty());
    }
}
