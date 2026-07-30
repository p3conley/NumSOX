package com.example.redsoxtracker.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_code", nullable = false, unique = true, length = 10)
    private String teamCode;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "league", length = 2)
    private String league;

    @Column(name = "division")
    private String division;

    @Column(name = "home_venue")
    private String homeVenue;

    @Column(name = "mlb_team_id")
    private Integer mlbTeamId;

    public Team() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }
    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }
    public String getHomeVenue() { return homeVenue; }
    public void setHomeVenue(String homeVenue) { this.homeVenue = homeVenue; }
    public Integer getMlbTeamId() { return mlbTeamId; }
    public void setMlbTeamId(Integer mlbTeamId) { this.mlbTeamId = mlbTeamId; }
}
