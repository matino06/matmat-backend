package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.entity.*;
import com.example.EduSprint.repository.AccountObjectiveRepository;
import com.example.EduSprint.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AccountObjectiveService {

    public final AccountObjectiveRepository accountObjectiveRepository;
    public final AccountRepository accountRepository;
    public final LearningObjectiveService learningObjectiveService;

    public AccountObjectiveService(AccountObjectiveRepository accountObjectiveRepository, AccountRepository accountRepository, LearningObjectiveService learningObjectiveService) {
        this.accountObjectiveRepository = accountObjectiveRepository;
        this.accountRepository = accountRepository;
        this.learningObjectiveService = learningObjectiveService;
    }

    @Transactional
    public boolean initializeAccountObjectives(Account account) {
        try {
            // lekcije vlastitih predmeta dobiva samo njihov kreator, pri potvrdi plana
            List<LearningObjective> objectives = learningObjectiveService.findOfficial();
            for (LearningObjective objective : objectives) {
                AccountObjective accountObjective = new AccountObjective();
                accountObjective.setAccount(account);
                accountObjective.setObjective(objective);
                accountObjective.setEf(2.5F);
                accountObjective.setI((short) 0);
                accountObjective.setN((short) 0);
                accountObjective.setLastSolvedDate(Instant.now());

                accountObjectiveRepository.save(accountObjective);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public boolean updateAccountObjective(SolvedTask newSolvedTask) {

        Account account = newSolvedTask.getAccount();
        Task task = newSolvedTask.getTask();
        LearningObjective objective = task.getObjective();

        Optional<AccountObjective> accountObjectiveOptional = accountObjectiveRepository.findByAccountAndObjective(account, objective);
        if (!accountObjectiveOptional.isPresent()) {
            return false;
        }

        AccountObjective accountObjective = accountObjectiveOptional.get();
        Float oldEF = accountObjective.getEf();
        Short oldN = accountObjective.getN();
        Short q = newSolvedTask.getQ();
        Short lastQ = accountObjective.getLastQ();
        Instant newSolvedTaskEndTime = newSolvedTask.getEndTime();

        Course course = newSolvedTask.getCourse();
        accountObjective.setI(capToExam(nextInterval(accountObjective, q, newSolvedTaskEndTime), q,
                newSolvedTaskEndTime, course == null ? null : course.getExamDate()));
        if (q >= 3) {
            if (lastQ == null && q.equals((short)5)) {
                accountObjective.setN((short)2);
            } else {
                accountObjective.setN(++oldN);
            }
        } else {
            accountObjective.setN((short) 0);
        }

        Float newEF = (float) (oldEF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)));
        newEF = (float) Math.max(newEF, 1.3);
        newEF = (float) Math.min(newEF, 2.5);

        accountObjective.setEf(newEF);
        accountObjective.setLastQ(q);
        accountObjective.setLastSolvedDate(newSolvedTaskEndTime);

        try {
            accountObjectiveRepository.save(accountObjective);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Days until the objective comes back for each possible rating q = 0..5 if the user rated it
     * right now. Same computation as {@link #updateAccountObjective}, so the rating picker can show
     * the real interval instead of a guess. Returns null if the account has no state for the objective.
     */
    public short[] previewIntervals(Account account, LearningObjective objective) {
        Optional<AccountObjective> accountObjective = accountObjectiveRepository.findByAccountAndObjective(account, objective);
        if (accountObjective.isEmpty()) {
            return null;
        }

        Instant now = Instant.now();
        Course course = account.getCurrentCourse();
        short[] intervals = new short[6];
        for (short q = 0; q <= 5; q++) {
            intervals[q] = capToExam(nextInterval(accountObjective.get(), q, now), q, now, course == null ? null : course.getExamDate());
        }
        return intervals;
    }

    /**
     * Datum ispita: dan prije ispita ponavlja se samo ono što bi inače bilo upitno na ispitu, ne sve lekcije.
     * <ul>
     *   <li>ponavljanje koje bi palo na dan ispita dolazi dan ranije, jer na dan ispita učenik možda ne stigne;</li>
     *   <li>lekcija ocijenjena s 3 ili manje kojoj bi ponavljanje palo nakon ispita dolazi najkasnije dan prije ispita;</li>
     *   <li>lekcija ocijenjena s 4 ili 5 kojoj ponavljanje pada nakon ispita zadržava SM-2 interval: po njemu je
     *   učenik zna i na dan ispita.</li>
     * </ul>
     * Bez datuma, na dan ispita i nakon njega SM-2 interval ostaje kakav jest.
     */
    static short capToExam(short interval, short q, Instant solvedAt, LocalDate examDate) {
        if (examDate == null) return interval;
        LocalDate solvedDay = solvedAt.atZone(AppTime.ZONE).toLocalDate();
        if (!solvedDay.isBefore(examDate)) return interval;
        long untilExam = ChronoUnit.DAYS.between(solvedDay, examDate);
        if (interval < untilExam) return interval;
        if (interval > untilExam && q > 3) return interval;
        return (short) Math.max(1, untilExam - 1);
    }

    /** SM-2 interval (in days) the objective gets after being rated q at the given time. */
    static short nextInterval(AccountObjective accountObjective, short q, Instant solvedAt) {
        if (q < 3) {
            return 1;
        }

        short oldN = accountObjective.getN();
        if (accountObjective.getLastQ() == null && q == 5) {
            return 6;
        } else if (oldN == 0) {
            return 1;
        } else if (oldN == 1) {
            return 6;
        }

        long elapsedDays = Math.max(0, Duration.between(accountObjective.getLastSolvedDate(), solvedAt).toDays());
        return computeElapsedAwareInterval(accountObjective.getI(), accountObjective.getEf(), elapsedDays, q);
    }

    /**
     * Computes the next SM-2 interval for a successful review from the time that actually elapsed, not only the
     * scheduled interval.
     * <ul>
     *   <li>Early (elapsedDays &lt; oldI): growth scales with how much of the interval elapsed, so reviewing an
     *   I=20 objective after 2 days (e.g. via the map) does not stretch it to ~50 as if the full interval had
     *   passed. Never shrinks below oldI.</li>
     *   <li>Late (elapsedDays &gt; oldI): the student remembered for longer than scheduled, so part of the delay
     *   counts towards the base, as in Anki's SM-2: q=5 all of it, q=4 half, q=3 none (it took effort, which
     *   says nothing about the longer gap). The delay counts up to {@link #MAX_LATE_BASE}×oldI, so coming back
     *   after months does not jump the interval to most of a year.</li>
     * </ul>
     * On time keeps the classic {@code round(oldI * ef)}.
     */
    static short computeElapsedAwareInterval(short oldI, float ef, long elapsedDays, short q) {
        int fullI = Math.round(oldI * ef);
        if (oldI <= 0) {
            return (short) fullI;
        }
        if (elapsedDays >= oldI) {
            long late = Math.min(elapsedDays, (long) MAX_LATE_BASE * oldI) - oldI;
            double base = oldI + late * lateCredit(q);
            return (short) Math.round(base * ef);
        }
        double ratio = (double) elapsedDays / oldI;     // 0..1
        int scaled = (int) Math.round(oldI + ratio * (fullI - oldI));
        return (short) Math.max(scaled, oldI);          // never shrink below the current interval
    }

    /** The late review's base counts the delay up to this many times the old interval. */
    static final int MAX_LATE_BASE = 3;

    /** How much of a late review's delay counts towards the next interval's base. */
    private static double lateCredit(short q) {
        return q >= 5 ? 1.0 : q == 4 ? 0.5 : 0.0;
    }
}
