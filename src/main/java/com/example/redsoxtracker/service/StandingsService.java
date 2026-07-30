package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.DivisionStandings;
import com.example.redsoxtracker.dto.LeagueStandings;
import com.example.redsoxtracker.dto.StandingsRow;
import com.example.redsoxtracker.repository.TeamRepository;
import com.example.redsoxtracker.repository.TeamStatSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StandingsService {

    private static final List<String> DIVISION_ORDER = List.of("East", "Central", "West");
    private static final List<String> LEAGUE_ORDER = List.of("AL", "NL");

    private final TeamRepository teamRepository;
    private final TeamStatSnapshotRepository snapshotRepository;

    public StandingsService(TeamRepository teamRepository, TeamStatSnapshotRepository snapshotRepository) {
        this.teamRepository = teamRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public List<LeagueStandings> buildStandings() {
        Map<String, Map<String, List<StandingsRow>>> byLeagueThenDivision = new HashMap<>();

        for (Team team : teamRepository.findAll()) {
            if (team.getLeague() == null || team.getDivision() == null) continue;
            Optional<TeamStatSnapshot> snapOpt = snapshotRepository.findTopByTeamOrderBySnapshotDateDesc(team);
            if (snapOpt.isEmpty()) continue;
            TeamStatSnapshot snap = snapOpt.get();
            if (snap.getWins() == null || snap.getLosses() == null) continue;

            StandingsRow row = new StandingsRow(
                    team.getShortName(),
                    snap.getWins(), snap.getLosses(),
                    pct(snap.getWins(), snap.getLosses()),
                    snap.getGamesBack() != null ? snap.getGamesBack() : "-",
                    snap.getWildCardGamesBack() != null ? snap.getWildCardGamesBack() : "-",
                    snap.getLast10Record(),
                    snap.getCurrentStreak() != null ? snap.getCurrentStreak() : "-",
                    snap.getRunsScored(), snap.getRunsAllowed(), snap.getRunDifferential(),
                    snap.getHomeRecord() != null ? snap.getHomeRecord() : "-",
                    snap.getAwayRecord() != null ? snap.getAwayRecord() : "-",
                    snap.getExpectedRecord() != null ? snap.getExpectedRecord() : "-",
                    Boolean.TRUE.equals(snap.getDivisionLeader()),
                    snap.getDivisionRank(),
                    snap.getWildCardRank(),
                    "BOS".equals(team.getTeamCode())
            );

            byLeagueThenDivision
                    .computeIfAbsent(team.getLeague(), k -> new HashMap<>())
                    .computeIfAbsent(team.getDivision(), k -> new ArrayList<>())
                    .add(row);
        }

        List<LeagueStandings> result = new ArrayList<>();
        for (String league : LEAGUE_ORDER) {
            Map<String, List<StandingsRow>> divisions = byLeagueThenDivision.get(league);
            if (divisions == null) continue;

            List<DivisionStandings> divList = new ArrayList<>();
            for (String divName : DIVISION_ORDER) {
                List<StandingsRow> rows = divisions.get(divName);
                if (rows == null) continue;
                rows.sort(Comparator.comparingInt(r -> r.getDivisionRank() != null ? r.getDivisionRank() : Integer.MAX_VALUE));
                divList.add(new DivisionStandings(divName, rows));
            }
            result.add(new LeagueStandings(league, divList));
        }
        return result;
    }

    private String pct(int wins, int losses) {
        int total = wins + losses;
        double p = total > 0 ? (double) wins / total : 0.0;
        String formatted = String.format("%.3f", p);
        return p < 1.0 ? formatted.replaceFirst("^0", "") : formatted;
    }
}
