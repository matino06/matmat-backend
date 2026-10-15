package com.example.EduSprint.security;

import java.util.Optional;

/**
 * Logins (Auth0 subjects: Google, Apple) linked to accounts. One account can have several logins.
 */
public interface AccountLogins {

    /** Email of the account this login belongs to, if it is linked. */
    Optional<String> emailForSubject(String subject);

    Optional<Long> accountIdForEmail(String email);

    /** Links the login to the account; does nothing if the login is already linked. */
    void link(String subject, long accountId);
}
