package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.dto.WinProbabilityResult;
import com.example.redsoxtracker.repository.GameRepository;
import com.example.redsoxtracker.service.GameService;
import com.example.redsoxtracker.service.HistoricalStandingsService;
import com.example.redsoxtracker.service.LiveScoreboardService;
import com.example.redsoxtracker.service.MatchupService;
import com.example.redsoxtracker.service.StandingsService;
import com.example.redsoxtracker.service.TeamRankingService;
import com.example.redsoxtracker.service.TeamRecordService;
import com.example.redsoxtracker.service.TeamStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    /** Short form for display. "Boston" is understood on a Red Sox site. */
    private static final String RED_SOX = "Red Sox";

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final LiveScoreboardService liveScoreboardService;
    private final MatchupService matchupService;
    private final TeamStatsService teamStatsService;
    private final StandingsService standingsService;
    private final TeamRankingService teamRankingService;
    private final TeamRecordService teamRecordService;
    private final HistoricalStandingsService historicalStandingsService;

    public DashboardController(GameRepository gameRepository, GameService gameService,
                               LiveScoreboardService liveScoreboardService,
                               MatchupService matchupService, TeamStatsService teamStatsService,
                               StandingsService standingsService, TeamRankingService teamRankingService,
                               TeamRecordService teamRecordService,
                               HistoricalStandingsService historicalStandingsService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.liveScoreboardService = liveScoreboardService;
        this.matchupService = matchupService;
        this.teamStatsService = teamStatsService;
        this.standingsService = standingsService;
        this.teamRankingService = teamRankingService;
        this.teamRecordService = teamRecordService;
        this.historicalStandingsService = historicalStandingsService;
    }

    @GetMapping("/")
    public String dashboard(Model model,
                            @RequestParam(name = "standingsDate", required = false) String standingsDate) {
        List<Game> games = gameRepository.findAllByOrderByGameDateAsc();
        model.addAttribute("games", games);
        model.addAttribute("wins", gameService.countWins());
        model.addAttribute("losses", gameService.countLosses());

        // Featured matchup preview
        Optional<Game> featuredGame = matchupService.getFeaturedGame();
        if (featuredGame.isPresent()) {
            Game game = featuredGame.get();
            model.addAttribute("featuredGame", game);
            model.addAttribute("featuredTitle", matchupService.buildMatchupTitle(game));
            model.addAttribute("featuredScoreline", matchupService.buildScoreline(game));
            boolean isLive = "In Progress".equals(game.getStatus());
            model.addAttribute("featuredIsToday", isLive || game.getGameDate().isEqual(LocalDate.now()));
            model.addAttribute("featuredIsLive", isLive);
            boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
            model.addAttribute("featuredAwayTeam", redSoxAreAway ? RED_SOX : game.getOpponent());
            model.addAttribute("featuredHomeTeam", redSoxAreAway ? game.getOpponent() : RED_SOX);
            model.addAttribute("featuredAwayRecord", redSoxAreAway ? game.getRedSoxRecord() : game.getOpponentRecord());
            model.addAttribute("featuredHomeRecord", redSoxAreAway ? game.getOpponentRecord() : game.getRedSoxRecord());
            model.addAttribute("featuredAwayScore", redSoxAreAway ? game.getRedSoxScore() : game.getOpponentScore());
            model.addAttribute("featuredHomeScore", redSoxAreAway ? game.getOpponentScore() : game.getRedSoxScore());
            WinProbabilityResult winProb = matchupService.calculateForGame(game);
            model.addAttribute("featuredWinProb", winProb);
            Optional<BallparkFactorSnapshot> park = matchupService.getParkForGame(game);
            park.ifPresent(p -> model.addAttribute("featuredPark", p));
            liveScoreboardService.buildForGame(game).ifPresent(s -> model.addAttribute("scoreboard", s));
        }

        // Previous completed game box score (skip if it's the same game already shown above)
        Optional<Game> previousGame = games.stream()
                .filter(g -> "Final".equals(g.getStatus()))
                .filter(g -> featuredGame.isEmpty() || !g.getId().equals(featuredGame.get().getId()))
                .reduce((first, second) -> second);
        if (previousGame.isPresent()) {
            Game prev = previousGame.get();
            model.addAttribute("previousGame", prev);
            boolean prevRedSoxAreAway = "Away".equalsIgnoreCase(prev.getHomeAway());
            model.addAttribute("previousAwayTeam", prevRedSoxAreAway ? RED_SOX : prev.getOpponent());
            model.addAttribute("previousHomeTeam", prevRedSoxAreAway ? prev.getOpponent() : RED_SOX);
            liveScoreboardService.buildForGame(prev).ifPresent(s -> model.addAttribute("previousScoreboard", s));
        }

        // Red Sox team stats for sidebar
        teamStatsService.getLatestStats("BOS").ifPresent(s -> model.addAttribute("bosStats", s));
        // Record, last 10 and streak come from the game log whenever it is ahead of the feed.
        model.addAttribute("bosRecord",
                teamRecordService.bosRecord(teamStatsService.getLatestStats("BOS").orElse(null)));
        model.addAttribute("rankSummary", teamRankingService.rankBos());

        addStandings(model, standingsDate);

        return "dashboard";
    }

    /**
     * Standings for the selected date, rebuilt from the league-wide game log.
     *
     * <p>Every date uses the same source, including the latest, so the scrubber, the
     * timelapse and the static table can never disagree. The computed table was checked
     * against MLB's own standings and matched all 30 clubs on record, streak and games
     * back, and it picks up a finished game before the cached standings feed does. The
     * feed is kept only as a fallback for a database that has no league log yet.</p>
     */
    private void addStandings(Model model, String standingsDate) {
        List<LocalDate> playedDates = historicalStandingsService.hasData()
                ? historicalStandingsService.playedDates()
                : List.of();

        if (playedDates.isEmpty()) {
            model.addAttribute("standings", standingsService.buildStandings());
            return;
        }

        LocalDate latest = playedDates.get(playedDates.size() - 1);
        LocalDate selected = latest;
        if (standingsDate != null && !standingsDate.isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(standingsDate);
                // Snap to the nearest played date at or before the request, so a day with
                // no baseball still shows the standings that were true that evening.
                selected = playedDates.stream()
                        .filter(d -> !d.isAfter(parsed))
                        .reduce((a, b) -> b)
                        .orElse(playedDates.get(0));
            } catch (DateTimeParseException ignored) {
                selected = latest;
            }
        }

        boolean isLatest = selected.isEqual(latest);
        model.addAttribute("standings", historicalStandingsService.standingsOn(selected));

        int index = playedDates.indexOf(selected);
        model.addAttribute("standingsSelectedDate", selected);
        model.addAttribute("standingsIsLatest", isLatest);
        model.addAttribute("standingsPrevDate", index > 0 ? playedDates.get(index - 1) : null);
        model.addAttribute("standingsNextDate", isLatest ? null : playedDates.get(index + 1));
        model.addAttribute("standingsFirstDate", playedDates.get(0));
        model.addAttribute("standingsLatestDate", latest);
        model.addAttribute("standingsDayIndex", index);
        model.addAttribute("standingsTotalDays", playedDates.size());
    }
}
