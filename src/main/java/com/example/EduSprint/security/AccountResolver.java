package com.example.EduSprint.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds which account a login belongs to: first by the login's subject, then by email. A login seen for the
 * first time on an existing account is linked to it, so the same real email via Google and Apple is one
 * account, and a login keeps its account if the email on the token later changes. A hidden Apple email with
 * no account yet stays unlinked until /account/create links it.
 */
@Component
public class AccountResolver {

    private final AccountLogins logins;
    private final Map<String, String> emailBySubject = new ConcurrentHashMap<>();

    public AccountResolver(AccountLogins logins) {
        this.logins = logins;
    }

    /** The email of the account to act as (the token's email when no account is linked yet). */
    public String accountEmail(String subject, String tokenEmail) {
        if (subject == null || subject.isBlank()) return tokenEmail;
        String cached = emailBySubject.get(subject);
        if (cached != null) return cached;

        String email = logins.emailForSubject(subject).orElse(null);
        if (email == null) {
            Long accountId = logins.accountIdForEmail(tokenEmail).orElse(null);
            if (accountId == null) return tokenEmail;   // no account yet: nothing to link or cache
            logins.link(subject, accountId);
            email = tokenEmail;
        }
        emailBySubject.put(subject, email);
        return email;
    }

    /** Links a login to a just-created account. */
    public void linkNewAccount(String subject, long accountId, String email) {
        if (subject == null || subject.isBlank()) return;
        logins.link(subject, accountId);
        emailBySubject.put(subject, email);
    }

    /** Drops cached logins of an account (after it is deleted). */
    public void forgetEmail(String email) {
        emailBySubject.values().removeIf(email::equals);
    }
}
