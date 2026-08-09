package com.example.redsoxtracker.dto;

public class TeamRankSummary {
    private final TeamRank record;
    private final TeamRank runDifferential;
    private final TeamRank ops;
    private final TeamRank era;
    private final TeamRank bullpenEra;
    private final TeamRank wrcPlus;
    private final TeamRank last10;
    private final TeamRank streak;
    private final TeamRank seriesWins;
    private final TeamRank seriesSweeps;

    public TeamRankSummary(TeamRank record, TeamRank runDifferential, TeamRank ops, TeamRank era,
                           TeamRank bullpenEra, TeamRank wrcPlus, TeamRank last10, TeamRank streak,
                           TeamRank seriesWins, TeamRank seriesSweeps) {
        this.record = record;
        this.runDifferential = runDifferential;
        this.ops = ops;
        this.era = era;
        this.bullpenEra = bullpenEra;
        this.wrcPlus = wrcPlus;
        this.last10 = last10;
        this.streak = streak;
        this.seriesWins = seriesWins;
        this.seriesSweeps = seriesSweeps;
    }

    public TeamRank getRecord() { return record; }
    public TeamRank getRunDifferential() { return runDifferential; }
    public TeamRank getOps() { return ops; }
    public TeamRank getEra() { return era; }
    public TeamRank getBullpenEra() { return bullpenEra; }
    public TeamRank getWrcPlus() { return wrcPlus; }
    public TeamRank getLast10() { return last10; }
    public TeamRank getStreak() { return streak; }
    public TeamRank getSeriesWins() { return seriesWins; }
    public TeamRank getSeriesSweeps() { return seriesSweeps; }
}
