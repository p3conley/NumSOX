package com.example.redsoxtracker.dto;

import java.time.LocalDate;

/**
 * One statistic plus where it came from and how much it should be trusted.
 *
 * <p>The model is only as honest as its inputs, so every number carried into it travels
 * with its provenance rather than as a bare double. An unavailable stat is represented
 * explicitly instead of being silently defaulted to a league-average value, because a
 * guessed input that looks like a real one is the single easiest way to make a model
 * confidently wrong.</p>
 *
 * @param label     what the number is, in plain words
 * @param value     the number itself, or null when it could not be sourced
 * @param display   pre-formatted for the page, e.g. ".768" or "3.51"
 * @param source    where it came from, e.g. "MLB Stats API"
 * @param updated   when that source last refreshed it
 * @param demo      true when this is sample data standing in for a real feed
 */
public record StatValue(
        String label,
        Double value,
        String display,
        String source,
        LocalDate updated,
        boolean demo
) {

    /** A real, sourced number. */
    public static StatValue of(String label, Double value, String display, String source, LocalDate updated) {
        return new StatValue(label, value, display, source, updated, false);
    }

    /** Explicitly missing. Renders as "Unavailable" and drags model confidence down. */
    public static StatValue unavailable(String label) {
        return new StatValue(label, null, "Unavailable", null, null, false);
    }

    /** Sample data standing in for a feed that is not wired up yet. */
    public static StatValue demo(String label, Double value, String display) {
        return new StatValue(label, value, display, "Demo data", null, true);
    }

    public boolean isAvailable() {
        return value != null && !demo;
    }

    /** What the page shows next to the number, or null when there is nothing to add. */
    public String badge() {
        if (demo) return "Demo Data";
        if (value == null) return "Unavailable";
        return null;
    }
}
