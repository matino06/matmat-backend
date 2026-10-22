package com.example.EduSprint.service;

/**
 * FSRS-6 (Free Spaced Repetition Scheduler), prema py-fsrs 6.3 bez koraka učenja i bez nasumičnog pomaka intervala.
 * Za svaku lekciju pamti stabilnost S (dani do pada sjećanja na 90 %) i težinu D (1–10), a ocjena je
 * 1 = ponovno (nije riješio), 2 = teško, 3 = dobro, 4 = lako. Intervali su cijeli dani, a razmak se broji u
 * kalendarskim danima: 0 znači isti dan.
 */
final class Fsrs {

    static final int AGAIN = 1;
    static final int HARD = 2;
    static final int GOOD = 3;
    static final int EASY = 4;

    /** Zadani parametri FSRS-6. Kad bude dovoljno ponavljanja, zamijenit će ih parametri naučeni na MatMatu. */
    static final double[] W = {
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542};

    /** Lekcija se vraća kad bi vjerojatnost da je učenik zna pala na ovu vrijednost. */
    static final double DESIRED_RETENTION = 0.9;

    /** Interval stane u smallint stupac {@code account_objective.i}. */
    static final int MAX_INTERVAL = 3650;

    private static final double DECAY = -W[20];
    private static final double FACTOR = Math.pow(0.9, 1 / DECAY) - 1;
    private static final double MIN_STABILITY = 0.001;

    record Memory(double stability, double difficulty) {}

    private Fsrs() {}

    /** Stanje nakon prve ocjene lekcije. */
    static Memory first(int grade) {
        return new Memory(Math.max(W[grade - 1], MIN_STABILITY), clampDifficulty(initialDifficulty(grade)));
    }

    /** Stanje nakon ocjene {@code grade} kad je od prošlog rješavanja prošlo {@code elapsedDays} dana. */
    static Memory review(Memory memory, int grade, long elapsedDays) {
        double s = memory.stability();
        double d = memory.difficulty();
        double next;
        if (elapsedDays < 1) {
            next = shortTermStability(s, grade);
        } else {
            double r = retrievability(elapsedDays, s);
            next = grade == AGAIN ? forgetStability(d, s, r) : recallStability(d, s, r, grade);
        }
        return new Memory(Math.max(next, MIN_STABILITY), nextDifficulty(d, grade));
    }

    /** Vjerojatnost da učenik zna lekciju {@code elapsedDays} dana nakon rješavanja. */
    static double retrievability(long elapsedDays, double stability) {
        return Math.pow(1 + FACTOR * Math.max(0, elapsedDays) / stability, DECAY);
    }

    /** Dani do sljedećeg ponavljanja: kad vjerojatnost padne na {@link #DESIRED_RETENTION}. */
    static int interval(double stability) {
        double days = stability / FACTOR * (Math.pow(DESIRED_RETENTION, 1 / DECAY) - 1);
        return (int) Math.min(Math.max(Math.rint(days), 1), MAX_INTERVAL);
    }

    private static double initialDifficulty(int grade) {
        return W[4] - Math.exp(W[5] * (grade - 1)) + 1;
    }

    private static double clampDifficulty(double d) {
        return Math.min(Math.max(d, 1), 10);
    }

    private static double nextDifficulty(double d, int grade) {
        double delta = -W[6] * (grade - GOOD);
        double damped = d + (10 - d) * delta / 9;
        return clampDifficulty(W[7] * initialDifficulty(EASY) + (1 - W[7]) * damped);
    }

    private static double recallStability(double d, double s, double r, int grade) {
        double hardPenalty = grade == HARD ? W[15] : 1;
        double easyBonus = grade == EASY ? W[16] : 1;
        return s * (1 + Math.exp(W[8]) * (11 - d) * Math.pow(s, -W[9]) * (Math.exp((1 - r) * W[10]) - 1)
                * hardPenalty * easyBonus);
    }

    private static double forgetStability(double d, double s, double r) {
        double longTerm = W[11] * Math.pow(d, -W[12]) * (Math.pow(s + 1, W[13]) - 1) * Math.exp((1 - r) * W[14]);
        double shortTerm = s / Math.exp(W[17] * W[18]);
        return Math.min(longTerm, shortTerm);
    }

    private static double shortTermStability(double s, int grade) {
        double increase = Math.exp(W[17] * (grade - 3 + W[18])) * Math.pow(s, -W[19]);
        if (grade >= HARD) {
            increase = Math.max(increase, 1);
        }
        return s * increase;
    }
}
