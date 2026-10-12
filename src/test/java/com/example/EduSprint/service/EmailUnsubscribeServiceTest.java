package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.repository.AccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailUnsubscribeServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final EmailUnsubscribeService service =
            new EmailUnsubscribeService(accountRepository, "https://api.matmat.online/api", "test-secret");

    private static String tokenOf(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Test
    void urlPointsToThePublicEndpoint() {
        String url = service.unsubscribeUrl(42L);
        assertTrue(url.startsWith("https://api.matmat.online/api/email/unsubscribe/42/"), url);
    }

    @Test
    void tokenOfOneAccountDoesNotWorkForAnother() {
        String token = tokenOf(service.unsubscribeUrl(42L));
        assertTrue(service.isValid(42L, token));
        assertFalse(service.isValid(43L, token));
    }

    @Test
    void tokenFromAnotherSecretIsRejected() {
        EmailUnsubscribeService other = new EmailUnsubscribeService(accountRepository, "https://x", "other-secret");
        assertFalse(service.isValid(42L, tokenOf(other.unsubscribeUrl(42L))));
    }

    @Test
    void validLinkTurnsRemindersOff() {
        Account account = new Account();
        account.setLearningRemindersEnabled(true);
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));

        assertTrue(service.unsubscribe(42L, tokenOf(service.unsubscribeUrl(42L))));
        assertEquals(false, account.isLearningRemindersEnabled());
        verify(accountRepository).save(account);
    }

    @Test
    void invalidLinkChangesNothing() {
        assertFalse(service.unsubscribe(42L, "forged"));
        verify(accountRepository, never()).findById(42L);
    }
}
