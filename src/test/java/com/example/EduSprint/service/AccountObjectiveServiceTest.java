package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AccountObjective;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.SolvedTask;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.AccountObjectiveRepository;
import com.example.EduSprint.repository.SolvedTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountObjectiveServiceTest {

    private static final Instant DAY0 = Instant.parse("2026-10-01T10:00:00Z");

    private final AccountObjectiveRepository accountObjectives = mock(AccountObjectiveRepository.class);
    private final SolvedTaskRepository solvedTasks = mock(SolvedTaskRepository.class);
    private final AccountObjectiveService service = new AccountObjectiveService(accountObjectives, null, solvedTasks);

    private final Account account = new Account();
    private final LearningObjective objective = new LearningObjective();
    private final Task task = new Task();
    private final AccountObjective state = new AccountObjective();
    private final List<SolvedTask> history = new ArrayList<>();

    AccountObjectiveServiceTest() {
        task.setObjective(objective);
        state.setAccount(account);
        state.setObjective(objective);
        state.setI((short) 0);
        state.setN((short) 0);
        state.setLastSolvedDate(DAY0);
        when(accountObjectives.findByAccountAndObjective(account, objective)).thenReturn(Optional.of(state));
        when(solvedTasks.findObjectiveHistory(any(), any())).thenReturn(history);
    }

    private static Instant day(int days) {
        return DAY0.plus(Duration.ofDays(days));
    }

    private SolvedTask solved(int grade, Instant at) {
        return new SolvedTask(account, task, (short) grade, at.minusSeconds(60), at, null, null, null, (short) 2);
    }

    /** Ocjena kakvu sprema kontroler: najprije u povijest, zatim u stanje lekcije. */
    private void rate(int grade, Instant at) {
        SolvedTask solved = solved(grade, at);
        history.add(solved);
        service.updateAccountObjective(solved);
    }

    @Test
    void newLessonStartsFromTheFirstRating() {
        rate(Fsrs.GOOD, DAY0);
        assertEquals((short) 2, state.getI());
        assertEquals((short) 1, state.getN());
        assertEquals((short) Fsrs.GOOD, state.getLastGrade());
        assertEquals(2.3065, state.getStability(), 1e-4);
    }

    @Test
    void firstRatingEasyCountsAsTwoSuccesses() {
        rate(Fsrs.EASY, DAY0);
        assertEquals((short) 8, state.getI());
        assertEquals((short) 2, state.getN());
    }

    @Test
    void againResetsTheStreak() {
        rate(Fsrs.GOOD, DAY0);
        rate(Fsrs.GOOD, day(2));
        rate(Fsrs.AGAIN, day(13));
        assertEquals((short) 0, state.getN());
        assertEquals((short) Fsrs.AGAIN, state.getLastGrade());
    }

    @Test
    void storedStateMatchesTheReferenceSequence() {
        rate(Fsrs.GOOD, DAY0);
        rate(Fsrs.GOOD, day(2));
        rate(Fsrs.GOOD, day(13));
        assertEquals(46.280217, state.getStability(), 1e-3);
        assertEquals((short) 46, state.getI());
        assertEquals((short) 3, state.getN());
    }

    @Test
    void lessonRatedBeforeFsrsIsReplayedFromItsHistory() {
        // dvije ocjene iz vremena SM-2: stanje ima samo zadnju ocjenu, bez S i D
        history.add(solved(Fsrs.GOOD, DAY0));
        history.add(solved(Fsrs.GOOD, day(2)));
        state.setLastGrade((short) Fsrs.GOOD);
        state.setN((short) 2);
        state.setI((short) 6);
        state.setLastSolvedDate(day(2));

        rate(Fsrs.GOOD, day(13));

        Fsrs.Memory expected = Fsrs.review(Fsrs.review(Fsrs.first(Fsrs.GOOD), Fsrs.GOOD, 2), Fsrs.GOOD, 11);
        assertEquals(expected.stability(), state.getStability(), 1e-4);
        assertEquals(expected.difficulty(), state.getDifficulty(), 1e-4);
        assertEquals((short) 3, state.getN());
    }

    @Test
    void replayIgnoresTheRatingBeingSaved() {
        history.add(solved(Fsrs.GOOD, DAY0));
        state.setLastGrade((short) Fsrs.GOOD);
        rate(Fsrs.GOOD, day(2));
        assertEquals(10.964332, state.getStability(), 1e-3);
    }

    @Test
    void elapsedDaysAreZagrebCalendarDays() {
        // 23:30 i 00:30 u Zagrebu: različiti dani, iako je prošao samo sat
        Instant evening = Instant.parse("2026-10-12T21:30:00Z");
        Instant afterMidnight = Instant.parse("2026-10-12T22:30:00Z");
        Fsrs.Memory first = Fsrs.first(Fsrs.GOOD);
        assertEquals(Fsrs.review(first, Fsrs.GOOD, 1).stability(),
                AccountObjectiveService.next(first, Fsrs.GOOD, evening, afterMidnight).stability(), 1e-9);
        assertEquals(Fsrs.review(first, Fsrs.GOOD, 0).stability(),
                AccountObjectiveService.next(first, Fsrs.GOOD, evening, evening.plusSeconds(600)).stability(), 1e-9);
    }

    @Test
    void replayOfNoHistoryIsANewLesson() {
        assertNull(AccountObjectiveService.replay(List.of()));
    }

    @Test
    void previewGivesOneIntervalPerGrade() {
        assertArrayEquals(new short[]{1, 1, 2, 8}, service.previewIntervals(account, objective));
    }
}
