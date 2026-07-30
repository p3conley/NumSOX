package com.example.redsoxtracker.dto;

public class StandingsRow {
    private final String teamName;
    private final int wins;
    private final int losses;
    private final String pct;
    private final String gamesBack;
    private final String wildCardGamesBack;
    private final String last10;
    private final String streak;
    private final Integer runsScored;
    private final Integer runsAllowed;
    private final Integer runDifferential;
    private final String homeRecord;
    private final String awayRecord;
    private final String expectedRecord;
    private final boolean divisionLeader;
    private final Integer divisionRank;
    private final Integer wildCardRank;
    private final boolean redSox;

    public StandingsRow(String teamName, int wins, int losses, String pct, String gamesBack,
                        String wildCardGamesBack, String last10, String streak,
                        Integer runsScored, Integer runsAllowed, Integer runDifferential,
                        String homeRecord, String awayRecord, String expectedRecord,
                        boolean divisionLeader, Integer divisionRank, Integer wildCardRank, boolean redSox) {
        this.teamName = teamName;
        this.wins = wins;
        this.losses = losses;
        this.pct = pct;
        this.gamesBack = gamesBack;
        this.wildCardGamesBack = wildCardGamesBack;
        this.last10 = last10;
        this.streak = streak;
        this.runsScored = runsScored;
        this.runsAllowed = runsAllowed;
        this.runDifferential = runDifferential;
        this.homeRecord = homeRecord;
        this.awayRecord = awayRecord;
        this.expectedRecord = expectedRecord;
        this.divisionLeader = divisionLeader;
        this.divisionRank = divisionRank;
        this.wildCardRank = wildCardRank;
        this.redSox = redSox;
    }

    public String getTeamName() { return teamName; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public String getPct() { return pct; }
    public String getGamesBack() { return gamesBack; }
    public String getWildCardGamesBack() { return wildCardGamesBack; }
    public String getLast10() { return last10; }
    public String getStreak() { return streak; }
    public Integer getRunsScored() { return runsScored; }
    public Integer getRunsAllowed() { return runsAllowed; }
    public Integer getRunDifferential() { return runDifferential; }
    public String getHomeRecord() { return homeRecord; }
    public String getAwayRecord() { return awayRecord; }
    public String getExpectedRecord() { return expectedRecord; }
    public boolean isDivisionLeader() { return divisionLeader; }
    public Integer getDivisionRank() { return divisionRank; }
    public Integer getWildCardRank() { return wildCardRank; }
    public boolean isRedSox() { return redSox; }

    /** CSS class for the sideways tag: shiny gold for division leaders, matte gold/silver/blond for WC 1-3. */
    public String getTagClass() {
        if (divisionLeader) return "tag-div-leader";
        if (wildCardRank != null && wildCardRank == 1) return "tag-wc-1";
        if (wildCardRank != null && wildCardRank == 2) return "tag-wc-2";
        if (wildCardRank != null && wildCardRank == 3) return "tag-wc-3";
        return "";
    }

    public String getTagLabel() {
        if (divisionLeader) return "DIV";
        if (wildCardRank != null && wildCardRank == 1) return "WC1";
        if (wildCardRank != null && wildCardRank == 2) return "WC2";
        if (wildCardRank != null && wildCardRank == 3) return "WC3";
        return "";
    }

    public boolean hasTag() { return !getTagLabel().isEmpty(); }
}
