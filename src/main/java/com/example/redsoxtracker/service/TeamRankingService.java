package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.TeamRank;
import com.example.redsoxtracker.dto.TeamRankSummary;
import com.example.redsoxtracker.dto.TeamRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Ranks the Red Sox against all 30 clubs and, separately, against the AL. */
@Service
public class TeamRankingService {

    private static final String BOS = "BOS";

    private final TeamStatsService teamStatsService;
    private final TeamRecordService teamRecordService;
    private final SeriesRecordService seriesRecordService;

    public TeamRankingService(TeamStatsService teamStatsService,
                              TeamRecordService teamRecordService,
                              SeriesRecordService seriesRecordService) {
        this.teamStatsService = teamStatsService;
        this.teamRecordService = teamRecordService;
        this.seriesRecordService = seriesRecordService;
    }

    public TeamRankSummary rankBos() {
        Optional<TeamStatSnapshot> bosSnapOpt = teamStatsService.getLatestStats(BOS);
        if (bosSnapOpt.isEmpty()) return emptySummary();
        TeamStatSnapshot bosSnap = bosSnapOpt.get();

        List<TeamStatSnapshot> allSnaps = new ArrayList<>();
        for (Team t : teamStatsService.getAllTeams()) {
            teamStatsService.getLatestStats(t).ifPresent(allSnaps::add);
        }

        // Rank the record-based stats off the same reconciled numbers the cards display,
        // so a game the feed hasn't posted yet still moves the Red Sox in the standings order.
        TeamRecord bosRecord = teamRecordService.bosRecord(bosSnap);

        Map<Integer, SeriesRecordService.SeriesRecord> series = seriesRecordService.recordsByTeam();
        Integer bosId = bosSnap.getTeam().getMlbTeamId();
        SeriesRecordService.SeriesRecord bosSeries =
                series.getOrDefault(bosId, SeriesRecordService.SeriesRecord.EMPTY);

        return new TeamRankSummary(
                rank(allSnaps, pct(bosRecord.getWins(), bosRecord.getLosses()), TeamRankingService::winPct, true),
                rank(allSnaps, bosSnap, s -> toDouble(s.getRunDifferential()), true),
                rank(allSnaps, bosSnap, TeamStatSnapshot::getTeamOps, true),
                rank(allSnaps, bosSnap, TeamStatSnapshot::getTeamEra, false),
                rank(allSnaps, bosSnap, TeamStatSnapshot::getBullpenEra, false),
                rank(allSnaps, bosSnap, TeamStatSnapshot::getTeamWrcPlus, true),
                rank(allSnaps, recordPct(bosRecord.getLast10()), TeamRankingService::last10Pct, true),
                rank(allSnaps, parseStreak(bosRecord.getStreak()), TeamRankingService::streakValue, true),
                rankSeries(allSnaps, series, SeriesRecordService.SeriesRecord::won, bosSeries),
                rankSeries(allSnaps, series, SeriesRecordService.SeriesRecord::sweeps, bosSeries)
        );
    }

    /** The Red Sox series line, for the card itself rather than its rank. */
    public SeriesRecordService.SeriesRecord bosSeriesRecord() {
        return teamStatsService.getLatestStats(BOS)
                .map(s -> seriesRecordService.forTeam(s.getTeam().getMlbTeamId()))
                .orElse(SeriesRecordService.SeriesRecord.EMPTY);
    }

    private TeamRank rank(List<TeamStatSnapshot> all, TeamStatSnapshot bosSnap,
                          Function<TeamStatSnapshot, Double> extractor, boolean higherIsBetter) {
        return rank(all, extractor.apply(bosSnap), extractor, higherIsBetter);
    }

