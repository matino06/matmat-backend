package com.example.EduSprint.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntSupplier;

// Which engagement email an account gets on a given day. Follows the usual win-back pattern:
// a few emails spread over the first month of inactivity, the last one saying it is the last,
// then silence until the user is active again (any solved task restarts the sequence).
public final class EngagementEmailPlan {

    public enum Type {
        NEVER_STARTED,
        NEVER_STARTED_LAST,
        DAILY_REMINDER,
        STREAK_LOST,
        PAUSE,
        LAST_CALL;

        // Value stored in email_log.email_type
        public String logType() {
            return name().toLowerCase();
        }
    }

    // Days since registration, for accounts that never solved a task
    static final int NEVER_STARTED_DAY = 2;
    static final int NEVER_STARTED_LAST_DAY = 7;

    // Days since the last solved task
    static final int DAILY_REMINDER_DAY = 1;
    static final int STREAK_LOST_DAY = 2;
    static final int FIRST_PAUSE_DAY = 7;
    static final int SECOND_PAUSE_DAY = 14;
    static final int LAST_CALL_DAY = 30;

    // A broken streak shorter than this is not worth an email of its own
    static final int STREAK_LOST_MIN_LENGTH = 3;

    // A course counts as neglected after this many days without a task (while another course is used)
    static final int NEGLECTED_COURSE_MIN_DAYS = 5;
    // After this long the user has most likely dropped the course on purpose
    static final int NEGLECTED_COURSE_MAX_DAYS = 30;

    public record NeglectedCourse(String courseName, int days) {}

    private EngagementEmailPlan() {}

    // lastActivityDate is null when the account never solved a task. lostStreak is only evaluated
    // when needed: the length of the streak that ended on lastActivityDate.
    public static Optional<Type> forDay(LocalDate today, LocalDate registrationDate, LocalDate lastActivityDate, IntSupplier lostStreak) {
        if (lastActivityDate == null) {
            long daysSinceRegistration = ChronoUnit.DAYS.between(registrationDate, today);
            if (daysSinceRegistration == NEVER_STARTED_DAY) return Optional.of(Type.NEVER_STARTED);
            if (daysSinceRegistration == NEVER_STARTED_LAST_DAY) return Optional.of(Type.NEVER_STARTED_LAST);
            return Optional.empty();
        }

        long daysInactive = ChronoUnit.DAYS.between(lastActivityDate, today);
        if (daysInactive == DAILY_REMINDER_DAY) return Optional.of(Type.DAILY_REMINDER);
        if (daysInactive == STREAK_LOST_DAY) {
            return lostStreak.getAsInt() >= STREAK_LOST_MIN_LENGTH ? Optional.of(Type.STREAK_LOST) : Optional.empty();
        }
        if (daysInactive == FIRST_PAUSE_DAY || daysInactive == SECOND_PAUSE_DAY) return Optional.of(Type.PAUSE);
        if (daysInactive == LAST_CALL_DAY) return Optional.of(Type.LAST_CALL);
        return Optional.empty();
    }

    // The course the user has been skipping the longest while still using another one, if any.
    public static Optional<NeglectedCourse> neglectedCourse(Map<String, LocalDate> lastActivityPerCourse, LocalDate today) {
        List<NeglectedCourse> courses = lastActivityPerCourse.entrySet().stream()
                .map(e -> new NeglectedCourse(e.getKey(), (int) ChronoUnit.DAYS.between(e.getValue(), today)))
                .toList();
        boolean usesAnotherCourse = courses.stream().anyMatch(c -> c.days() < NEGLECTED_COURSE_MIN_DAYS);
        if (!usesAnotherCourse) {
            return Optional.empty();
        }
        return courses.stream()
                .filter(c -> c.days() >= NEGLECTED_COURSE_MIN_DAYS && c.days() <= NEGLECTED_COURSE_MAX_DAYS)
                .max(Comparator.comparingInt(NeglectedCourse::days));
    }
}
