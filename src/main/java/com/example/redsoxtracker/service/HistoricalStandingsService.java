package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.LeagueGame;
import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.dto.DivisionStandings;
import com.example.redsoxtracker.dto.LeagueStandings;
import com.example.redsoxtracker.dto.StandingsRow;
import com.example.redsoxtracker.repository.LeagueGameRepository;
import com.example.redsoxtracker.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds the full standings for any date in the season by tallying every club's results
 * up to that day, rather than asking the MLB standings feed once per date.
 *
 * <p>That matters for the timelapse: replaying a season is roughly 190 frames, and each
 * frame needs all 30 clubs. Tallying locally makes a frame essentially free.</p>
 */
@Service
public class HistoricalStandingsService {

    private static final List<String> DIVISION_ORDER = List.of("East", "Central", "West");
    private static final List<String> LEAGUE_ORDER = List.of("AL", "NL");
    /** Wild card spots per league. */
    private static final int WILD_CARD_SPOTS = 3;

    private final LeagueGameRepository leagueGameRepo;
    private final TeamRepository teamRepo;

    public HistoricalStandingsService(LeagueGameRepository leagueGameRepo, TeamRepository teamRepo) {
        this.leagueGameRepo = leagueGameRepo;
        this.teamRepo = teamRepo;
    }

    /** True once the league-wide log has been imported; the feature stays hidden until then. */
    public boolean hasData() {
        return leagueGameRepo.count() > 0;
    }

    /** Every date in the season that had at least one completed game, in order. */
    public List<LocalDate> playedDates() {
        return playedDates(decidedGames());
    }

    /** Standings as they stood at the end of play on the given date. */
    public List<LeagueStandings> standingsOn(LocalDate date) {
        return buildStandings(tallyThrough(decidedGames(), date));
    }

    /**
     * One entry per played date, each holding the complete standings as of that evening.
     *
     * <p>The game log is read once and re-tallied per date. That is a few hundred thousand
     * cheap iterations for a full season, which is far quicker than the alternative of one
     * standings request per frame.</p>
     */
    public List<SeasonFrame> series() {
        List<LeagueGame> games = decidedGames();
        List<SeasonFrame> frames = new ArrayList<>();
        for (LocalDate date : playedDates(games)) {
            frames.add(new SeasonFrame(date, buildStandings(tallyThrough(games, date))));
        }
        return frames;
    }

    private List<LocalDate> playedDates(List<LeagueGame> games) {
        return games.stream().map(LeagueGame::getGameDate).distinct().sorted().toList();
    }

    /**
     * One tally per club counting only games completed on or before the date.
     * Games are walked in date order so streak and last-10 come out right.
     */
    private Map<Integer, Tally> tallyThrough(List<LeagueGame> games, LocalDate date) {
        Map<Integer, Tally> tallies = new HashMap<>();
        for (LeagueGame g : games) {
            if (g.getGameDate().isAfter(date)) break; // games arrive in date order
            boolean homeWon = g.getHomeScore() > g.getAwayScore();
            tallies.computeIfAbsent(g.getHomeTeamId(), k -> new Tally())
                   .add(homeWon, true, g.getHomeScore(), g.getAwayScore());
            tallies.computeIfAbsent(g.getAwayTeamId(), k -> new Tally())
                   .add(!homeWon, false, g.getAwayScore(), g.getHomeScore());
        }
        return tallies;
    }

    /** The standings on one date, as sent to the browser for the timelapse. */
    public record SeasonFrame(LocalDate date, List<LeagueStandings> standings) {}

    private List<LeagueStandings> buildStandings(Map<Integer, Tally> tallies) {
        // Every club appears on every date, including Opening Day when most are still 0-0.
        // Dropping the ones that have not played yet would leave gaps in early frames of
        // the timelapse and strand those rows showing a later day's numbers.
        Map<String, Map<String, List<Standing>>> grouped = new LinkedHashMap<>();
        for (Team team : teamRepo.findAll()) {
            if (team.getLeague() == null || team.getDivision() == null || team.getMlbTeamId() == null) continue;
            Tally t = tallies.getOrDefault(team.getMlbTeamId(), new Tally());
            grouped.computeIfAbsent(team.getLeague(), k -> new LinkedHashMap<>())
                   .computeIfAbsent(team.getDivision(), k -> new ArrayList<>())
                   .add(new Standing(team, t));
        }

        List<LeagueStandings> result = new ArrayList<>();
        for (String league : LEAGUE_ORDER) {
            Map<String, List<Standing>> divisions = grouped.get(league);
            if (divisions == null) continue;

            // Wild card race: everyone in the league who is not leading a division,
            // ranked by winning percentage.
            List<Standing> wildCardPool = new ArrayList<>();
            for (List<Standing> division : divisions.values()) {
                division.sort(STANDING_ORDER);
                for (int i = 1; i < division.size(); i++) wildCardPool.add(division.get(i));
            }
            wildCardPool.sort(STANDING_ORDER);
            Standing wildCardCut = wildCardPool.size() >= WILD_CARD_SPOTS
                    ? wildCardPool.get(WILD_CARD_SPOTS - 1)
                    : null;

            List<DivisionStandings> divList = new ArrayList<>();
            for (String divName : DIVISION_ORDER) {
                List<Standing> division = divisions.get(divName);
                if (division == null) continue;

                Standing leader = division.get(0);
                List<StandingsRow> rows = new ArrayList<>();
                for (int i = 0; i < division.size(); i++) {
                    Standing s = division.get(i);
                    boolean isLeader = i == 0;
                    Integer wcRank = null;
                    if (!isLeader) {
                        int idx = wildCardPool.indexOf(s);
                        if (idx >= 0 && idx < WILD_CARD_SPOTS) wcRank = idx + 1;
                    }
                    rows.add(toRow(s, leader, wildCardCut, isLeader, i + 1, wcRank));
                }
                divList.add(new DivisionStandings(divName, rows));
            }
            result.add(new LeagueStandings(league, divList));
        }
        return result;
    }

