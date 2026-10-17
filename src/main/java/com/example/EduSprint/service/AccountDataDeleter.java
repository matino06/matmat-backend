package com.example.EduSprint.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Deletes every row of an account in one transaction: if any delete fails, nothing is deleted. Files on S3 are
 * deleted by {@link AccountDeletionService} after the commit, so a failure here loses nothing.
 *
 * <p>ai_report and account_login rows go by ON DELETE CASCADE, so this also works where those tables don't exist yet.
 */
@Service
public class AccountDataDeleter {

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseService ownCourses;

    public AccountDataDeleter(NamedParameterJdbcTemplate jdbc, OwnCourseService ownCourses) {
        this.jdbc = jdbc;
        this.ownCourses = ownCourses;
    }

    /** Deletes the account's rows; returns its mock exam attempts, whose answer photos are on S3. */
    @Transactional
    public List<Long> deleteRows(long accountId) {
        MapSqlParameterSource a = new MapSqlParameterSource("a", accountId);
        List<Long> attempts = jdbc.queryForList("SELECT attempt_id FROM mock_exam_attempt WHERE account_id = :a", a, Long.class);

        // Own courses first: their lessons, tasks, progress and AI conversations, also other accounts' progress in them.
        ownCourses.deleteRowsOfAllOwnedBy(accountId);

        jdbc.update("""
                DELETE FROM ai_message WHERE conversation_id IN
                    (SELECT conversation_id FROM ai_conversation WHERE account_id = :a)""", a);
        jdbc.update("DELETE FROM ai_conversation WHERE account_id = :a", a);
        jdbc.update("""
                DELETE FROM mock_exam_criterion_score WHERE answer_id IN
                    (SELECT answer_id FROM mock_exam_answer WHERE attempt_id IN
                        (SELECT attempt_id FROM mock_exam_attempt WHERE account_id = :a))""", a);
        jdbc.update("""
                DELETE FROM mock_exam_answer WHERE attempt_id IN
                    (SELECT attempt_id FROM mock_exam_attempt WHERE account_id = :a)""", a);
        jdbc.update("DELETE FROM mock_exam_attempt WHERE account_id = :a", a);
        jdbc.update("DELETE FROM solved_task WHERE account_id = :a", a);
        jdbc.update("DELETE FROM account_objective WHERE account_id = :a", a);
        jdbc.update("DELETE FROM weak_objectives WHERE account_id = :a", a);
        jdbc.update("DELETE FROM user_course_goal WHERE account_id = :a", a);
        jdbc.update("DELETE FROM user_daily_tasks WHERE account_id = :a", a);
        jdbc.update("DELETE FROM email_log WHERE account_id = :a", a);
        // Build jobs, costs and batches of own courses (cost rows stay when only a course is deleted).
        jdbc.update("DELETE FROM variation_batch WHERE account_id = :a", a);
        jdbc.update("DELETE FROM build_job WHERE account_id = :a", a);
        jdbc.update("DELETE FROM build_cost WHERE account_id = :a", a);
        // An admin's name on AI settings they changed.
        jdbc.update("UPDATE ai_settings SET updated_by = NULL WHERE updated_by = :a", a);
        jdbc.update("DELETE FROM account WHERE account_id = :a", a);
        return attempts;
    }
}
