package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.LeagueGame;
import com.example.redsoxtracker.repository.LeagueGameRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Series records for all 30 clubs, worked out from the league-wide game log.
 *
 * <p>A series is a run of consecutive games against the same opponent at the same site,
 * which is how the schedule is actually built. It is won when a club takes more games
 * than it drops, and swept when it takes all of them.</p>
 */
@Service
public class SeriesRecordService {

    /** A one-game "series" cannot be swept, so sweeps need at least this many games. */
    private static final int MIN_SWEEP_GAMES = 2;

    private final LeagueGameRepository leagueGameRepo;

    public SeriesRecordService(LeagueGameRepository leagueGameRepo) {
        this.leagueGameRepo = leagueGameRepo;
    }

    /** Series record per MLB team id. */
    public Map<Integer, SeriesRecord> recordsByTeam() {
        List<LeagueGame> games = leagueGameRepo.findByStatusOrderByGameDateAsc("Final").stream()
                .filter(LeagueGame::isDecided)
                .toList();

        // Each club's own games, in date order.
        Map<Integer, List<LeagueGame>> byTeam = new HashMap<>();
        for (LeagueGame g : games) {
            byTeam.computeIfAbsent(g.getHomeTeamId(), k -> new ArrayList<>()).add(g);
            byTeam.computeIfAbsent(g.getAwayTeamId(), k -> new ArrayList<>()).add(g);
        }

        Map<Integer, SeriesRecord> out = new HashMap<>();
        byTeam.forEach((teamId, teamGames) -> out.put(teamId, walk(teamId, teamGames)));
        return out;
    }

    public SeriesRecord forTeam(Integer mlbTeamId) {
        if (mlbTeamId == null) return SeriesRecord.EMPTY;
        return recordsByTeam().getOrDefault(mlbTeamId, SeriesRecord.EMPTY);
    }

    /**
     * Walk one club's season, closing off a series whenever the opponent changes or the
     * club switches between home and away.
     */
    private SeriesRecord walk(int teamId, List<LeagueGame> teamGames) {
        int seriesWon = 0, seriesLost = 0, seriesSplit = 0, sweeps = 0, sweptBy = 0;

        Integer currentOpponent = null;
        Boolean currentAtHome = null;
        int won = 0, lost = 0;

        for (LeagueGame g : teamGames) {
            boolean atHome = g.getHomeTeamId() == teamId;
            int opponent = atHome ? g.getAwayTeamId() : g.getHomeTeamId();
            boolean teamWon = atHome
                    ? g.getHomeScore() > g.getAwayScore()
                    : g.getAwayScore() > g.getHomeScore();

            boolean sameSeries = currentOpponent != null
                    && currentOpponent == opponent
                    && currentAtHome != null && currentAtHome == atHome;

            if (!sameSeries) {
                if (currentOpponent != null) {
                    int[] r = close(won, lost);
                    seriesWon += r[0]; seriesLost += r[1]; seriesSplit += r[2];
                    sweeps += r[3]; sweptBy += r[4];
                }
                currentOpponent = opponent;
                currentAtHome = atHome;
                won = 0; lost = 0;
            }

            if (teamWon) won++; else lost++;
        }

        if (currentOpponent != null) {
            int[] r = close(won, lost);
            seriesWon += r[0]; seriesLost += r[1]; seriesSplit += r[2];
            sweeps += r[3]; sweptBy += r[4];
        }

        return new SeriesRecord(seriesWon, seriesLost, seriesSplit, sweeps, sweptBy);
    }

    /** Returns {won, lost, split, swept, sweptBy} for a single finished series. */
    private int[] close(int won, int lost) {
        int games = won + lost;
        int w = won > lost ? 1 : 0;
        int l = lost > won ? 1 : 0;
        int s = won == lost ? 1 : 0;
        int sweep = (lost == 0 && games >= MIN_SWEEP_GAMES) ? 1 : 0;
        int sweptBy = (won == 0 && games >= MIN_SWEEP_GAMES) ? 1 : 0;
        return new int[]{w, l, s, sweep, sweptBy};
    }

    /** One club's series season. */
    public record SeriesRecord(int won, int lost, int split, int sweeps, int sweptBy) {

        public static final SeriesRecord EMPTY = new SeriesRecord(0, 0, 0, 0, 0);

        public int total() { return won + lost + split; }

        /** "24-13-2", dropping the split count when there are none. */
        public String record() {
            return split > 0 ? won + "-" + lost + "-" + split : won + "-" + lost;
        }
    }
}