    private StandingsRow toRow(Standing s, Standing leader, Standing wildCardCut,
                               boolean isLeader, int divisionRank, Integer wildCardRank) {
        Tally t = s.tally;
        return new StandingsRow(
                s.team.getShortName(),
                t.wins, t.losses,
                pct(t.wins, t.losses),
                isLeader ? "-" : gamesBack(leader.tally, t),
                isLeader ? "-" : wildCardGamesBack(s, wildCardCut),
                t.last10(),
                t.streak(),
                t.runsScored, t.runsAllowed, t.runsScored - t.runsAllowed,
                t.homeWins + "-" + t.homeLosses,
                t.awayWins + "-" + t.awayLosses,
                expectedRecord(t),
                isLeader,
                divisionRank,
                wildCardRank,
                "BOS".equals(s.team.getTeamCode())
        );
    }

    /** Standard games-back: half the sum of the win gap and the loss gap. */
    private String gamesBack(Tally leader, Tally team) {
        double gb = ((leader.wins - team.wins) + (team.losses - leader.losses)) / 2.0;
        return gb == 0 ? "-" : trimHalf(gb);
    }

    /**
     * Distance to the final wild card spot. Clubs already holding a spot show how much
     * cushion they have, written with a leading plus the way MLB does.
     */
    private String wildCardGamesBack(Standing s, Standing cut) {
        if (cut == null) return "-";
        if (s == cut) return "-";
        double gb = ((cut.tally.wins - s.tally.wins) + (s.tally.losses - cut.tally.losses)) / 2.0;
        if (gb == 0) return "-";
        return gb < 0 ? "+" + trimHalf(-gb) : trimHalf(gb);
    }

    /** Pythagorean expectation, the same exponent MLB uses for its expected record column. */
    private String expectedRecord(Tally t) {
        int games = t.wins + t.losses;
        if (games == 0 || (t.runsScored == 0 && t.runsAllowed == 0)) return "-";
        double rs = Math.pow(t.runsScored, 1.83);
        double ra = Math.pow(t.runsAllowed, 1.83);
        double expectedWinPct = rs / (rs + ra);
        long expectedWins = Math.round(expectedWinPct * games);
        return expectedWins + "-" + (games - expectedWins);
    }

    private String trimHalf(double value) {
        return String.format("%.1f", value);
    }

    private String pct(int wins, int losses) {
        int total = wins + losses;
        double p = total > 0 ? (double) wins / total : 0.0;
        String formatted = String.format("%.3f", p);
        return p < 1.0 ? formatted.replaceFirst("^0", "") : formatted;
    }

    /** Better record first; ties broken by run differential so the order is stable. */
    private static final Comparator<Standing> STANDING_ORDER = Comparator
            .comparingDouble((Standing s) -> {
                int total = s.tally.wins + s.tally.losses;
                return total > 0 ? (double) s.tally.wins / total : 0.0;
            }).reversed()
            .thenComparing(Comparator.comparingInt((Standing s) -> s.tally.runsScored - s.tally.runsAllowed).reversed())
            .thenComparing(s -> s.team.getShortName());

    /** Completed games in date order. Re-read per request so a sync is picked up immediately. */
    private List<LeagueGame> decidedGames() {
        return leagueGameRepo.findByStatusOrderByGameDateAsc("Final").stream()
                .filter(LeagueGame::isDecided)
                .toList();
    }

    /** A club paired with its running totals, used while sorting a division. */
    private record Standing(Team team, Tally tally) {}

    /** Running totals for one club as the season is walked forward. */
    static class Tally {
        int wins, losses;
        int homeWins, homeLosses, awayWins, awayLosses;
        int runsScored, runsAllowed;
        /** Most recent results first is awkward to build, so append and read from the end. */
        private final List<Boolean> results = new ArrayList<>();

        void add(boolean won, boolean atHome, int scored, int allowed) {
            if (won) wins++; else losses++;
            if (atHome) {
                if (won) homeWins++; else homeLosses++;
            } else {
                if (won) awayWins++; else awayLosses++;
            }
            runsScored += scored;
            runsAllowed += allowed;
            results.add(won);
        }

        String last10() {
            int from = Math.max(0, results.size() - 10);
            int w = 0;
            for (int i = from; i < results.size(); i++) if (results.get(i)) w++;
            return w + "-" + (results.size() - from - w);
        }

        String streak() {
            if (results.isEmpty()) return "-";
            boolean latest = results.get(results.size() - 1);
            int run = 0;
            for (int i = results.size() - 1; i >= 0 && results.get(i) == latest; i--) run++;
            return (latest ? "W" : "L") + run;
        }
    }
}
