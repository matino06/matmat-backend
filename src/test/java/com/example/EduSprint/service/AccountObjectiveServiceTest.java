package com.example.EduSprint.service;

import com.example.EduSprint.entity.AccountObjective;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

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

    private static AccountObjective state(float ef, int i, int n, Short lastQ, Instant lastSolved) {
        AccountObjective ao = new AccountObjective();
        ao.setEf(ef);
        ao.setI((short) i);
        ao.setN((short) n);
        ao.setLastQ(lastQ);
        ao.setLastSolvedDate(lastSolved);
        return ao;
    }

    @Test
    void failedRatingsAlwaysComeBackTomorrow() {
        Instant now = Instant.now();
        AccountObjective ao = state(2.5f, 20, 4, (short) 5, now.minus(Duration.ofDays(20)));
        for (short q = 0; q <= 2; q++) {
            assertEquals((short) 1, AccountObjectiveService.nextInterval(ao, q, now));
        }
    }

    @Test
    void newObjectiveRatedPerfectJumpsToSixDays() {
        Instant now = Instant.now();
        AccountObjective ao = state(2.5f, 0, 0, null, now);
        assertEquals((short) 6, AccountObjectiveService.nextInterval(ao, (short) 5, now));
        assertEquals((short) 1, AccountObjectiveService.nextInterval(ao, (short) 4, now));
        assertEquals((short) 1, AccountObjectiveService.nextInterval(ao, (short) 3, now));
    }

    @Test
    void secondSuccessfulReviewIsSixDays() {
        Instant now = Instant.now();
        AccountObjective ao = state(2.5f, 1, 1, (short) 4, now.minus(Duration.ofDays(1)));
        assertEquals((short) 6, AccountObjectiveService.nextInterval(ao, (short) 3, now));
        assertEquals((short) 6, AccountObjectiveService.nextInterval(ao, (short) 5, now));
    }

    @Test
    void laterReviewsUseEaseFactorAndElapsedTime() {
        Instant now = Instant.now();
        AccountObjective onTime = state(2.5f, 6, 2, (short) 5, now.minus(Duration.ofDays(6)));
        assertEquals((short) 15, AccountObjectiveService.nextInterval(onTime, (short) 4, now));

        AccountObjective early = state(2.5f, 6, 2, (short) 5, now.minus(Duration.ofDays(3)));
        assertEquals((short) 11, AccountObjectiveService.nextInterval(early, (short) 4, now));
    }
}
