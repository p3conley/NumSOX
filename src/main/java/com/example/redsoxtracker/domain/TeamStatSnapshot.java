package com.example.redsoxtracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "team_stat_snapshots")
public class TeamStatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "season")
    private Integer season;

    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    // Record
    @Column(name = "wins") private Integer wins;
    @Column(name = "losses") private Integer losses;
    @Column(name = "run_differential") private Integer runDifferential;
    @Column(name = "runs_scored") private Integer runsScored;
    @Column(name = "runs_allowed") private Integer runsAllowed;
    @Column(name = "runs_per_game") private Double runsPerGame;

    // Standings (MLB Stats API standings feed — already-computed ranks/tiebreakers)
    @Column(name = "division_rank") private Integer divisionRank;
    @Column(name = "wild_card_rank") private Integer wildCardRank;
    @Column(name = "games_back") private String gamesBack;
    @Column(name = "wild_card_games_back") private String wildCardGamesBack;
    @Column(name = "division_leader") private Boolean divisionLeader;
    @Column(name = "home_record") private String homeRecord;
    @Column(name = "away_record") private String awayRecord;
    @Column(name = "expected_record") private String expectedRecord;

    // Offense
    @Column(name = "team_obp") private Double teamObp;
    @Column(name = "team_slg") private Double teamSlg;
    @Column(name = "team_ops") private Double teamOps;
    @Column(name = "team_wrc_plus") private Double teamWrcPlus;
    @Column(name = "team_war") private Double teamWar;
    @Column(name = "team_k_rate") private Double teamKRate;
    @Column(name = "team_bb_rate") private Double teamBbRate;
    @Column(name = "team_iso") private Double teamIso;
    @Column(name = "team_babip") private Double teamBabip;
    @Column(name = "team_avg") private Double teamAvg;
    @Column(name = "team_hard_hit_rate") private Double teamHardHitRate;
    @Column(name = "team_barrel_rate") private Double teamBarrelRate;
    @Column(name = "team_home_runs") private Integer teamHomeRuns;

    // Pitching
    @Column(name = "team_era") private Double teamEra;
    @Column(name = "team_fip") private Double teamFip;
    @Column(name = "team_whip") private Double teamWhip;
    @Column(name = "team_k_rate_pitching") private Double teamKRatePitching;
    @Column(name = "team_bb_rate_pitching") private Double teamBbRatePitching;
    @Column(name = "home_runs_allowed") private Integer homeRunsAllowed;

    // Bullpen
    @Column(name = "bullpen_era") private Double bullpenEra;
    @Column(name = "bullpen_fip") private Double bullpenFip;
    @Column(name = "bullpen_whip") private Double bullpenWhip;
    @Column(name = "bullpen_k_rate") private Double bullpenKRate;
    @Column(name = "bullpen_bb_rate") private Double bullpenBbRate;
    @Column(name = "bullpen_recent_era") private Double bullpenRecentEra;

    // Defense
    @Column(name = "outs_above_average") private Integer outsAboveAverage;
    @Column(name = "defensive_efficiency") private Double defensiveEfficiency;
    @Column(name = "fielding_percentage") private Double fieldingPercentage;

    // Recent form
    @Column(name = "last10_wins") private Integer last10Wins;
    @Column(name = "last10_losses") private Integer last10Losses;
    @Column(name = "last5_run_differential") private Integer last5RunDifferential;
    @Column(name = "last5_runs_scored") private Integer last5RunsScored;
    @Column(name = "last5_runs_allowed") private Integer last5RunsAllowed;
    @Column(name = "current_streak") private String currentStreak;

    // Meta
    @Column(name = "probable_starter_name") private String probableStarterName;
    @Column(name = "source_name") private String sourceName;
    @Column(name = "source_last_updated") private LocalDate sourceLastUpdated;

    public TeamStatSnapshot() {}

    // Convenience
    public String getRecord() {
        if (wins == null || losses == null) return "N/A";
        return wins + "-" + losses;
    }

    public String getLast10Record() {
        if (last10Wins == null || last10Losses == null) return "N/A";
        return last10Wins + "-" + last10Losses;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }
    public Integer getRunDifferential() { return runDifferential; }
    public void setRunDifferential(Integer runDifferential) { this.runDifferential = runDifferential; }
    public Integer getRunsScored() { return runsScored; }
    public void setRunsScored(Integer runsScored) { this.runsScored = runsScored; }
    public Integer getRunsAllowed() { return runsAllowed; }
    public void setRunsAllowed(Integer runsAllowed) { this.runsAllowed = runsAllowed; }
    public Double getRunsPerGame() { return runsPerGame; }
    public void setRunsPerGame(Double runsPerGame) { this.runsPerGame = runsPerGame; }
    public Integer getDivisionRank() { return divisionRank; }
    public void setDivisionRank(Integer divisionRank) { this.divisionRank = divisionRank; }
    public Integer getWildCardRank() { return wildCardRank; }
    public void setWildCardRank(Integer wildCardRank) { this.wildCardRank = wildCardRank; }
    public String getGamesBack() { return gamesBack; }
    public void setGamesBack(String gamesBack) { this.gamesBack = gamesBack; }
    public String getWildCardGamesBack() { return wildCardGamesBack; }
    public void setWildCardGamesBack(String wildCardGamesBack) { this.wildCardGamesBack = wildCardGamesBack; }
    public Boolean getDivisionLeader() { return divisionLeader; }
    public void setDivisionLeader(Boolean divisionLeader) { this.divisionLeader = divisionLeader; }
    public String getHomeRecord() { return homeRecord; }
    public void setHomeRecord(String homeRecord) { this.homeRecord = homeRecord; }
    public String getAwayRecord() { return awayRecord; }
    public void setAwayRecord(String awayRecord) { this.awayRecord = awayRecord; }
    public String getExpectedRecord() { return expectedRecord; }
    public void setExpectedRecord(String expectedRecord) { this.expectedRecord = expectedRecord; }
    public Double getTeamObp() { return teamObp; }
    public void setTeamObp(Double teamObp) { this.teamObp = teamObp; }
    public Double getTeamSlg() { return teamSlg; }
    public void setTeamSlg(Double teamSlg) { this.teamSlg = teamSlg; }
    public Double getTeamOps() { return teamOps; }
    public void setTeamOps(Double teamOps) { this.teamOps = teamOps; }
    public Double getTeamWrcPlus() { return teamWrcPlus; }
    public void setTeamWrcPlus(Double teamWrcPlus) { this.teamWrcPlus = teamWrcPlus; }
    public Double getTeamWar() { return teamWar; }
    public void setTeamWar(Double teamWar) { this.teamWar = teamWar; }
    public Double getTeamKRate() { return teamKRate; }
    public void setTeamKRate(Double teamKRate) { this.teamKRate = teamKRate; }
    public Double getTeamBbRate() { return teamBbRate; }
    public void setTeamBbRate(Double teamBbRate) { this.teamBbRate = teamBbRate; }
    public Double getTeamIso() { return teamIso; }
    public void setTeamIso(Double teamIso) { this.teamIso = teamIso; }
    public Double getTeamBabip() { return teamBabip; }
    public void setTeamBabip(Double teamBabip) { this.teamBabip = teamBabip; }
    public Double getTeamAvg() { return teamAvg; }
    public void setTeamAvg(Double teamAvg) { this.teamAvg = teamAvg; }
    public Double getTeamHardHitRate() { return teamHardHitRate; }
    public void setTeamHardHitRate(Double teamHardHitRate) { this.teamHardHitRate = teamHardHitRate; }
    public Double getTeamBarrelRate() { return teamBarrelRate; }
    public void setTeamBarrelRate(Double teamBarrelRate) { this.teamBarrelRate = teamBarrelRate; }
    public Integer getTeamHomeRuns() { return teamHomeRuns; }
    public void setTeamHomeRuns(Integer teamHomeRuns) { this.teamHomeRuns = teamHomeRuns; }
    public Double getTeamEra() { return teamEra; }
    public void setTeamEra(Double teamEra) { this.teamEra = teamEra; }
    public Double getTeamFip() { return teamFip; }
    public void setTeamFip(Double teamFip) { this.teamFip = teamFip; }
    public Double getTeamWhip() { return teamWhip; }
    public void setTeamWhip(Double teamWhip) { this.teamWhip = teamWhip; }
    public Double getTeamKRatePitching() { return teamKRatePitching; }
    public void setTeamKRatePitching(Double teamKRatePitching) { this.teamKRatePitching = teamKRatePitching; }
    public Double getTeamBbRatePitching() { return teamBbRatePitching; }
    public void setTeamBbRatePitching(Double teamBbRatePitching) { this.teamBbRatePitching = teamBbRatePitching; }
    public Integer getHomeRunsAllowed() { return homeRunsAllowed; }
    public void setHomeRunsAllowed(Integer homeRunsAllowed) { this.homeRunsAllowed = homeRunsAllowed; }
    public Double getBullpenEra() { return bullpenEra; }
    public void setBullpenEra(Double bullpenEra) { this.bullpenEra = bullpenEra; }
    public Double getBullpenFip() { return bullpenFip; }
    public void setBullpenFip(Double bullpenFip) { this.bullpenFip = bullpenFip; }
    public Double getBullpenWhip() { return bullpenWhip; }
    public void setBullpenWhip(Double bullpenWhip) { this.bullpenWhip = bullpenWhip; }
    public Double getBullpenKRate() { return bullpenKRate; }
    public void setBullpenKRate(Double bullpenKRate) { this.bullpenKRate = bullpenKRate; }
    public Double getBullpenBbRate() { return bullpenBbRate; }
    public void setBullpenBbRate(Double bullpenBbRate) { this.bullpenBbRate = bullpenBbRate; }
    public Double getBullpenRecentEra() { return bullpenRecentEra; }
    public void setBullpenRecentEra(Double bullpenRecentEra) { this.bullpenRecentEra = bullpenRecentEra; }
    public Integer getOutsAboveAverage() { return outsAboveAverage; }
    public void setOutsAboveAverage(Integer outsAboveAverage) { this.outsAboveAverage = outsAboveAverage; }
    public Double getDefensiveEfficiency() { return defensiveEfficiency; }
    public void setDefensiveEfficiency(Double defensiveEfficiency) { this.defensiveEfficiency = defensiveEfficiency; }
    public Double getFieldingPercentage() { return fieldingPercentage; }
    public void setFieldingPercentage(Double fieldingPercentage) { this.fieldingPercentage = fieldingPercentage; }
    public Integer getLast10Wins() { return last10Wins; }
    public void setLast10Wins(Integer last10Wins) { this.last10Wins = last10Wins; }
    public Integer getLast10Losses() { return last10Losses; }
    public void setLast10Losses(Integer last10Losses) { this.last10Losses = last10Losses; }
    public Integer getLast5RunDifferential() { return last5RunDifferential; }
    public void setLast5RunDifferential(Integer last5RunDifferential) { this.last5RunDifferential = last5RunDifferential; }
    public Integer getLast5RunsScored() { return last5RunsScored; }
    public void setLast5RunsScored(Integer last5RunsScored) { this.last5RunsScored = last5RunsScored; }
    public Integer getLast5RunsAllowed() { return last5RunsAllowed; }
    public void setLast5RunsAllowed(Integer last5RunsAllowed) { this.last5RunsAllowed = last5RunsAllowed; }
    public String getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(String currentStreak) { this.currentStreak = currentStreak; }
    public String getProbableStarterName() { return probableStarterName; }
    public void setProbableStarterName(String probableStarterName) { this.probableStarterName = probableStarterName; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public LocalDate getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(LocalDate sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }
}
