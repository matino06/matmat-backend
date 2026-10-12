package com.example.EduSprint.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

// Days (calendar, streak, daily goal, reminders) are counted in Croatian time,
// independent of the server's and the database's time zone.
public final class AppTime {
    public static final ZoneId ZONE = ZoneId.of("Europe/Zagreb");

    private AppTime() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDate dateOf(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }
}
