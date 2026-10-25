package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AccountObjective;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.repository.AccountObjectiveRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Lekcija je na ponavljanju tek kad ju je učenik već ocijenio.
class ReviewStateTest {

    private final AccountObjectiveRepository repository = mock(AccountObjectiveRepository.class);
    private final AccountObjectiveService service = new AccountObjectiveService(repository, null, null);
    private final Account account = new Account();
    private final LearningObjective objective = new LearningObjective();

    private void state(Short lastGrade) {
        AccountObjective accountObjective = new AccountObjective();
        accountObjective.setLastGrade(lastGrade);
        when(repository.findByAccountAndObjective(account, objective)).thenReturn(Optional.of(accountObjective));
    }

    @Test
    void neverRatedIsNew() {
        state(null);
        assertFalse(service.isReview(account, objective));
    }

    @Test
    void ratedBeforeIsAReview() {
        state((short) 3);
        assertTrue(service.isReview(account, objective));
    }

    @Test
    void lapsedIsStillAReview() {
        state((short) 1);
        assertTrue(service.isReview(account, objective));
    }

    @Test
    void withoutStateIsNew() {
        when(repository.findByAccountAndObjective(account, objective)).thenReturn(Optional.empty());
        assertFalse(service.isReview(account, objective));
    }
}
