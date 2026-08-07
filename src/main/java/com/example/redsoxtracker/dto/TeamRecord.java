package com.example.redsoxtracker.dto;

/**
 * A team's win/loss picture. Built either from the MLB standings feed or, when that feed
 * still lags behind a game the app has already scored, from the local game log.
 */
public class TeamRecord {

    private final Integer wins;
    private final Integer losses;
    private final String last10;
    private final String streak;
    private final boolean derivedFromGameLog;

    public TeamRecord(Integer wins, Integer losses, String last10, String streak, boolean derivedFromGameLog) {
        this.wins = wins;
        this.losses = losses;
        this.last10 = last10;
        this.streak = streak;
        this.derivedFromGameLog = derivedFromGameLog;
    }

    public Integer getWins() { return wins; }

    public Integer getLosses() { return losses; }

    public String getRecord() {
        if (wins == null || losses == null) return "N/A";
        return wins + "-" + losses;
    }

    public String getLast10() { return last10 != null ? last10 : "N/A"; }

    public String getStreak() { return streak != null ? streak : "N/A"; }

    public int getGamesPlayed() {
        if (wins == null || losses == null) return 0;
        return wins + losses;
    }

    public boolean isDerivedFromGameLog() { return derivedFromGameLog; }

    public String getWinPct() {
        int total = getGamesPlayed();
        if (total == 0) return "N/A";
        String formatted = String.format("%.3f", (double) wins / total);
        return formatted.startsWith("0") ? formatted.substring(1) : formatted;
    }
}
