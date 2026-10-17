package com.example.EduSprint.security;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountResolverTest {

    /** In-memory stand-in for the account_login table. */
    static class FakeLogins implements AccountLogins {
        final Map<String, String> accountEmailByEmail = new HashMap<>();
        final Map<String, Long> accountIdByEmail = new HashMap<>();
        final Map<String, Long> accountIdBySubject = new HashMap<>();
        int lookups;

        void account(long id, String email) {
            accountIdByEmail.put(email, id);
            accountEmailByEmail.put(email, email);
        }

        @Override
        public Optional<String> emailForSubject(String subject) {
            lookups++;
            Long id = accountIdBySubject.get(subject);
            return accountIdByEmail.entrySet().stream().filter(e -> e.getValue().equals(id)).map(Map.Entry::getKey).findFirst();
        }

        @Override
        public Optional<Long> accountIdForEmail(String email) {
            return Optional.ofNullable(accountIdByEmail.get(email));
        }

        @Override
        public void link(String subject, long accountId) {
            accountIdBySubject.putIfAbsent(subject, accountId);
        }

        @Override
        public List<String> subjectsOf(long accountId) {
            return accountIdBySubject.entrySet().stream().filter(e -> e.getValue() == accountId).map(Map.Entry::getKey).toList();
        }
    }

    private final FakeLogins logins = new FakeLogins();
    private final AccountResolver resolver = new AccountResolver(logins);

    @Test
    void existingAccountIsLinkedToTheLoginOnFirstUse() {
        logins.account(7, "ana@gmail.com");

        assertEquals("ana@gmail.com", resolver.accountEmail("google-oauth2|1", "ana@gmail.com"));
        assertEquals(7L, logins.accountIdBySubject.get("google-oauth2|1"));
    }

    @Test
    void aLinkedLoginKeepsItsAccountWhenTheTokenEmailChanges() {
        // The student changed their Google address; the login (sub) still points at the old account.
        logins.account(7, "ana@gmail.com");
        logins.link("google-oauth2|1", 7);

        assertEquals("ana@gmail.com", resolver.accountEmail("google-oauth2|1", "ana.novi@gmail.com"));
    }

    @Test
    void appleWithTheSameRealEmailJoinsTheGoogleAccount() {
        logins.account(7, "ana@gmail.com");
        resolver.accountEmail("google-oauth2|1", "ana@gmail.com");

        assertEquals("ana@gmail.com", resolver.accountEmail("apple|9", "ana@gmail.com"));
        assertEquals(7L, logins.accountIdBySubject.get("apple|9"));
    }

    @Test
    void hiddenAppleEmailWithoutAnAccountStaysUnlinkedUntilTheAccountIsCreated() {
        assertEquals("x1y2@privaterelay.appleid.com", resolver.accountEmail("apple|9", "x1y2@privaterelay.appleid.com"));
        assertTrue(logins.accountIdBySubject.isEmpty());
    }

    @Test
    void resolvedLoginsAreCached() {
        logins.account(7, "ana@gmail.com");
        resolver.accountEmail("google-oauth2|1", "ana@gmail.com");
        int before = logins.lookups;

        resolver.accountEmail("google-oauth2|1", "ana@gmail.com");
        resolver.accountEmail("google-oauth2|1", "ana@gmail.com");

        assertEquals(before, logins.lookups);
    }
}
