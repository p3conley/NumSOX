package com.example.redsoxtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class RedSoxTrackerApplication {

    /**
     * NumSOX is a Red Sox site, so every date and time it shows is Eastern: first pitch,
     * "today's" game, and the post-game hold window. Hosts run in UTC by default, which
     * would render a 7:10 PM first pitch as 11:10 PM and roll the calendar over to the
     * next day at 8 PM Eastern, mid-game. Pinning the JVM zone here keeps LocalDate.now(),
     * LocalDateTime.now() and the schedule conversion all on the same clock.
     *
     * <p>Override with the APP_TIMEZONE environment variable if the app is ever hosted for
     * a different audience.</p>
     */
    private static final String DEFAULT_TIMEZONE = "America/New_York";

    public static void main(String[] args) {
        String zone = System.getenv("APP_TIMEZONE");
        if (zone == null || zone.isBlank()) zone = DEFAULT_TIMEZONE;
        TimeZone.setDefault(TimeZone.getTimeZone(zone));

        SpringApplication.run(RedSoxTrackerApplication.class, args);
    }
}
