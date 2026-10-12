package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BuildQuotaServiceTest {

    @Test
    void quotaRunsOutWhenSpendReachesTheLimit() {
        assertTrue(BuildQuotaService.hasQuota(new BigDecimal("9.99"), BigDecimal.TEN));
        assertFalse(BuildQuotaService.hasQuota(BigDecimal.TEN, BigDecimal.TEN));
        assertFalse(BuildQuotaService.hasQuota(new BigDecimal("10.04"), BigDecimal.TEN));
        assertFalse(BuildQuotaService.hasQuota(BigDecimal.ZERO, null));
    }

    @Test
    void percentIsRoundedDownAndCappedAt100() {
        assertEquals(0, BuildQuotaService.usedPercent(BigDecimal.ZERO, BigDecimal.TEN));
        assertEquals(34, BuildQuotaService.usedPercent(new BigDecimal("3.499"), BigDecimal.TEN));
        assertEquals(99, BuildQuotaService.usedPercent(new BigDecimal("9.999"), BigDecimal.TEN));
        assertEquals(100, BuildQuotaService.usedPercent(new BigDecimal("10.04"), BigDecimal.TEN));
        assertEquals(100, BuildQuotaService.usedPercent(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void monthStartsAtMidnightInZagreb() {
        ZonedDateTime start = BuildQuotaService.monthStart(LocalDate.of(2026, 10, 12)).atZone(java.time.ZoneId.of("Europe/Zagreb"));
        assertEquals(LocalDate.of(2026, 10, 1), start.toLocalDate());
        assertEquals(0, start.getHour());
    }

    @Test
    void quotaResetsOnTheFirstOfNextMonth() {
        assertEquals(LocalDate.of(2026, 11, 1), BuildQuotaService.resetsOn(LocalDate.of(2026, 10, 12)));
        assertEquals(LocalDate.of(2027, 1, 1), BuildQuotaService.resetsOn(LocalDate.of(2026, 12, 31)));
    }
}
