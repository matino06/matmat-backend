package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiConsentDTO;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

/**
 * Whether an account allows its data to be sent to the AI (sql/2026-10-15-ai-privola.sql). Plain JDBC, like
 * account_login: the Account entity and {@code ddl-auto=validate} are left alone.
 */
@Service
public class AiConsentService {

    /** Where the student turns AI on: Profil in the app, Postavke on the web. */
    public static String missingMessage(String client) {
        return "ios".equalsIgnoreCase(client) ? "Za AI uključi dozvolu u Profilu." : "Za AI uključi dozvolu u Postavkama.";
    }
    private static final Set<String> SOURCES = Set.of("ios", "web");

    private final NamedParameterJdbcTemplate jdbc;

    public AiConsentService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AiConsentDTO get(long accountId) {
        try {
            return jdbc.queryForObject("""
                    SELECT ai_consent, ai_consent_version, ai_consent_at, ai_consent_source FROM account
                    WHERE account_id = :id""", new MapSqlParameterSource("id", accountId), (rs, i) -> {
                Object granted = rs.getObject(1);
                Timestamp at = rs.getTimestamp(3);
                return new AiConsentDTO(granted == null ? null : (Boolean) granted, rs.getString(2),
                        at == null ? null : at.toInstant(), rs.getString(4));
            });
        } catch (DataAccessException e) {
            return new AiConsentDTO(null, null, null, null);
        }
    }

    public boolean isGranted(long accountId) {
        return Boolean.TRUE.equals(get(accountId).granted());
    }

    public AiConsentDTO set(long accountId, Boolean granted, String version, String source) {
        if (granted == null || version == null || version.isBlank() || version.length() > 20 || !SOURCES.contains(source)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neispravna dozvola");
        }
        jdbc.update("""
                UPDATE account SET ai_consent = :granted, ai_consent_version = :version, ai_consent_at = :at,
                ai_consent_source = :source WHERE account_id = :id""",
                new MapSqlParameterSource()
                        .addValue("granted", granted).addValue("version", version)
                        .addValue("at", Timestamp.from(Instant.now())).addValue("source", source)
                        .addValue("id", accountId));
        return get(accountId);
    }
}
