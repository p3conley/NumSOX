package com.example.redsoxtracker.dto;

public class TeamRank {
    private final int position; // 0 = unknown / not enough data to rank
    private final boolean tied;

    public TeamRank(int position, boolean tied) {
        this.position = position;
        this.tied = tied;
    }

    public int getPosition() { return position; }
    public boolean isTied() { return tied; }

    /** e.g. "1st in MLB", "T-5th in MLB", or null if not rankable. */
    public String getLabel() {
        if (position <= 0) return null;
        return (tied ? "T-" : "") + ordinal(position) + " in MLB";
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
