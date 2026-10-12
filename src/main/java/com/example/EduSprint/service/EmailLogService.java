package com.example.EduSprint.service;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class EmailLogService {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public EmailLogService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    // Records that an email of this type goes to the account on this day. Returns false when it is
    // already recorded (e.g. another instance is sending it), so the caller must not send it again.
    public boolean claim(Long accountId, String emailType, LocalDate date) {
        String sql = """
            INSERT INTO email_log (account_id, email_type, sent_date)
            VALUES (:accountId, :emailType, :date)
            ON CONFLICT DO NOTHING
        """;
        return namedParameterJdbcTemplate.update(sql, params(accountId, emailType, date)) == 1;
    }

    // Undoes claim when sending failed, so the log only holds emails that were actually sent.
    public void release(Long accountId, String emailType, LocalDate date) {
        String sql = """
            DELETE FROM email_log
            WHERE account_id = :accountId
              AND email_type = :emailType
              AND sent_date = :date
        """;
        namedParameterJdbcTemplate.update(sql, params(accountId, emailType, date));
    }

    private static Map<String, Object> params(Long accountId, String emailType, LocalDate date) {
        return Map.of(
                "accountId", accountId,
                "emailType", emailType,
                "date", date
        );
    }
}
