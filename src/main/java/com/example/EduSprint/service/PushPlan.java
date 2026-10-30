package com.example.EduSprint.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// What a push notification says, from the account's day as the backend knows it (every device and the web count).
// The same texts as the iOS app's local reminders, which these replace while the app has push.
public final class PushPlan {

    // A reminder set this late is itself the evening reminder, so no separate streak warning.
    static final int LATE_REMINDER_MINUTE = 20 * 60;

    /**
     * @param goal          today's goal (the daily goal, or fewer if fewer tasks are available)
     * @param completed     tasks solved today, on any device
     * @param streak        current streak in days (today counts only once something is solved)
     * @param freezes       streak freezes held
     * @param reviewsDue    objectives due for review today in the current course
     * @param next          the first of them, or the next topic; null when unknown
     * @param daysInactive  days since the last solved task; null if the account never solved one
     */
    public record Day(int goal, int completed, int streak, int freezes, int reviewsDue, String next, Integer daysInactive) {}

    public record Message(String title, String body) {}

    private PushPlan() {}

    // While a streak can still be saved the reminder comes every day; after that the emails take over
    // (EngagementEmailPlan: a few spread over a month, then silence).
    public static Optional<Message> daily(Day day, LocalDate today) {
        if (day.daysInactive() == null || day.daysInactive() > EngagementEmailPlan.LAST_STREAK_DAY) return Optional.empty();
        if (day.goal() <= 0 || day.completed() >= day.goal()) return Optional.empty();
        if (day.completed() > 0) {
            String next = day.next() == null ? "" : " Sljedeće: " + day.next() + ".";
            return Optional.of(new Message("Još " + tasks(day.goal() - day.completed()) + " do cilja",
                    "Riješio si " + day.completed() + " od " + day.goal() + "." + next));
        }
        String first = day.next() == null ? "Danas te čeka " + tasks(day.goal()) + "." : "Prvo te čeka: " + day.next() + ".";
        if (day.streak() > 0) {
            return Optional.of(new Message("Čuvaj niz od " + days(day.streak()), first + " Već jedan zadatak čuva niz."));
        }
        if (day.reviewsDue() > 0) {
            return Optional.of(new Message("Danas te čeka " + reviews(day.reviewsDue()),
                    first + " Vrati ih dok su još svježa."));
        }
        // New texts work best at first, so the general one changes from day to day.
        List<Message> general = List.of(
                new Message("Vrijeme za MatMat", first),
                new Message("Zadaci za danas su spremni", "Vraćaju ti se baš kad ih počinješ zaboravljati."),
                new Message("Pet minuta za MatMat", "Prvi zadatak je najteži. Ostali idu lakše."),
                new Message("Mali korak danas", "Malo svaki dan vrijedi više od cijelog dana učenja pred ispit."));
        return Optional.of(general.get((int) (today.toEpochDay() % general.size())));
    }

    // In the evening, only while a streak is on and nothing has been solved today.
    public static Optional<Message> streakWarning(Day day, Integer reminderMinute) {
        if (reminderMinute != null && reminderMinute >= LATE_REMINDER_MINUTE) return Optional.empty();
        if (day.streak() <= 0 || day.completed() >= SolvedTaskService.STREAK_MIN_TASKS) return Optional.empty();
        if (day.freezes() > 0) {
            return Optional.of(new Message("Niz od " + days(day.streak()) + " čeka zadatak",
                    "Bez zadatka do ponoći potrošit ćeš zaštitu niza. Dovoljan je jedan."));
        }
        return Optional.of(new Message("Niz od " + days(day.streak()) + " večeras pukne",
                "Riješi jedan zadatak prije ponoći i sačuvaj ga."));
    }

    // The score stays off the lock screen.
    public static Message examGraded(String examTitle) {
        return new Message("Probna matura je ocijenjena", examTitle + ": pogledaj rezultat i objašnjenja po zadacima.");
    }

    static String tasks(int n) {
        return plural(n, "zadatak", "zadatka", "zadataka");
    }

    static String reviews(int n) {
        return plural(n, "ponavljanje", "ponavljanja", "ponavljanja");
    }

    static String days(int n) {
        return n % 10 == 1 && n % 100 != 11 ? n + " dan" : n + " dana";
    }

    private static String plural(int n, String one, String few, String many) {
        if (n % 10 == 1 && n % 100 != 11) return n + " " + one;
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 > 14)) return n + " " + few;
        return n + " " + many;
    }
}
