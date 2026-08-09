package com.example.redsoxtracker.dto;

/**
 * Where the Red Sox sit for one stat, both against all 30 clubs and against the AL.
 * A position of 0 means there was not enough data to rank.
 */
public class TeamRank {

    private final int position;
    private final boolean tied;
    private final int leaguePosition;
    private final boolean leagueTied;

    public TeamRank(int position, boolean tied, int leaguePosition, boolean leagueTied) {
        this.position = position;
        this.tied = tied;
        this.leaguePosition = leaguePosition;
        this.leagueTied = leagueTied;
    }

    /** Convenience for ranks that only make sense league-wide. */
    public TeamRank(int position, boolean tied) {
        this(position, tied, 0, false);
    }

    public int getPosition() { return position; }
    public boolean isTied() { return tied; }
    public int getLeaguePosition() { return leaguePosition; }
    public boolean isLeagueTied() { return leagueTied; }

    /** e.g. "1st in MLB", "T-5th in MLB", or null if not rankable. */
    public String getLabel() {
        if (position <= 0) return null;
        return (tied ? "T-" : "") + ordinal(position) + " in MLB";
    }

    /** e.g. "2nd in AL", shown under the MLB line. */
    public String getLeagueLabel() {
        if (leaguePosition <= 0) return null;
        return (leagueTied ? "T-" : "") + ordinal(leaguePosition) + " in AL";
    }

    private String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
