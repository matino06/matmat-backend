package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Vrijednosti su iz py-fsrs 6.3.2 (Scheduler bez koraka učenja i bez nasumičnog pomaka), da Java prati referentnu verziju.
class FsrsTest {

    private static final double EPS = 1e-4;

    private static void assertMemory(double stability, double difficulty, Fsrs.Memory memory) {
        assertEquals(stability, memory.stability(), EPS, "stabilnost");
        assertEquals(difficulty, memory.difficulty(), EPS, "težina");
    }

    @Test
    void firstRatingSetsInitialStabilityAndDifficulty() {
        assertMemory(0.212, 6.4133, Fsrs.first(Fsrs.AGAIN));
        assertMemory(1.2931, 5.112171, Fsrs.first(Fsrs.HARD));
        assertMemory(2.3065, 2.118104, Fsrs.first(Fsrs.GOOD));
        assertMemory(8.2956, 1.0, Fsrs.first(Fsrs.EASY));
    }

    @Test
    void firstIntervals() {
        assertEquals(1, Fsrs.interval(Fsrs.first(Fsrs.AGAIN).stability()));
        assertEquals(1, Fsrs.interval(Fsrs.first(Fsrs.HARD).stability()));
        assertEquals(2, Fsrs.interval(Fsrs.first(Fsrs.GOOD).stability()));
        assertEquals(8, Fsrs.interval(Fsrs.first(Fsrs.EASY).stability()));
    }

    @Test
    void goodOnTimeGrowsTheInterval() {
        Fsrs.Memory m = Fsrs.first(Fsrs.GOOD);
        m = Fsrs.review(m, Fsrs.GOOD, 2);
        assertMemory(10.964332, 2.111214, m);
        assertEquals(11, Fsrs.interval(m.stability()));
        m = Fsrs.review(m, Fsrs.GOOD, 11);
        assertMemory(46.280217, 2.104331, m);
        m = Fsrs.review(m, Fsrs.GOOD, 46);
        assertMemory(162.862202, 2.097455, m);
        assertEquals(163, Fsrs.interval(m.stability()));
    }

    @Test
    void hardAfterALongGap() {
        Fsrs.Memory m = Fsrs.review(Fsrs.first(Fsrs.EASY), Fsrs.HARD, 20);
        assertMemory(41.462893, 4.010609, m);
        assertEquals(41, Fsrs.interval(m.stability()));
    }

    @Test
    void againDropsStabilityAndRaisesDifficulty() {
        Fsrs.Memory m = Fsrs.review(Fsrs.first(Fsrs.GOOD), Fsrs.AGAIN, 2);
        assertMemory(0.60758, 7.394503, m);
        assertEquals(1, Fsrs.interval(m.stability()));
        m = Fsrs.review(m, Fsrs.GOOD, 1);
        assertMemory(2.400404, 7.382337, m);
    }

    @Test
    void sameDayReviewUsesShortTermStability() {
        Fsrs.Memory m = Fsrs.review(Fsrs.first(Fsrs.GOOD), Fsrs.GOOD, 0);
        assertMemory(2.3065, 2.111214, m);
        m = Fsrs.review(m, Fsrs.AGAIN, 0);
        assertMemory(0.775084, 7.392238, m);
    }

    @Test
    void earlyReviewGrowsLessThanOnTime() {
        Fsrs.Memory m = Fsrs.review(Fsrs.first(Fsrs.GOOD), Fsrs.GOOD, 11);
        assertMemory(26.132579, 2.111214, m);
        m = Fsrs.review(m, Fsrs.GOOD, 1);
        assertMemory(30.094703, 2.104331, m);
    }

    @Test
    void retrievabilityIsNinetyPercentAfterStabilityDays() {
        assertEquals(0.9094932559773545, Fsrs.retrievability(2, 2.3065), 1e-9);
        assertEquals(0.7743669167614039, Fsrs.retrievability(10, 2.3065), 1e-9);
        assertEquals(0.9, Fsrs.retrievability(30, 30), 1e-9);
    }

    @Test
    void intervalIsCappedToFitTheColumn() {
        assertEquals(Fsrs.MAX_INTERVAL, Fsrs.interval(1e6));
    }
}
