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
    void readingStatePrefersProblemsOverProgress() {
        assertEquals("radi", OwnCourseMaterialService.readingState(Map.of("read_page/running", 2), 3, 10));
        assertEquals("neuspjelo", OwnCourseMaterialService.readingState(Map.of("read_page/failed", 1, "read_page/waiting", 4), 3, 10));
        assertEquals("ceka_kvotu", OwnCourseMaterialService.readingState(Map.of("read_page/waiting_quota", 5), 3, 10));
        assertEquals("pauzirano", OwnCourseMaterialService.readingState(Map.of("read_page/paused", 5), 3, 10));
        assertEquals("procitano", OwnCourseMaterialService.readingState(Map.of("read_page/done", 10), 10, 10));
    }
}
