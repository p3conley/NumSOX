package com.example.redsoxtracker.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ballpark_factor_snapshots")
public class BallparkFactorSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "venue_name", nullable = false)
    private String venueName;

    @Column(name = "season") private Integer season;
    @Column(name = "snapshot_date") private LocalDate snapshotDate;

    // Quick View factors (100 = league average)
    @Column(name = "park_factor") private Integer parkFactor;
    @Column(name = "run_park_factor") private Integer runParkFactor;
    @Column(name = "home_run_park_factor") private Integer homeRunParkFactor;
    @Column(name = "hit_park_factor") private Integer hitParkFactor;
    @Column(name = "double_park_factor") private Integer doubleParkFactor;
    @Column(name = "triple_park_factor") private Integer tripleParkFactor;
    @Column(name = "lhb_park_factor") private Integer lhbParkFactor;
    @Column(name = "rhb_park_factor") private Integer rhbParkFactor;

    // Advanced factors
    @Column(name = "single_park_factor") private Integer singleParkFactor;
    @Column(name = "walk_park_factor") private Integer walkParkFactor;
    @Column(name = "strikeout_park_factor") private Integer strikeoutParkFactor;
    @Column(name = "babip_park_factor") private Integer babipParkFactor;
    @Column(name = "lf_home_run_factor") private Integer lfHomeRunFactor;
    @Column(name = "cf_home_run_factor") private Integer cfHomeRunFactor;
    @Column(name = "rf_home_run_factor") private Integer rfHomeRunFactor;
    @Column(name = "altitude_factor") private Integer altitudeFactor;
    @Column(name = "multi_year_park_factor") private Integer multiYearParkFactor;

    // Descriptive info
    @Column(name = "city") private String city;
    @Column(name = "capacity") private Integer capacity;
    @Column(name = "surface") private String surface;
    @Column(name = "roof_type") private String roofType;
    @Column(name = "notable_features", length = 500) private String notableFeatures;

    // Meta
    @Column(name = "source_name") private String sourceName;
    @Column(name = "source_last_updated") private LocalDate sourceLastUpdated;

    public BallparkFactorSnapshot() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Integer getParkFactor() { return parkFactor; }
    public void setParkFactor(Integer parkFactor) { this.parkFactor = parkFactor; }
    public Integer getRunParkFactor() { return runParkFactor; }
    public void setRunParkFactor(Integer runParkFactor) { this.runParkFactor = runParkFactor; }
    public Integer getHomeRunParkFactor() { return homeRunParkFactor; }
    public void setHomeRunParkFactor(Integer homeRunParkFactor) { this.homeRunParkFactor = homeRunParkFactor; }
    public Integer getHitParkFactor() { return hitParkFactor; }
    public void setHitParkFactor(Integer hitParkFactor) { this.hitParkFactor = hitParkFactor; }
    public Integer getDoubleParkFactor() { return doubleParkFactor; }
    public void setDoubleParkFactor(Integer doubleParkFactor) { this.doubleParkFactor = doubleParkFactor; }
    public Integer getTripleParkFactor() { return tripleParkFactor; }
    public void setTripleParkFactor(Integer tripleParkFactor) { this.tripleParkFactor = tripleParkFactor; }
    public Integer getLhbParkFactor() { return lhbParkFactor; }
    public void setLhbParkFactor(Integer lhbParkFactor) { this.lhbParkFactor = lhbParkFactor; }
    public Integer getRhbParkFactor() { return rhbParkFactor; }
    public void setRhbParkFactor(Integer rhbParkFactor) { this.rhbParkFactor = rhbParkFactor; }
    public Integer getSingleParkFactor() { return singleParkFactor; }
    public void setSingleParkFactor(Integer singleParkFactor) { this.singleParkFactor = singleParkFactor; }
    public Integer getWalkParkFactor() { return walkParkFactor; }
    public void setWalkParkFactor(Integer walkParkFactor) { this.walkParkFactor = walkParkFactor; }
    public Integer getStrikeoutParkFactor() { return strikeoutParkFactor; }
    public void setStrikeoutParkFactor(Integer strikeoutParkFactor) { this.strikeoutParkFactor = strikeoutParkFactor; }
    public Integer getBabipParkFactor() { return babipParkFactor; }
    public void setBabipParkFactor(Integer babipParkFactor) { this.babipParkFactor = babipParkFactor; }
    public Integer getLfHomeRunFactor() { return lfHomeRunFactor; }
    public void setLfHomeRunFactor(Integer lfHomeRunFactor) { this.lfHomeRunFactor = lfHomeRunFactor; }
    public Integer getCfHomeRunFactor() { return cfHomeRunFactor; }
    public void setCfHomeRunFactor(Integer cfHomeRunFactor) { this.cfHomeRunFactor = cfHomeRunFactor; }
    public Integer getRfHomeRunFactor() { return rfHomeRunFactor; }
    public void setRfHomeRunFactor(Integer rfHomeRunFactor) { this.rfHomeRunFactor = rfHomeRunFactor; }
    public Integer getAltitudeFactor() { return altitudeFactor; }
    public void setAltitudeFactor(Integer altitudeFactor) { this.altitudeFactor = altitudeFactor; }
    public Integer getMultiYearParkFactor() { return multiYearParkFactor; }
    public void setMultiYearParkFactor(Integer multiYearParkFactor) { this.multiYearParkFactor = multiYearParkFactor; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getSurface() { return surface; }
    public void setSurface(String surface) { this.surface = surface; }
    public String getRoofType() { return roofType; }
    public void setRoofType(String roofType) { this.roofType = roofType; }
    public String getNotableFeatures() { return notableFeatures; }
    public void setNotableFeatures(String notableFeatures) { this.notableFeatures = notableFeatures; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public LocalDate getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(LocalDate sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }
}