    /**
     * Standard competition ranking against every club, then against the AL only.
     * Higher is better unless told otherwise, which is how ERA is handled.
     */
    private TeamRank rank(List<TeamStatSnapshot> all, Double bosValue,
                          Function<TeamStatSnapshot, Double> extractor, boolean higherIsBetter) {
        if (bosValue == null) return new TeamRank(0, false, 0, false);

        int better = 0, leagueBetter = 0;
        boolean tied = false, leagueTied = false;
        for (TeamStatSnapshot s : all) {
            if (BOS.equals(s.getTeam().getTeamCode())) continue;
            Double v = extractor.apply(s);
            if (v == null) continue;
            boolean isAl = "AL".equals(s.getTeam().getLeague());
            boolean beats = higherIsBetter ? v > bosValue : v < bosValue;
            if (beats) {
                better++;
                if (isAl) leagueBetter++;
            } else if (Objects.equals(v, bosValue)) {
                tied = true;
                if (isAl) leagueTied = true;
            }
        }
        return new TeamRank(better + 1, tied, leagueBetter + 1, leagueTied);
    }

    /**
     * Series counts live outside the stat snapshot, so they are looked up by team id.
     * More is always better for both series wins and sweeps.
     */
    private TeamRank rankSeries(List<TeamStatSnapshot> all,
                                Map<Integer, SeriesRecordService.SeriesRecord> series,
                                Function<SeriesRecordService.SeriesRecord, Integer> extractor,
                                SeriesRecordService.SeriesRecord bosSeries) {
        if (series.isEmpty()) return new TeamRank(0, false, 0, false);
        int bosValue = extractor.apply(bosSeries);

        int better = 0, leagueBetter = 0;
        boolean tied = false, leagueTied = false;
        for (TeamStatSnapshot s : all) {
            Team team = s.getTeam();
            if (BOS.equals(team.getTeamCode()) || team.getMlbTeamId() == null) continue;
            SeriesRecordService.SeriesRecord r = series.get(team.getMlbTeamId());
            if (r == null) continue;
            int v = extractor.apply(r);
            boolean isAl = "AL".equals(team.getLeague());
            if (v > bosValue) {
                better++;
                if (isAl) leagueBetter++;
            } else if (v == bosValue) {
                tied = true;
                if (isAl) leagueTied = true;
            }
        }
        return new TeamRank(better + 1, tied, leagueBetter + 1, leagueTied);
    }

    private static Double winPct(TeamStatSnapshot s) {
        if (s.getWins() == null || s.getLosses() == null) return null;
        int total = s.getWins() + s.getLosses();
        return total > 0 ? (double) s.getWins() / total : null;
    }

    private static Double last10Pct(TeamStatSnapshot s) {
        if (s.getLast10Wins() == null || s.getLast10Losses() == null) return null;
        int total = s.getLast10Wins() + s.getLast10Losses();
        return total > 0 ? (double) s.getLast10Wins() / total : null;
    }

    /** Turns a streak like "W7"/"L3" into a signed magnitude so streaks can be ranked. */
    private static Double streakValue(TeamStatSnapshot s) {
        return parseStreak(s.getCurrentStreak());
    }

    private static Double parseStreak(String streak) {
        if (streak == null || streak.isBlank()) return null;
        try {
            char type = Character.toUpperCase(streak.charAt(0));
            int count = Integer.parseInt(streak.substring(1).trim());
            return type == 'W' ? (double) count : -(double) count;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static Double pct(Integer wins, Integer losses) {
        if (wins == null || losses == null) return null;
        int total = wins + losses;
        return total > 0 ? (double) wins / total : null;
    }

    /** Parses a "6-4" style record into a winning percentage. */
    private static Double recordPct(String record) {
        if (record == null) return null;
        String[] parts = record.split("-");
        if (parts.length != 2) return null;
        try {
            return pct(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double toDouble(Integer i) {
        return i == null ? null : i.doubleValue();
    }

    private TeamRankSummary emptySummary() {
        TeamRank none = new TeamRank(0, false, 0, false);
        return new TeamRankSummary(none, none, none, none, none, none, none, none, none, none);
    }
}
