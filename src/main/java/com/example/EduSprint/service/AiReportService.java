package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiReportDTO;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

/**
 * Students report a wrong or inappropriate AI answer: a chat answer or the AI grading of a mock exam answer
 * (sql/2026-10-16-ai-prijave.sql). Plain JDBC, like {@link AiConsentService}, so {@code ddl-auto=validate} does not
 * depend on the new table.
 */
@Service
public class AiReportService {

    static final Set<String> REASONS = Set.of("incorrect", "inappropriate");
    static final int MAX_NOTE_CHARS = 1000;
    private static final int ADMIN_LIST_LIMIT = 200;

    private final NamedParameterJdbcTemplate jdbc;

    public AiReportService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Reports the student's own assistant message; it also counts as a thumbs down. */
    @Transactional
    public void reportMessage(long accountId, long messageId, String reason, String note, String source) {
        MapSqlParameterSource params = params(accountId, reason, note, source).addValue("messageId", messageId);
        Integer owned = jdbc.queryForObject("""
                SELECT count(*) FROM ai_message m JOIN ai_conversation c ON c.conversation_id = m.conversation_id
                WHERE m.message_id = :messageId AND c.account_id = :accountId AND m.role = 'assistant'""",
                params, Integer.class);
        if (owned == null || owned == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        jdbc.update("""
                INSERT INTO ai_report (account_id, message_id, reason, note, source)
                VALUES (:accountId, :messageId, :reason, :note, :source)
                ON CONFLICT (account_id, message_id) WHERE message_id IS NOT NULL
                DO UPDATE SET reason = EXCLUDED.reason, note = EXCLUDED.note, source = EXCLUDED.source,
                created_at = now()""", params);
        jdbc.update("UPDATE ai_message SET rating = -1 WHERE message_id = :messageId", params);
    }

    /** Reports the AI grading of the student's answer to one question of their own attempt. */
    @Transactional
    public void reportMockExamAnswer(long accountId, long attemptId, long questionId, String reason, String note,
                                     String source) {
        MapSqlParameterSource params = params(accountId, reason, note, source)
                .addValue("attemptId", attemptId).addValue("questionId", questionId);
        List<Long> answerIds = jdbc.queryForList("""
                SELECT a.answer_id FROM mock_exam_answer a JOIN mock_exam_attempt t ON t.attempt_id = a.attempt_id
                WHERE a.attempt_id = :attemptId AND a.question_id = :questionId AND t.account_id = :accountId
                AND a.ai_grading_status = 'graded'""", params, Long.class);
        if (answerIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        jdbc.update("""
                INSERT INTO ai_report (account_id, mock_exam_answer_id, reason, note, source)
                VALUES (:accountId, :answerId, :reason, :note, :source)
                ON CONFLICT (account_id, mock_exam_answer_id) WHERE mock_exam_answer_id IS NOT NULL
                DO UPDATE SET reason = EXCLUDED.reason, note = EXCLUDED.note, source = EXCLUDED.source,
                created_at = now()""", params.addValue("answerId", answerIds.get(0)));
    }

    /** The newest reports first, with what was reported, for the admin. */
    public List<AiReportDTO> list() {
        return jdbc.query("""
                SELECT r.report_id, r.reason, r.note, r.source, r.created_at, acc.email,
                       m.message_id, m.conversation_id, c.task_id, m.model, m.content AS answer,
                       (SELECT u.content FROM ai_message u WHERE u.conversation_id = m.conversation_id
                          AND u.role = 'user' AND u.message_id < m.message_id
                        ORDER BY u.message_id DESC LIMIT 1) AS question,
                       a.attempt_id, a.question_id, q.question_text, a.answer_text, a.answer_image_filename,
                       a.score_awarded, q.points, a.ai_feedback
                FROM ai_report r
                JOIN account acc ON acc.account_id = r.account_id
                LEFT JOIN ai_message m ON m.message_id = r.message_id
                LEFT JOIN ai_conversation c ON c.conversation_id = m.conversation_id
                LEFT JOIN mock_exam_answer a ON a.answer_id = r.mock_exam_answer_id
                LEFT JOIN mock_exam_question q ON q.question_id = a.question_id
                ORDER BY r.created_at DESC LIMIT :limit""",
                new MapSqlParameterSource("limit", ADMIN_LIST_LIMIT), (rs, i) -> row(rs));
    }

    private static AiReportDTO row(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Long messageId = longOrNull(rs, "message_id");
        return new AiReportDTO(
                rs.getLong("report_id"), messageId != null ? "chat" : "mock_exam",
                rs.getString("reason"), rs.getString("note"), rs.getString("source"),
                created == null ? null : created.toInstant(), rs.getString("email"),
                messageId, longOrNull(rs, "conversation_id"), longOrNull(rs, "task_id"),
                rs.getString("model"), rs.getString("question"), rs.getString("answer"),
                longOrNull(rs, "attempt_id"), longOrNull(rs, "question_id"),
                rs.getString("question_text"), rs.getString("answer_text"), rs.getString("answer_image_filename"),
                shortOrNull(rs, "score_awarded"), shortOrNull(rs, "points"),
                rs.getString("ai_feedback"));
    }

    private static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Short shortOrNull(ResultSet rs, String column) throws SQLException {
        short value = rs.getShort(column);
        return rs.wasNull() ? null : value;
    }

    private static MapSqlParameterSource params(long accountId, String reason, String note, String source) {
        return new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("reason", validReason(reason))
                .addValue("note", cleanNote(note))
                .addValue("source", "ios".equals(source) ? "ios" : "web");
    }

    static String validReason(String reason) {
        if (reason == null || !REASONS.contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razlog mora biti incorrect ili inappropriate");
        }
        return reason;
    }

    /** Blank becomes null; a long note is cut to {@value #MAX_NOTE_CHARS} characters. */
    static String cleanNote(String note) {
        if (note == null || note.isBlank()) return null;
        String s = note.strip();
        return s.length() > MAX_NOTE_CHARS ? s.substring(0, MAX_NOTE_CHARS) : s;
    }
}
