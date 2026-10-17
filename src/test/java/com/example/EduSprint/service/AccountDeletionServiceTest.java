package com.example.EduSprint.service;

import com.example.EduSprint.security.AccountLogins;
import com.example.EduSprint.security.AccountResolver;
import com.example.EduSprint.storage.MaterialStorage;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    private final AccountLogins logins = mock(AccountLogins.class);
    private final AccountResolver resolver = mock(AccountResolver.class);
    private final AccountDataDeleter rows = mock(AccountDataDeleter.class);
    private final MaterialStorage storage = mock(MaterialStorage.class);
    private final Auth0Users auth0 = mock(Auth0Users.class);
    private final AppleTokens apple = mock(AppleTokens.class);
    private final EmailService email = mock(EmailService.class);
    private final AccountDeletionService service = new AccountDeletionService(logins, resolver, rows, storage, auth0, apple, email);

    AccountDeletionServiceTest() {
        when(logins.accountIdForEmail("ana@gmail.com")).thenReturn(Optional.of(7L));
        when(logins.subjectsOf(7L)).thenReturn(List.of("google-oauth2|1", "apple|2"));
        when(rows.deleteRows(7L)).thenReturn(List.of(31L, 32L));
        when(auth0.configured()).thenReturn(true);
        when(apple.configured()).thenReturn(true);
    }

    @Test
    void appleIsRevokedFirstThenRowsThenFilesLoginsAndEmail() {
        service.delete("ana@gmail.com", "apple|2", "apple-code");

        InOrder order = inOrder(apple, rows, storage, auth0, email);
        order.verify(apple).revoke("apple-code");
        order.verify(rows).deleteRows(7L);
        order.verify(storage).deleteAccountFolder(7L);
        order.verify(storage).deleteAnswerImages(31L);
        order.verify(storage).deleteAnswerImages(32L);
        order.verify(auth0).delete(Set.of("google-oauth2|1", "apple|2"));
        order.verify(email).sendAccountDeletedEmail("ana@gmail.com");
        verify(resolver).forgetEmail("ana@gmail.com");
    }

    @Test
    void theRequestsLoginIsDeletedEvenIfItWasNeverLinked() {
        when(logins.subjectsOf(7L)).thenReturn(List.of());

        service.delete("ana@gmail.com", "google-oauth2|9", null);

        verify(auth0).delete(Set.of("google-oauth2|9"));
        verify(apple, never()).revoke(anyString());
    }

    @Test
    void unknownAccountIs404AndNothingIsDeleted() {
        assertThrows(AccountNotFoundException.class, () -> service.delete("nobody@gmail.com", "google-oauth2|1", "apple-code"));

        verify(apple, never()).revoke(anyString());
        verify(rows, never()).deleteRows(anyLong());
        verify(auth0, never()).delete(any());
    }

    @Test
    void failedAppleRevokeStillDeletesTheAccount() {
        doThrow(new IllegalStateException("invalid_grant")).when(apple).revoke("apple-code");

        service.delete("ana@gmail.com", "apple|2", "apple-code");

        verify(rows).deleteRows(7L);
        verify(email).sendAccountDeletedEmail("ana@gmail.com");
    }

    @Test
    void failuresAfterTheRowsAreGoneDontFailTheRequest() {
        doThrow(new RuntimeException("S3 down")).when(storage).deleteAccountFolder(7L);
        doThrow(new RuntimeException("Auth0 down")).when(auth0).delete(any());
        doThrow(new IllegalStateException("Resend down")).when(email).sendAccountDeletedEmail(anyString());

        service.delete("ana@gmail.com", "apple|2", null);

        verify(storage).deleteAnswerImages(31L);
        verify(storage).deleteAnswerImages(32L);
    }

    @Test
    void failedRowDeletionStopsBeforeAnythingElse() {
        when(rows.deleteRows(7L)).thenThrow(new RuntimeException("foreign key"));

        assertThrows(RuntimeException.class, () -> service.delete("ana@gmail.com", "apple|2", null));

        verify(storage, never()).deleteAccountFolder(anyLong());
        verify(auth0, never()).delete(any());
        verify(email, never()).sendAccountDeletedEmail(anyString());
    }

    @Test
    void missingConfigurationSkipsAuth0AndApple() {
        when(auth0.configured()).thenReturn(false);
        when(apple.configured()).thenReturn(false);

        service.delete("ana@gmail.com", "apple|2", "apple-code");

        verify(apple, never()).revoke(anyString());
        verify(auth0, never()).delete(any());
        verify(rows).deleteRows(7L);
    }
}
