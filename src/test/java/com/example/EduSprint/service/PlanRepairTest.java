package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlanRepairTest {

    private static PlanRepair.Lesson lesson(long outcome, int outcomePos, int pos, String name, Long... items) {
        return new PlanRepair.Lesson(outcome, outcomePos, pos, name, name + ".", List.of(items));
    }

    @Test
    void smallLessonIsMergedWithTheSmallerNeighbourInTheSameOutcome() {
        List<PlanRepair.Lesson> out = PlanRepair.mergeSmall(List.of(
                lesson(1, 1, 1, "A", 1L, 2L, 3L, 4L, 5L),
                lesson(1, 1, 2, "B", 6L, 7L),
                lesson(1, 1, 3, "C", 8L, 9L, 10L, 11L)));
        assertEquals(List.of("A", "B + C"), out.stream().map(PlanRepair.Lesson::name).toList());
        assertEquals(List.of(6L, 7L, 8L, 9L, 10L, 11L), out.get(1).items());
    }

    @Test
    void smallLessonStaysWhenMergingWouldBeTooBig() {
        List<PlanRepair.Lesson> out = PlanRepair.mergeSmall(List.of(
                lesson(1, 1, 1, "A", 1L, 2L, 3L, 4L, 5L, 6L),
                lesson(1, 1, 2, "B", 7L)));
        assertEquals(2, out.size());
    }

    @Test
    void lessonsAreNotMergedAcrossOutcomes() {
        List<PlanRepair.Lesson> out = PlanRepair.mergeSmall(List.of(
                lesson(1, 1, 1, "A", 1L, 2L),
                lesson(2, 2, 1, "B", 3L, 4L)));
        assertEquals(2, out.size());
    }

    @Test
    void lessonComesAfterItsPrerequisiteEvenFromALaterOutcome() {
        // B (ishod 1) koristi stavku iz C (ishod 2), pa C mora doći prije B
        PlanRepair.Result r = PlanRepair.repair(List.of(
                        lesson(1, 1, 1, "A", 1L, 2L, 3L, 4L),
                        lesson(1, 1, 2, "B", 5L, 6L, 7L, 8L),
                        lesson(2, 2, 1, "C", 9L, 10L, 11L, 12L)),
                Map.of(5L, Set.of(9L), 9L, Set.of(1L)));
        assertEquals(List.of("A", "C", "B"), r.lessons().stream().map(PlanRepair.Lesson::name).toList());
        assertEquals(List.of(0), r.prerequisites().get(1)); // C treba A
        assertEquals(List.of(1), r.prerequisites().get(2)); // B treba C
        assertTrue(r.backwards().isEmpty());
    }

    @Test
    void cycleIsKeptAndReported() {
        PlanRepair.Result r = PlanRepair.repair(List.of(
                        lesson(1, 1, 1, "A", 1L, 2L, 3L, 4L),
                        lesson(1, 1, 2, "B", 5L, 6L, 7L, 8L)),
                Map.of(1L, Set.of(5L), 5L, Set.of(1L)));
        assertEquals(2, r.lessons().size());
        assertEquals(1, r.backwards().size());
    }
}
