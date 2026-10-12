package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OwnCourseMaterialServiceTest {

    @Test
    void estimateIsShownAsShareOfMonthlyQuota() {
        // fizika: 252 stranice × 0,016 $ ≈ 4,03 $ od 10 $ kvote
        assertEquals(41, OwnCourseMaterialService.estimatedPercent(252, BigDecimal.TEN));
        assertEquals(1, OwnCourseMaterialService.estimatedPercent(1, BigDecimal.TEN));
        assertEquals(0, OwnCourseMaterialService.estimatedPercent(0, BigDecimal.TEN));
        assertEquals(100, OwnCourseMaterialService.estimatedPercent(10, BigDecimal.ZERO));
    }

    @Test
    void tooManyPagesSaysHowManyAreOver() {
        assertEquals("Ovaj materijal ima 80 str., pa bi predmet imao 320 stranica; dopušteno je najviše 300 (20 previše).",
                OwnCourseMaterialService.tooManyPages(80, 320));
    }

    @Test
    void fileSizeLimits() {
        assertThrows(ResponseStatusException.class, () -> OwnCourseMaterialService.checkFileSize(0));
        assertThrows(ResponseStatusException.class, () -> OwnCourseMaterialService.checkFileSize(100L * 1024 * 1024 + 1));
        assertDoesNotThrow(() -> OwnCourseMaterialService.checkFileSize(100L * 1024 * 1024));
    }

    @Test
    void statePrefersProblemsOverProgress() {
        assertEquals("radi", OwnCourseMaterialService.state(Map.of("read_page/running", 2)));
        assertEquals("neuspjelo", OwnCourseMaterialService.state(Map.of("read_page/failed", 1, "read_page/waiting", 4)));
        assertEquals("ceka_kvotu", OwnCourseMaterialService.state(Map.of("plan_points/waiting_quota", 5)));
        assertEquals("pauzirano", OwnCourseMaterialService.state(Map.of("plan_edges/paused", 5)));
    }

    @Test
    void planStepIsTheFirstPhaseWithUnfinishedSteps() {
        assertEquals(-1, OwnCourseMaterialService.planStep(Map.of("read_page/running", 3)));
        assertEquals(0, OwnCourseMaterialService.planStep(Map.of("read_page/done", 3, "plan_scope/running", 1)));
        assertEquals(0, OwnCourseMaterialService.planStep(Map.of("plan_scope/done", 1, "plan_exam_tasks/waiting", 2)));
        assertEquals(1, OwnCourseMaterialService.planStep(Map.of("plan_scope/done", 1, "plan_points/running", 4, "plan_points/done", 3)));
        assertEquals(2, OwnCourseMaterialService.planStep(Map.of("plan_scope/done", 1, "plan_points/done", 7, "plan_dedupe/done", 1, "plan_edges/waiting", 2)));
        assertEquals(5, OwnCourseMaterialService.planStep(Map.of("plan_finalize/done", 1)));
    }
}
