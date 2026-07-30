package com.example.redsoxtracker.dto;

import java.util.List;

public class LeagueStandings {
    private final String league; // "AL" or "NL"
    private final List<DivisionStandings> divisions;

    public LeagueStandings(String league, List<DivisionStandings> divisions) {
        this.league = league;
        this.divisions = divisions;
    }

    public String getLeague() { return league; }
    public List<DivisionStandings> getDivisions() { return divisions; }
    public String getFullName() { return "AL".equals(league) ? "American League" : "National League"; }
}
