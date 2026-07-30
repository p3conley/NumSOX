package com.example.redsoxtracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "pitcher_stat_snapshots")
public class PitcherStatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "season") private Integer season;
    @Column(name = "snapshot_date") private LocalDate snapshotDate;
    @Column(name = "role", length = 10) private String role; // "SP" or "RP"

    // Quick View
    @Column(name = "war") private Double war;
    @Column(name = "era") private Double era;
    @Column(name = "fip") private Double fip;
    @Column(name = "whip") private Double whip;
    @Column(name = "k_rate") private Double kRate;
    @Column(name = "bb_rate") private Double bbRate;
    @Column(name = "k_minus_bb_rate") private Double kMinusBbRate;
    @Column(name = "innings_pitched") private Double inningsPitched;
    @Column(name = "opponent_ops") private Double opponentOps;
    @Column(name = "games_started") private Integer gamesStarted;
    @Column(name = "games") private Integer games;
    @Column(name = "saves") private Integer saves;
    @Column(name = "save_opportunities") private Integer saveOpportunities;
    @Column(name = "holds") private Integer holds;
    @Column(name = "blown_saves") private Integer blownSaves;

    // Advanced
    @Column(name = "xfip") private Double xfip;
    @Column(name = "k_per9") private Double kPer9;
    @Column(name = "bb_per9") private Double bbPer9;
    @Column(name = "hr_per9") private Double hrPer9;
    @Column(name = "era_plus") private Double eraPlus;
    @Column(name = "ground_ball_rate") private Double groundBallRate;
    @Column(name = "whiff_rate") private Double whiffRate;
    @Column(name = "chase_rate") private Double chaseRate;
    @Column(name = "csw_rate") private Double cswRate;
    @Column(name = "avg_fastball_velocity") private Double avgFastballVelocity;
    @Column(name = "spin_rate") private Double spinRate;

    // Contact quality allowed
    @Column(name = "hard_hit_rate_allowed") private Double hardHitRateAllowed;
    @Column(name = "barrel_rate_allowed") private Double barrelRateAllowed;
    @Column(name = "avg_exit_velocity_allowed") private Double avgExitVelocityAllowed;
    @Column(name = "home_runs_allowed") private Integer homeRunsAllowed;

    // Meta
    @Column(name = "source_name") private String sourceName;
    @Column(name = "source_last_updated") private LocalDate sourceLastUpdated;

    public PitcherStatSnapshot() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Double getWar() { return war; }
    public void setWar(Double war) { this.war = war; }
    public Double getEra() { return era; }
    public void setEra(Double era) { this.era = era; }
    public Double getFip() { return fip; }
    public void setFip(Double fip) { this.fip = fip; }
    public Double getWhip() { return whip; }
    public void setWhip(Double whip) { this.whip = whip; }
    public Double getKRate() { return kRate; }
    public void setKRate(Double kRate) { this.kRate = kRate; }
    public Double getBbRate() { return bbRate; }
    public void setBbRate(Double bbRate) { this.bbRate = bbRate; }
    public Double getKMinusBbRate() { return kMinusBbRate; }
    public void setKMinusBbRate(Double kMinusBbRate) { this.kMinusBbRate = kMinusBbRate; }
    public Double getInningsPitched() { return inningsPitched; }
    public void setInningsPitched(Double inningsPitched) { this.inningsPitched = inningsPitched; }
    public Double getOpponentOps() { return opponentOps; }
    public void setOpponentOps(Double opponentOps) { this.opponentOps = opponentOps; }
    public Integer getGamesStarted() { return gamesStarted; }
    public void setGamesStarted(Integer gamesStarted) { this.gamesStarted = gamesStarted; }
    public Integer getGames() { return games; }
    public void setGames(Integer games) { this.games = games; }
    public Integer getSaves() { return saves; }
    public void setSaves(Integer saves) { this.saves = saves; }
    public Integer getSaveOpportunities() { return saveOpportunities; }
    public void setSaveOpportunities(Integer saveOpportunities) { this.saveOpportunities = saveOpportunities; }
    public Integer getHolds() { return holds; }
    public void setHolds(Integer holds) { this.holds = holds; }
    public Integer getBlownSaves() { return blownSaves; }
    public void setBlownSaves(Integer blownSaves) { this.blownSaves = blownSaves; }
    public Double getXfip() { return xfip; }
    public void setXfip(Double xfip) { this.xfip = xfip; }
    public Double getKPer9() { return kPer9; }
    public void setKPer9(Double kPer9) { this.kPer9 = kPer9; }
    public Double getBbPer9() { return bbPer9; }
    public void setBbPer9(Double bbPer9) { this.bbPer9 = bbPer9; }
    public Double getHrPer9() { return hrPer9; }
    public void setHrPer9(Double hrPer9) { this.hrPer9 = hrPer9; }
    public Double getEraPlus() { return eraPlus; }
    public void setEraPlus(Double eraPlus) { this.eraPlus = eraPlus; }
    public Double getGroundBallRate() { return groundBallRate; }
    public void setGroundBallRate(Double groundBallRate) { this.groundBallRate = groundBallRate; }
    public Double getWhiffRate() { return whiffRate; }
    public void setWhiffRate(Double whiffRate) { this.whiffRate = whiffRate; }
    public Double getChaseRate() { return chaseRate; }
    public void setChaseRate(Double chaseRate) { this.chaseRate = chaseRate; }
    public Double getCswRate() { return cswRate; }
    public void setCswRate(Double cswRate) { this.cswRate = cswRate; }
    public Double getAvgFastballVelocity() { return avgFastballVelocity; }
    public void setAvgFastballVelocity(Double avgFastballVelocity) { this.avgFastballVelocity = avgFastballVelocity; }
    public Double getSpinRate() { return spinRate; }
    public void setSpinRate(Double spinRate) { this.spinRate = spinRate; }
    public Double getHardHitRateAllowed() { return hardHitRateAllowed; }
    public void setHardHitRateAllowed(Double hardHitRateAllowed) { this.hardHitRateAllowed = hardHitRateAllowed; }
    public Double getBarrelRateAllowed() { return barrelRateAllowed; }
    public void setBarrelRateAllowed(Double barrelRateAllowed) { this.barrelRateAllowed = barrelRateAllowed; }
    public Double getAvgExitVelocityAllowed() { return avgExitVelocityAllowed; }
    public void setAvgExitVelocityAllowed(Double avgExitVelocityAllowed) { this.avgExitVelocityAllowed = avgExitVelocityAllowed; }
    public Integer getHomeRunsAllowed() { return homeRunsAllowed; }
    public void setHomeRunsAllowed(Integer homeRunsAllowed) { this.homeRunsAllowed = homeRunsAllowed; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public LocalDate getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(LocalDate sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }
}
