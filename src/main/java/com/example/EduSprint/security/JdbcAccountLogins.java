package com.example.EduSprint.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link AccountLogins} on the account_login table (sql/2026-10-14-racun-prijave.sql).
 *
 * <p>Plain JDBC on purpose: if the table is missing (backend deployed before the SQL ran), login linking is
 * skipped and accounts are found by email as before, instead of the app failing to start on schema validation.
 */
@Component
public class JdbcAccountLogins implements AccountLogins {

    private static final Logger log = LoggerFactory.getLogger(JdbcAccountLogins.class);

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAccountLogins(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> emailForSubject(String subject) {
        try {
            return jdbc.queryForList("""
                    SELECT a.email FROM account_login l JOIN account a ON a.account_id = l.account_id
                    WHERE l.subject = :subject""", Map.of("subject", subject), String.class).stream().findFirst();
        } catch (DataAccessException e) {
            log.warn("account_login lookup failed, falling back to email: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Long> accountIdForEmail(String email) {
        return jdbc.queryForList("SELECT account_id FROM account WHERE email = :email",
                Map.of("email", email), Long.class).stream().findFirst();
    }

    @Override
    public void link(String subject, long accountId) {
        try {
            jdbc.update("""
                    INSERT INTO account_login (subject, account_id) VALUES (:subject, :accountId)
                    ON CONFLICT (subject) DO NOTHING""", Map.of("subject", subject, "accountId", accountId));
        } catch (DataAccessException e) {
            log.warn("account_login link failed for account {}: {}", accountId, e.getMessage());
        }
    }

    @Override
    public List<String> subjectsOf(long accountId) {
        try {
            return jdbc.queryForList("SELECT subject FROM account_login WHERE account_id = :accountId",
                    Map.of("accountId", accountId), String.class);
        } catch (DataAccessException e) {
            log.warn("account_login lookup failed for account {}: {}", accountId, e.getMessage());
            return List.of();
        }
    }
}
