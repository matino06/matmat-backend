package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.repository.AccountRepository;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Push notifications to the iOS app: the devices an account registered, what it wants (daily reminder time, evening
 * streak warning, mock exam results), and sending them. Each push goes out at most once per account and day,
 * recorded in email_log like the emails.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    // The reminder job runs every 5 minutes; a reminder whose time passed longer ago than this (a restart, a
    // deploy) is skipped rather than sent late.
    static final int LATE_MINUTES = 60;

    public record Settings(Integer reminderMinute, boolean streakWarning, boolean examResults) {}

    private record Device(String token, boolean sandbox) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final ApnsClient apns;
    private final EmailLogService sent;
    private final SolvedTaskService solvedTasks;
    private final AccountRepository accounts;
    private final LearningObjectiveRepository objectives;

    public PushService(NamedParameterJdbcTemplate jdbc, ApnsClient apns, EmailLogService sent, SolvedTaskService solvedTasks,
                       AccountRepository accounts, LearningObjectiveRepository objectives) {
        this.jdbc = jdbc;
        this.apns = apns;
        this.sent = sent;
        this.solvedTasks = solvedTasks;
        this.accounts = accounts;
        this.objectives = objectives;
    }

    // ---- Devices and settings

    /** A device token from the app; a token that moved to another account (sign-in on a shared phone) moves along. */
    public void registerDevice(long accountId, String token, boolean sandbox) {
        jdbc.update("""
                INSERT INTO push_device (token, account_id, sandbox) VALUES (:token, :accountId, :sandbox)
                ON CONFLICT (token) DO UPDATE SET account_id = :accountId, sandbox = :sandbox, last_seen_at = now()
                """, Map.of("token", token, "accountId", accountId, "sandbox", sandbox));
    }

    /** Signing out: this device gets nothing more for the account. */
    public void removeDevice(long accountId, String token) {
        jdbc.update("DELETE FROM push_device WHERE token = :token AND account_id = :accountId",
                Map.of("token", token, "accountId", accountId));
    }

    public Settings settings(long accountId) {
        List<Settings> rows = jdbc.query("SELECT reminder_minute, streak_warning, exam_results FROM push_settings WHERE account_id = :a",
                Map.of("a", accountId), (rs, i) -> new Settings((Integer) rs.getObject("reminder_minute"),
                        rs.getBoolean("streak_warning"), rs.getBoolean("exam_results")));
        return rows.isEmpty() ? new Settings(null, true, true) : rows.get(0);
    }

    public void saveSettings(long accountId, Settings settings) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("a", accountId);
        params.put("minute", settings.reminderMinute());
        params.put("streak", settings.streakWarning());
        params.put("exam", settings.examResults());
        jdbc.update("""
                INSERT INTO push_settings (account_id, reminder_minute, streak_warning, exam_results)
                VALUES (:a, :minute, :streak, :exam)
                ON CONFLICT (account_id) DO UPDATE
                SET reminder_minute = :minute, streak_warning = :streak, exam_results = :exam
                """, params);
    }

    /** The app sends the daily reminder, so the reminder email would be a second one. When unsure, the email goes. */
    public boolean sendsDailyReminder(long accountId) {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT count(*) FROM push_settings s
                    WHERE s.account_id = :a AND s.reminder_minute IS NOT NULL
                      AND EXISTS (SELECT 1 FROM push_device d WHERE d.account_id = s.account_id)
                    """, Map.of("a", accountId), Integer.class);
            return count != null && count > 0;
        } catch (RuntimeException e) {
            log.warn("Push settings of account {} unreadable: {}", accountId, e.getMessage());
            return false;
        }
    }

    // ---- Scheduled pushes

    @Scheduled(cron = "0 */5 * * * *", zone = "Europe/Zagreb")
    public void sendDailyReminders() {
        int now = LocalTime.now(AppTime.ZONE).toSecondOfDay() / 60;
        LocalDate today = AppTime.today();
        List<Long> due = jdbc.queryForList("""
                SELECT s.account_id FROM push_settings s
                WHERE s.reminder_minute IS NOT NULL AND s.reminder_minute <= :now AND s.reminder_minute > :now - :late
                  AND EXISTS (SELECT 1 FROM push_device d WHERE d.account_id = s.account_id)
                """, Map.of("now", now, "late", LATE_MINUTES), Long.class);
        for (Long accountId : due) {
            if (!sent.claim(accountId, "push_daily", today)) continue;
            run(accountId, () -> day(accountId).flatMap(d -> PushPlan.daily(d, today))
                    .ifPresent(m -> send(accountId, m, Map.of("open", "tasks"))));
        }
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Europe/Zagreb")
    public void sendStreakWarnings() {
        LocalDate today = AppTime.today();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT s.account_id, s.reminder_minute FROM push_settings s
                WHERE s.streak_warning AND EXISTS (SELECT 1 FROM push_device d WHERE d.account_id = s.account_id)
                """, Map.of());
        for (Map<String, Object> row : rows) {
            long accountId = ((Number) row.get("account_id")).longValue();
            Integer minute = row.get("reminder_minute") == null ? null : ((Number) row.get("reminder_minute")).intValue();
            if (!sent.claim(accountId, "push_streak", today)) continue;
            run(accountId, () -> day(accountId).flatMap(d -> PushPlan.streakWarning(d, minute))
                    .ifPresent(m -> send(accountId, m, Map.of("open", "tasks"))));
        }
    }

    /** All answers of a mock exam attempt are graded. Called for each answer that finishes; the log keeps it to one. */
    public void examGraded(long accountId, long attemptId, String examTitle) {
        // Grading must never fail because of a push.
        run(accountId, () -> {
            if (!settings(accountId).examResults()) return;
            if (!sent.claim(accountId, "push_exam_" + attemptId, AppTime.today())) return;
            send(accountId, PushPlan.examGraded(examTitle), Map.of("open", "exam", "attemptId", attemptId));
        });
    }

    // ---- Helpers

    /** The account's day, as the reminders need it. */
    Optional<PushPlan.Day> day(long accountId) {
        Optional<Account> found = accounts.findById(accountId);
        if (found.isEmpty()) return Optional.empty();
        Account account = found.get();
        List<DailyGoalDTO> calendar = solvedTasks.getDailyGoalDTOs(accountId, account.getDailyGoal());
        SolvedTaskService.Streak streak = solvedTasks.streak(calendar);
        LocalDate today = AppTime.today();
        int completed = calendar.stream().filter(d -> d.getDate().equals(today)).findFirst().map(DailyGoalDTO::getCompleted).orElse(0);
        Integer daysInactive = calendar.stream().filter(d -> d.getCompleted() > 0).map(DailyGoalDTO::getDate)
                .max(Comparator.naturalOrder())
                .map(last -> (int) java.time.temporal.ChronoUnit.DAYS.between(last, today))
                .orElse(null);
        List<Object[]> reviews = account.getCurrentCourse() == null ? List.of()
                : objectives.findObjectivesForToday(accountId, account.getCurrentCourse().getCourseId());
        String next = reviews.isEmpty() ? null : (String) reviews.get(0)[0];
        return Optional.of(new PushPlan.Day(solvedTasks.getTodayGoal(accountId, account.getDailyGoal()), completed,
                streak.current(), streak.freezes(), reviews.size(), next, daysInactive));
    }

    private void send(long accountId, PushPlan.Message message, Map<String, Object> data) {
        List<Device> devices = jdbc.query("SELECT token, sandbox FROM push_device WHERE account_id = :a", Map.of("a", accountId),
                (rs, i) -> new Device(rs.getString("token"), rs.getBoolean("sandbox")));
        for (Device device : devices) {
            if (apns.send(device.token(), device.sandbox(), message.title(), message.body(), data) == ApnsClient.Result.GONE) {
                jdbc.update("DELETE FROM push_device WHERE token = :t", Map.of("t", device.token()));
            }
        }
    }

    private static void run(long accountId, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            log.warn("Push to account {} failed: {}", accountId, e.getMessage());
        }
    }
}
