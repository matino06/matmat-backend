package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.entity.*;
import com.example.EduSprint.repository.AccountObjectiveRepository;
import com.example.EduSprint.repository.AccountRepository;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AccountObjectiveService {

    public final AccountObjectiveRepository accountObjectiveRepository;
    public final AccountRepository accountRepository;
    private final SolvedTaskRepository solvedTaskRepository;

    public AccountObjectiveService(AccountObjectiveRepository accountObjectiveRepository, AccountRepository accountRepository, SolvedTaskRepository solvedTaskRepository) {
        this.accountObjectiveRepository = accountObjectiveRepository;
        this.accountRepository = accountRepository;
        this.solvedTaskRepository = solvedTaskRepository;
    }

    /**
     * Gives the account a new-lesson row for each lesson of these official courses it doesn't have yet: a course
     * gets its rows only once the student picks it or switches to it. Rows already there are kept as they are, so a
     * course taken off the list and picked again later still has its review history. Own courses are skipped; their
     * creator gets the rows when confirming the plan.
     */
    @Transactional
    public void addCourseObjectives(long accountId, Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return;
        accountObjectiveRepository.addOfficialCourseObjectives(accountId, courseIds);
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
        newSolvedTask.rememberBefore(accountObjective);
        short grade = newSolvedTask.getGrade();
        Instant solvedAt = newSolvedTask.getEndTime();
        Fsrs.Memory before = memoryBefore(accountObjective, solvedAt);
        Fsrs.Memory after = next(before, grade, accountObjective.getLastSolvedDate(), solvedAt);

        Course course = newSolvedTask.getCourse();
        accountObjective.setI(capToExam(Fsrs.interval(after.stability()), after.stability(), solvedAt,
                course == null ? null : course.getExamDate()));
        accountObjective.setN(nextStreak(before, accountObjective.getN(), grade));
        accountObjective.setStability((float) after.stability());
        accountObjective.setDifficulty((float) after.difficulty());
        accountObjective.setLastGrade(grade);
        accountObjective.setLastSolvedDate(solvedAt);

        try {
            accountObjectiveRepository.save(accountObjective);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Days until the objective comes back for each possible grade 1..4 if the user rated it right now (index
     * grade - 1). Same computation as {@link #updateAccountObjective}, so the rating picker can show the real
     * interval instead of a guess. Returns null if the account has no state for the objective.
     */
    public short[] previewIntervals(Account account, LearningObjective objective) {
        return previewIntervals(account, objective, Instant.now(), account.getCurrentCourse());
    }

    /** Isto kao {@link #previewIntervals(Account, LearningObjective)}, za ocjenu u trenutku {@code at}. */
    public short[] previewIntervals(Account account, LearningObjective objective, Instant at, Course course) {
        Optional<AccountObjective> accountObjective = accountObjectiveRepository.findByAccountAndObjective(account, objective);
        if (accountObjective.isEmpty()) {
            return null;
        }

        Fsrs.Memory before = memoryBefore(accountObjective.get(), at);
        short[] intervals = new short[4];
        for (int grade = Fsrs.AGAIN; grade <= Fsrs.EASY; grade++) {
            Fsrs.Memory after = next(before, grade, accountObjective.get().getLastSolvedDate(), at);
            intervals[grade - 1] = capToExam(Fsrs.interval(after.stability()), after.stability(), at,
                    course == null ? null : course.getExamDate());
        }
        return intervals;
    }

    /** Koliko se dugo nakon ocjene ona još smije promijeniti. */
    static final Duration REGRADE_WINDOW = Duration.ofDays(1);

    /**
     * Smije li se ocjena promijeniti: zapamtila je stanje lekcije prije sebe, nije starija od
     * {@link #REGRADE_WINDOW} i zadnja je ocjena te lekcije (na kasniju se ocjenu već nadovezalo njezino stanje).
     */
    public boolean canChangeGrade(SolvedTask solved, Instant now) {
        if (!solved.remembersBefore() || solved.getEndTime().isBefore(now.minus(REGRADE_WINDOW))) {
            return false;
        }
        List<SolvedTask> history = solvedTaskRepository.findObjectiveHistory(
                solved.getAccount().getAccountId(), solved.getTask().getObjective().getObjectiveId());
        return !history.isEmpty() && Objects.equals(history.get(history.size() - 1).getId(), solved.getId());
    }

    /**
     * Vrati lekciji stanje prije ove ocjene, kao da je još nije ocijenio. Nakon toga {@link #updateAccountObjective}
     * primijeni novu ocjenu kao da je ona bila prva.
     */
    @Transactional
    public void restoreBefore(SolvedTask solved) {
        accountObjectiveRepository.findByAccountAndObjective(solved.getAccount(), solved.getTask().getObjective())
                .ifPresent(accountObjective -> {
                    solved.restoreBefore(accountObjective);
                    accountObjectiveRepository.saveAndFlush(accountObjective);
                });
    }

    /**
     * Je li učenik ovu lekciju već ocijenio. Ako jest, lekcija je na ponavljanju: trebaju li mu koraci, zaboravio
     * ju je (a prvi put su koraci dio učenja). Interaktivna lekcija o tome ovisno predlaže ocjenu.
     */
    public boolean isReview(Account account, LearningObjective objective) {
        return accountObjectiveRepository.findByAccountAndObjective(account, objective)
                .map(accountObjective -> accountObjective.getLastGrade() != null)
                .orElse(false);
    }

    /**
     * FSRS stanje lekcije prije ocjene u trenutku {@code at}, ili null ako je još nije rješavao. Lekcija ocijenjena
     * prije FSRS-a nema S i D, pa se izračunaju iz njezine povijesti u solved_task (ocjene prije {@code at}).
     */
    private Fsrs.Memory memoryBefore(AccountObjective accountObjective, Instant at) {
        if (accountObjective.getStability() != null && accountObjective.getDifficulty() != null) {
            return new Fsrs.Memory(accountObjective.getStability(), accountObjective.getDifficulty());
        }
        List<SolvedTask> history = solvedTaskRepository.findObjectiveHistory(
                accountObjective.getAccount().getAccountId(), accountObjective.getObjective().getObjectiveId());
        return replay(history.stream().filter(solved -> solved.getEndTime().isBefore(at)).toList());
    }

    /** FSRS stanje nakon svih ocjena iz povijesti (najstarija prva), ili null ako povijesti nema. */
    static Fsrs.Memory replay(List<SolvedTask> history) {
        Fsrs.Memory memory = null;
        Instant last = null;
        for (SolvedTask solved : history) {
            if (solved.getGrade() == null) continue;
            memory = next(memory, solved.getGrade(), last, solved.getEndTime());
            last = solved.getEndTime();
        }
        return memory;
    }

    /** FSRS stanje nakon ocjene {@code grade} u {@code solvedAt}; {@code before} je null za prvu ocjenu. */
    static Fsrs.Memory next(Fsrs.Memory before, int grade, Instant lastSolved, Instant solvedAt) {
        if (before == null) {
            return Fsrs.first(grade);
        }
        return Fsrs.review(before, grade, ChronoUnit.DAYS.between(AppTime.dateOf(lastSolved), AppTime.dateOf(solvedAt)));
    }

    /**
     * Uzastopna uspješna rješavanja (n): uz „s mukom“ lekcija otključava one koje se na nju nadovezuju tek kad je n
     * barem 2 (pravilo je u LearningObjectiveRepository). Prva ocjena „lako“ odmah vrijedi kao dva.
     */
    static short nextStreak(Fsrs.Memory before, short n, int grade) {
        if (grade == Fsrs.AGAIN) return 0;
        if (before == null && grade == Fsrs.EASY) return 2;
        return (short) (n + 1);
    }

    /**
     * Na ispitu učenik treba znati lekciju barem s ovom vjerojatnošću. Ista je kao {@link Fsrs#DESIRED_RETENTION},
     * pa interval koji prelazi ispit već znači da je učenik zna i na dan ispita.
     */
    static final double EXAM_RETENTION = 0.9;

    /**
     * Datum ispita: dan prije ispita ponavlja se samo ono što bi inače bilo upitno na ispitu, ne sve lekcije.
     * <ul>
     *   <li>ponavljanje koje bi palo na dan ispita dolazi dan ranije, jer na dan ispita učenik možda ne stigne;</li>
     *   <li>lekcija kojoj bi ponavljanje palo nakon ispita, a vjerojatnost da je zna na dan ispita bila bi ispod
     *   {@link #EXAM_RETENTION}, dolazi najkasnije dan prije ispita;</li>
     *   <li>ostale zadržavaju FSRS interval.</li>
     * </ul>
     * Bez datuma, na dan ispita i nakon njega interval ostaje kakav jest.
     */
    static short capToExam(int interval, double stability, Instant solvedAt, LocalDate examDate) {
        if (examDate == null) return (short) interval;
        LocalDate solvedDay = AppTime.dateOf(solvedAt);
        if (!solvedDay.isBefore(examDate)) return (short) interval;
        long untilExam = ChronoUnit.DAYS.between(solvedDay, examDate);
        if (interval < untilExam) return (short) interval;
        if (interval > untilExam && Fsrs.retrievability(untilExam, stability) >= EXAM_RETENTION) return (short) interval;
        return (short) Math.max(1, untilExam - 1);
    }
}
