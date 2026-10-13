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
        accountObjective.setI(capToExam(nextInterval(accountObjective, q, newSolvedTaskEndTime),
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
            intervals[q] = capToExam(nextInterval(accountObjective.get(), q, now), now, course == null ? null : course.getExamDate());
        }
        return intervals;
    }

    /**
     * Datum ispita: dok je ispit pred nama, sljedeće ponavljanje dolazi najkasnije dan prije ispita
     * (sažeto ponavljanje). Bez datuma, na dan ispita i nakon njega SM-2 interval ostaje kakav jest.
     */
    static short capToExam(short interval, Instant solvedAt, LocalDate examDate) {
        if (examDate == null) return interval;
        LocalDate solvedDay = solvedAt.atZone(AppTime.ZONE).toLocalDate();
        if (!solvedDay.isBefore(examDate)) return interval;
        long untilDayBefore = ChronoUnit.DAYS.between(solvedDay, examDate.minusDays(1));
        return (short) Math.max(1, Math.min(interval, untilDayBefore));
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
        return computeEarlyAwareInterval(accountObjective.getI(), accountObjective.getEf(), elapsedDays);
    }

    /**
     * Computes the next SM-2 interval for a successful review, but scales the growth by how much
     * of the previous interval actually elapsed. This prevents the interval from ballooning when a
     * user reviews an objective early (e.g. via the map) — reviewing an I=20 objective after 2 days
     * should not stretch it to ~50 as if the full interval had passed. Reviews that happen on time
     * or late (elapsedDays >= oldI) keep the classic {@code round(oldI * ef)} behaviour.
     */
    static short computeEarlyAwareInterval(short oldI, float ef, long elapsedDays) {
        int fullI = Math.round(oldI * ef);
        if (oldI <= 0 || elapsedDays >= oldI) {
            return (short) fullI;                       // on time or late → normal SM-2
        }
        double ratio = (double) elapsedDays / oldI;     // 0..1
        int scaled = (int) Math.round(oldI + ratio * (fullI - oldI));
        return (short) Math.max(scaled, oldI);          // never shrink below the current interval
    }
}
