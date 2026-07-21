package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountObjectiveServiceTest {

    @Test
    void onTimeReviewKeepsClassicSm2Growth() {
        // elapsedDays == oldI → classic round(oldI * ef)
        assertEquals((short) 50, AccountObjectiveService.computeEarlyAwareInterval((short) 20, 2.5f, 20));
    }

    @Test
    void lateReviewKeepsClassicSm2Growth() {
        // elapsedDays > oldI → still classic round(oldI * ef)
        assertEquals((short) 50, AccountObjectiveService.computeEarlyAwareInterval((short) 20, 2.5f, 40));
    }

    @Test
    void earlyReviewScalesGrowthProportionally() {
        // I=20, ef=2.5, full would be 50 (+30). After 2 days: 20 + 0.10*30 = 23
        assertEquals((short) 23, AccountObjectiveService.computeEarlyAwareInterval((short) 20, 2.5f, 2));
        // After 10 days: 20 + 0.5*30 = 35
        assertEquals((short) 35, AccountObjectiveService.computeEarlyAwareInterval((short) 20, 2.5f, 10));
    }

    @Test
    void zeroElapsedKeepsCurrentInterval() {
        // No time passed → no ballooning, interval stays the same
        assertEquals((short) 20, AccountObjectiveService.computeEarlyAwareInterval((short) 20, 2.5f, 0));
    }

    @Test
    void neverShrinksBelowCurrentInterval() {
        // Even with a low ef, an early success must not reduce the interval below oldI
        short oldI = 20;
        short result = AccountObjectiveService.computeEarlyAwareInterval(oldI, 1.3f, 1);
        assertTrue(result >= oldI, "interval must never shrink below oldI, was " + result);
    }
}
