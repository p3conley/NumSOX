package com.example.redsoxtracker.dto;

import java.util.List;

/**
 * One weighted category of the model: what it measured, which way it leaned, how much it
 * counted, and why, in words a reader can check against the numbers underneath it.
 *
 * @param key            stable id, e.g. "starter"
 * @param label          display name, e.g. "Starting Pitcher"
 * @param weight         share of the model as specified, 0-1, before any redistribution
 * @param effectiveWeight what it actually counted for after unavailable categories were
 *                       dropped and their weight shared out
 * @param edge           -1 to +1; positive favours the home club, negative the away club
 * @param awayDisplay    the away club's headline number for this category
 * @param homeDisplay    the home club's headline number for this category
 * @param available      false when there was not enough data to score it at all
 * @param confidence     High, Medium, Low, or Unavailable
 * @param explanation    plain English, no model jargon
 * @param supporting     the individual stats this was built from, with their provenance
 */
public record CategoryScore(
        String key,
        String label,
        double weight,
        double effectiveWeight,
        double edge,
        String awayDisplay,
        String homeDisplay,
        boolean available,
        String confidence,
        String explanation,
        List<StatValue> supporting
) {

    /** A category that could not be scored, so it is shown as such and carries no weight. */
    public static CategoryScore unavailable(String key, String label, double weight, String why) {
        return new CategoryScore(key, label, weight, 0.0, 0.0,
                "Unavailable", "Unavailable", false, "Unavailable", why, List.of());
    }

    /** Which club this category favours, for the comparison table's Edge column. */
    public String edgeLabel(String awayTeam, String homeTeam) {
        if (!available) return "None";
        if (Math.abs(edge) < 0.04) return "Even";
        return edge > 0 ? homeTeam : awayTeam;
    }

    /** How strongly it leans, for reading the table at a glance. */
    public String strength() {
        double m = Math.abs(edge);
        if (!available) return "";
        if (m < 0.04) return "Even";
        if (m < 0.18) return "Slight";
        if (m < 0.40) return "Clear";
        return "Strong";
    }

    /** Weight as a percentage of the model, for the breakdown view. */
    public int weightPct() {
        return (int) Math.round(weight * 100);
    }

    public int effectiveWeightPct() {
        return (int) Math.round(effectiveWeight * 100);
    }
}
