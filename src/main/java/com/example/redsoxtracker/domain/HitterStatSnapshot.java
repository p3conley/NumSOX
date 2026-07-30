package com.example.redsoxtracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "hitter_stat_snapshots")
public class HitterStatSnapshot {

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

    // Quick View stats
    @Column(name = "war") private Double war;
    @Column(name = "wrc_plus") private Double wrcPlus;
    @Column(name = "ops") private Double ops;
    @Column(name = "obp") private Double obp;
    @Column(name = "slg") private Double slg;
    @Column(name = "avg") private Double avg;
    @Column(name = "home_runs") private Integer homeRuns;
    @Column(name = "rbi") private Integer rbi;
    @Column(name = "runs") private Integer runs;
    @Column(name = "stolen_bases") private Integer stolenBases;
    @Column(name = "games_played") private Integer gamesPlayed;
    @Column(name = "plate_appearances") private Integer plateAppearances;

    // Advanced stats
    @Column(name = "bb_rate") private Double bbRate;
    @Column(name = "k_rate") private Double kRate;
    @Column(name = "iso") private Double iso;
    @Column(name = "babip") private Double babip;
    @Column(name = "hard_hit_rate") private Double hardHitRate;
    @Column(name = "barrel_rate") private Double barrelRate;
    @Column(name = "avg_exit_velocity") private Double avgExitVelocity;
    @Column(name = "launch_angle") private Double launchAngle;
    @Column(name = "sprint_speed") private Double sprintSpeed;

    // Fielding
    @Column(name = "outs_above_average") private Integer outsAboveAverage;
    @Column(name = "fielding_percentage") private Double fieldingPercentage;
    @Column(name = "range_factor") private Double rangeFactor;
    @Column(name = "arm_strength") private Double armStrength;
    @Column(name = "pop_time") private Double popTime;
    @Column(name = "caught_stealing_pct") private Double caughtStealingPct;
    @Column(name = "framing_runs") private Double framingRuns;

    // Meta
    @Column(name = "source_name") private String sourceName;
    @Column(name = "source_last_updated") private LocalDate sourceLastUpdated;

    public HitterStatSnapshot() {}

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
    public Double getWar() { return war; }
    public void setWar(Double war) { this.war = war; }
    public Double getWrcPlus() { return wrcPlus; }
    public void setWrcPlus(Double wrcPlus) { this.wrcPlus = wrcPlus; }
    public Double getOps() { return ops; }
    public void setOps(Double ops) { this.ops = ops; }
    public Double getObp() { return obp; }
    public void setObp(Double obp) { this.obp = obp; }
    public Double getSlg() { return slg; }
    public void setSlg(Double slg) { this.slg = slg; }
    public Double getAvg() { return avg; }
    public void setAvg(Double avg) { this.avg = avg; }
    public Integer getHomeRuns() { return homeRuns; }
    public void setHomeRuns(Integer homeRuns) { this.homeRuns = homeRuns; }
    public Integer getRbi() { return rbi; }
    public void setRbi(Integer rbi) { this.rbi = rbi; }
    public Integer getRuns() { return runs; }
    public void setRuns(Integer runs) { this.runs = runs; }
    public Integer getStolenBases() { return stolenBases; }
    public void setStolenBases(Integer stolenBases) { this.stolenBases = stolenBases; }
    public Integer getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(Integer gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public Integer getPlateAppearances() { return plateAppearances; }
    public void setPlateAppearances(Integer plateAppearances) { this.plateAppearances = plateAppearances; }
    public Double getBbRate() { return bbRate; }
    public void setBbRate(Double bbRate) { this.bbRate = bbRate; }
    public Double getKRate() { return kRate; }
    public void setKRate(Double kRate) { this.kRate = kRate; }
    public Double getIso() { return iso; }
    public void setIso(Double iso) { this.iso = iso; }
    public Double getBabip() { return babip; }
    public void setBabip(Double babip) { this.babip = babip; }
    public Double getHardHitRate() { return hardHitRate; }
    public void setHardHitRate(Double hardHitRate) { this.hardHitRate = hardHitRate; }
    public Double getBarrelRate() { return barrelRate; }
    public void setBarrelRate(Double barrelRate) { this.barrelRate = barrelRate; }
    public Double getAvgExitVelocity() { return avgExitVelocity; }
    public void setAvgExitVelocity(Double avgExitVelocity) { this.avgExitVelocity = avgExitVelocity; }
    public Double getLaunchAngle() { return launchAngle; }
    public void setLaunchAngle(Double launchAngle) { this.launchAngle = launchAngle; }
    public Double getSprintSpeed() { return sprintSpeed; }
    public void setSprintSpeed(Double sprintSpeed) { this.sprintSpeed = sprintSpeed; }
    public Integer getOutsAboveAverage() { return outsAboveAverage; }
    public void setOutsAboveAverage(Integer outsAboveAverage) { this.outsAboveAverage = outsAboveAverage; }
    public Double getFieldingPercentage() { return fieldingPercentage; }
    public void setFieldingPercentage(Double fieldingPercentage) { this.fieldingPercentage = fieldingPercentage; }
    public Double getRangeFactor() { return rangeFactor; }
    public void setRangeFactor(Double rangeFactor) { this.rangeFactor = rangeFactor; }
    public Double getArmStrength() { return armStrength; }
    public void setArmStrength(Double armStrength) { this.armStrength = armStrength; }
    public Double getPopTime() { return popTime; }
    public void setPopTime(Double popTime) { this.popTime = popTime; }
    public Double getCaughtStealingPct() { return caughtStealingPct; }
    public void setCaughtStealingPct(Double caughtStealingPct) { this.caughtStealingPct = caughtStealingPct; }
    public Double getFramingRuns() { return framingRuns; }
    public void setFramingRuns(Double framingRuns) { this.framingRuns = framingRuns; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public LocalDate getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(LocalDate sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }
}
