package com.example.redsoxtracker.dto;

import java.util.List;

public class DivisionStandings {
    private final String name;
    private final List<StandingsRow> rows;

    public DivisionStandings(String name, List<StandingsRow> rows) {
        this.name = name;
        this.rows = rows;
    }

    public String getName() { return name; }
    public List<StandingsRow> getRows() { return rows; }
}
